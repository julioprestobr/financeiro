package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.SicoobExtrato;
import com.prestobr.financeiro.domain.util.SicoobExtratoAnonymizer;
import com.prestobr.financeiro.dto.request.SicoobExtratoPageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import com.prestobr.financeiro.dto.response.SicoobExtratoResponse;
import com.prestobr.financeiro.dto.response.SicoobExtratoSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

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
public class SicoobExtratoService {

    private static final String TABLE = "sicoob_extrato";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "movementDate", "data_movimento",
            "amount", "valor",
            "entryType", "tipo_lancamento",
            "description", "historico",
            "id", "id"
    );

    private static final RowMapper<SicoobExtrato> ROW_MAPPER = (rs, rowNum) -> SicoobExtrato.builder()
            .id(getInteger(rs, "id"))
            .entryId(rs.getString("codigo_historico"))
            .documentNumber(rs.getString("numero_documento"))
            .movementDate(getLocalDate(rs, "data_movimento"))
            .description(rs.getString("historico"))
            .entryType(rs.getString("tipo_lancamento"))
            .amount(rs.getBigDecimal("valor"))
            .openingBalance(rs.getBigDecimal("saldo_anterior"))
            .closingBalance(rs.getBigDecimal("saldo_atual"))
            .movementYear(getInteger(rs, "ano_movimento"))
            .movementMonth(getInteger(rs, "mes_movimento"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.sicoob-extrato.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<SicoobExtratoResponse> search(SicoobExtratoPageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<SicoobExtratoResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(SicoobExtratoResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    /**
     * O saldo de fechamento e calculado (abertura + creditos - debitos) em vez
     * de lido da coluna saldo_atual, que guarda o saldo final do extrato
     * replicado em todas as linhas e por isso nao serve como fechamento de
     * periodo.
     */
    public List<SicoobExtratoSummaryResponse> summary(Integer year, Integer month) {
        StringBuilder sql = new StringBuilder(
                "SELECT ano_movimento,"
                        + " mes_movimento,"
                        + " count(*)                                             AS qtd_lancamentos,"
                        + " count(*) FILTER (WHERE tipo_lancamento = 'CREDITO')   AS qtd_creditos,"
                        + " count(*) FILTER (WHERE tipo_lancamento = 'DEBITO')    AS qtd_debitos,"
                        + " COALESCE(sum(valor) FILTER (WHERE tipo_lancamento = 'CREDITO'), 0) AS vlr_creditos,"
                        + " COALESCE(sum(valor) FILTER (WHERE tipo_lancamento = 'DEBITO'), 0)  AS vlr_debitos,"
                        + " min(saldo_anterior)                                  AS saldo_abertura"
                        + " FROM " + TABLE);

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (year != null) {
            conditions.add("ano_movimento = ?");
            params.add(year);
        }
        if (month != null) {
            conditions.add("mes_movimento = ?");
            params.add(month);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" GROUP BY ano_movimento, mes_movimento ORDER BY ano_movimento, mes_movimento");

        return dataLakeJdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            BigDecimal credits = rs.getBigDecimal("vlr_creditos");
            BigDecimal debits = rs.getBigDecimal("vlr_debitos");
            BigDecimal opening = rs.getBigDecimal("saldo_abertura");
            BigDecimal net = credits.subtract(debits);
            BigDecimal closing = opening == null ? null : opening.add(net);

            return new SicoobExtratoSummaryResponse(
                    getInteger(rs, "ano_movimento"),
                    getInteger(rs, "mes_movimento"),
                    rs.getLong("qtd_lancamentos"),
                    rs.getLong("qtd_creditos"),
                    rs.getLong("qtd_debitos"),
                    applyAnonymization(credits),
                    applyAnonymization(debits),
                    applyAnonymization(net),
                    applyAnonymization(opening),
                    applyAnonymization(closing)
            );
        }, params.toArray());
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(SicoobExtratoPageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.entryId() != null) {
            conditions.add("codigo_historico = ?");
            params.add(request.entryId());
        }
        if (request.documentNumber() != null) {
            conditions.add("numero_documento = ?");
            params.add(request.documentNumber());
        }
        if (request.entryType() != null) {
            conditions.add("tipo_lancamento = ?");
            params.add(request.entryType());
        }
        if (request.description() != null) {
            conditions.add("historico ILIKE ?");
            params.add("%" + request.description() + "%");
        }
        if (request.movementDateFrom() != null) {
            conditions.add("data_movimento >= ?");
            params.add(request.movementDateFrom());
        }
        if (request.movementDateTo() != null) {
            conditions.add("data_movimento <= ?");
            params.add(request.movementDateTo());
        }
        if (request.minAmount() != null) {
            conditions.add("valor >= ?");
            params.add(request.minAmount());
        }
        if (request.maxAmount() != null) {
            conditions.add("valor <= ?");
            params.add(request.maxAmount());
        }
        if (request.movementYear() != null) {
            conditions.add("ano_movimento = ?");
            params.add(request.movementYear());
        }
        if (request.movementMonth() != null) {
            conditions.add("mes_movimento = ?");
            params.add(request.movementMonth());
        }

        String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new WhereClause(sql, params);
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

    private SicoobExtrato applyAnonymization(SicoobExtrato extrato) {
        return anonymizeData ? SicoobExtratoAnonymizer.anonymize(extrato) : extrato;
    }

    private BigDecimal applyAnonymization(BigDecimal amount) {
        return anonymizeData ? SicoobExtratoAnonymizer.randomizeValue(amount) : amount;
    }

    // =========================================================================
    // MAPEAMENTO RESULTSET -> ENTITY
    // =========================================================================

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record WhereClause(String sql, List<Object> params) {}
}
