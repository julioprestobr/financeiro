package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.Faturamento;
import com.prestobr.financeiro.domain.util.FaturamentoAnonymizer;
import com.prestobr.financeiro.dto.request.FaturamentoPageRequest;
import com.prestobr.financeiro.dto.response.FaturamentoResponse;
import com.prestobr.financeiro.dto.response.FaturamentoSummaryResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

    private static final String TABLE = "faturamento_base";

    private static final String REVENUE_CATEGORY = "Receita";

    // nota fica como ordenação de texto: 13 valores não são numéricos, então
    // um CAST para BIGINT derrubaria a consulta.
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "saleDate", "data",
            "amount", "valor",
            "customerCode", "cliente_codigo",
            "saleCode", "venda_codigo",
            "invoiceNumber", "nota"
    );

    private static final RowMapper<Faturamento> ROW_MAPPER = (rs, rowNum) -> Faturamento.builder()
            .saleCode(rs.getString("venda_codigo"))
            .invoiceNumber(rs.getString("nota"))
            .companyCode(rs.getString("empresa_codigo"))
            .companyName(trim(rs.getString("empresa_nome")))
            .customerCode(getInteger(rs, "cliente_codigo"))
            .customerName(trim(rs.getString("cliente_nome")))
            .economicGroup(trim(rs.getString("grupo_economico")))
            .operationCode(rs.getString("operacao_venda_codigo"))
            .type(rs.getString("tipo"))
            .category(rs.getString("categoria"))
            .subcategory(rs.getString("subcategoria"))
            .saleDate(getLocalDateTime(rs, "data"))
            .amount(rs.getBigDecimal("valor"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.faturamento.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<FaturamentoResponse> search(FaturamentoPageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<FaturamentoResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(FaturamentoResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public FaturamentoResponse getBySaleCode(String saleCode) {
        String sql = "SELECT * FROM " + TABLE + " WHERE venda_codigo = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, saleCode)
                .stream()
                .findFirst()
                .map(this::applyAnonymization)
                .map(FaturamentoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lançamento não encontrado para o código de venda: " + saleCode
                ));
    }

    /**
     * Separa receita do total de propósito: a tabela mistura faturamento com
     * custo, remessa, baixa e controle interno. Somar tudo daria mais que o
     * dobro do faturamento real.
     */
    public List<FaturamentoSummaryResponse> summary(Integer year, Integer month) {
        StringBuilder sql = new StringBuilder(
                "SELECT EXTRACT(YEAR FROM data)::int  AS ano,"
                        + " EXTRACT(MONTH FROM data)::int AS mes,"
                        + " count(*)                      AS qtd_lancamentos,"
                        + " COALESCE(sum(valor), 0)       AS vlr_total,"
                        + " count(*) FILTER (WHERE categoria = ?)                 AS qtd_receita,"
                        + " COALESCE(sum(valor) FILTER (WHERE categoria = ?), 0)  AS vlr_receita,"
                        + " count(DISTINCT cliente_codigo) FILTER (WHERE categoria = ?) AS qtd_clientes_receita"
                        + " FROM " + TABLE);

        List<Object> params = new ArrayList<>();
        params.add(REVENUE_CATEGORY);
        params.add(REVENUE_CATEGORY);
        params.add(REVENUE_CATEGORY);

        List<String> conditions = new ArrayList<>();
        if (year != null) {
            conditions.add("EXTRACT(YEAR FROM data) = ?");
            params.add(year);
        }
        if (month != null) {
            conditions.add("EXTRACT(MONTH FROM data) = ?");
            params.add(month);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" GROUP BY 1, 2 ORDER BY 1, 2");

        return dataLakeJdbcTemplate.query(sql.toString(), (rs, rowNum) -> new FaturamentoSummaryResponse(
                getInteger(rs, "ano"),
                getInteger(rs, "mes"),
                rs.getLong("qtd_lancamentos"),
                applyAnonymization(rs.getBigDecimal("vlr_total")),
                rs.getLong("qtd_receita"),
                applyAnonymization(rs.getBigDecimal("vlr_receita")),
                rs.getLong("qtd_clientes_receita")
        ), params.toArray());
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(FaturamentoPageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.saleCode() != null) {
            conditions.add("venda_codigo = ?");
            params.add(request.saleCode());
        }
        if (request.invoiceNumber() != null) {
            conditions.add("nota = ?");
            params.add(request.invoiceNumber());
        }
        if (request.companyCode() != null) {
            conditions.add("empresa_codigo = ?");
            params.add(request.companyCode());
        }
        if (request.customerCode() != null) {
            conditions.add("cliente_codigo = ?");
            params.add(request.customerCode());
        }
        if (request.customerName() != null) {
            conditions.add("cliente_nome ILIKE ?");
            params.add("%" + request.customerName() + "%");
        }
        if (request.economicGroup() != null) {
            conditions.add("grupo_economico = ?");
            params.add(request.economicGroup());
        }
        if (request.operationCode() != null) {
            conditions.add("operacao_venda_codigo = ?");
            params.add(request.operationCode());
        }
        if (request.type() != null) {
            conditions.add("tipo = ?");
            params.add(request.type());
        }
        if (request.category() != null) {
            conditions.add("categoria = ?");
            params.add(request.category());
        }
        if (request.subcategory() != null) {
            conditions.add("subcategoria = ?");
            params.add(request.subcategory());
        }
        if (request.saleDateFrom() != null) {
            conditions.add("data >= ?");
            params.add(request.saleDateFrom().atStartOfDay());
        }
        if (request.saleDateTo() != null) {
            // data tem hora real, então o limite superior vai para o início do
            // dia seguinte. Com "data <= 2026-12-31" os lançamentos do dia 31
            // depois da meia-noite ficariam de fora.
            conditions.add("data < ?");
            params.add(request.saleDateTo().plusDays(1).atStartOfDay());
        }
        addPeriodConditions(request.year(), request.month(), conditions, params);
        if (request.minAmount() != null) {
            conditions.add("valor >= ?");
            params.add(request.minAmount());
        }
        if (request.maxAmount() != null) {
            conditions.add("valor <= ?");
            params.add(request.maxAmount());
        }

        String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new WhereClause(sql, params);
    }

    /**
     * Ano e mês viram intervalo de data sempre que possível, em vez de EXTRACT.
     * A tabela hoje não tem índice nenhum, mas um intervalo aproveita um índice
     * futuro em data, enquanto EXTRACT o inutilizaria.
     */
    private void addPeriodConditions(Integer year, Integer month,
                                     List<String> conditions, List<Object> params) {
        if (year != null && month != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            conditions.add("data >= ?");
            params.add(start.atStartOfDay());
            conditions.add("data < ?");
            params.add(start.plusMonths(1).atStartOfDay());
        } else if (year != null) {
            LocalDate start = LocalDate.of(year, 1, 1);
            conditions.add("data >= ?");
            params.add(start.atStartOfDay());
            conditions.add("data < ?");
            params.add(start.plusYears(1).atStartOfDay());
        } else if (month != null) {
            conditions.add("EXTRACT(MONTH FROM data) = ?");
            params.add(month);
        }
    }

    private long countTotal(WhereClause where) {
        String sql = "SELECT count(*) FROM " + TABLE + where.sql();
        Long total = dataLakeJdbcTemplate.queryForObject(sql, Long.class, where.params().toArray());
        return total == null ? 0 : total;
    }

    // =========================================================================
    // ORDENAÇÃO
    // =========================================================================

    private String buildOrderBy(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return "";
        }

        List<String> orders = new ArrayList<>();
        for (String s : sort) {
            String[] parts = s.split(",");
            String column = SORTABLE_COLUMNS.get(parts[0]);
            if (column == null) {
                continue;
            }
            String direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]) ? "DESC" : "ASC";
            orders.add(column + " " + direction);
        }

        return orders.isEmpty() ? "" : " ORDER BY " + String.join(", ", orders);
    }

    // =========================================================================
    // ANONIMIZAÇÃO
    // =========================================================================

    private Faturamento applyAnonymization(Faturamento faturamento) {
        return anonymizeData ? FaturamentoAnonymizer.anonymize(faturamento) : faturamento;
    }

    private BigDecimal applyAnonymization(BigDecimal amount) {
        return anonymizeData ? FaturamentoAnonymizer.randomizeValue(amount) : amount;
    }

    // =========================================================================
    // MAPEAMENTO RESULTSET -> ENTITY
    // =========================================================================

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    // Alguns nomes de cliente vêm com espaço sobrando na origem.
    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private record WhereClause(String sql, List<Object> params) {}
}
