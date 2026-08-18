package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.Nfe;
import com.prestobr.financeiro.domain.util.NfeAnonymizer;
import com.prestobr.financeiro.dto.request.NfePageRequest;
import com.prestobr.financeiro.dto.response.NfeResponse;
import com.prestobr.financeiro.dto.response.NfeSummaryResponse;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NfeService {

    private static final String TABLE = "nfe_consolidadas";

    // numero_nf é texto contendo apenas dígitos: sem o CAST, a ordenação seria
    // lexicográfica e "9" viria depois de "10".
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "issueDate", "data_emissao",
            "totalAmount", "vlr_total_nf",
            "invoiceNumber", "CAST(numero_nf AS BIGINT)",
            "importDate", "data_importacao",
            "issueYear", "ano_emissao",
            "daysUntilImport", "dias_ate_importacao",
            "accessKey", "chave_acesso"
    );

    private static final RowMapper<Nfe> ROW_MAPPER = (rs, rowNum) -> Nfe.builder()
            .accessKey(rs.getString("chave_acesso"))
            .invoiceNumber(rs.getString("numero_nf"))
            .series(rs.getString("serie"))
            .invoiceType(rs.getString("tipo_nf"))
            .status(rs.getString("status"))
            .canceled(getBoolean(rs, "cancelada"))
            .cancellationDate(getLocalDateFromText(rs, "evento_cancelamento_emissao"))
            .issuerTaxId(rs.getString("cnpj_emitente"))
            .issuerName(rs.getString("nome_emitente"))
            .recipientTaxId(rs.getString("cnpj_destinatario"))
            .recipientName(rs.getString("nome_destinatario"))
            .issueDate(getLocalDate(rs, "data_emissao"))
            .issueYear(getInteger(rs, "ano_emissao"))
            .issueMonth(getInteger(rs, "mes_emissao"))
            .importDate(getLocalDate(rs, "data_importacao"))
            .daysUntilImport(getInteger(rs, "dias_ate_importacao"))
            .totalAmount(rs.getBigDecimal("vlr_total_nf"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.nfe.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<NfeResponse> search(NfePageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<NfeResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(NfeResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public NfeResponse getByAccessKey(String accessKey) {
        String sql = "SELECT * FROM " + TABLE + " WHERE chave_acesso = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, accessKey)
                .stream()
                .findFirst()
                .map(this::applyAnonymization)
                .map(NfeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nota fiscal não encontrada para a chave de acesso: " + accessKey
                ));
    }

    public List<NfeSummaryResponse> summary(Integer year) {
        StringBuilder sql = new StringBuilder(
                "SELECT ano_emissao,"
                        + " mes_emissao,"
                        + " count(*) FILTER (WHERE tipo_nf = 'Emitida')  AS qtd_emitidas,"
                        + " count(*) FILTER (WHERE tipo_nf = 'Recebida') AS qtd_recebidas,"
                        + " count(*) FILTER (WHERE cancelada)            AS qtd_canceladas,"
                        + " count(*)                                     AS qtd_total,"
                        + " COALESCE(sum(vlr_total_nf) FILTER (WHERE tipo_nf = 'Emitida'), 0)  AS vlr_emitidas,"
                        + " COALESCE(sum(vlr_total_nf) FILTER (WHERE tipo_nf = 'Recebida'), 0) AS vlr_recebidas"
                        + " FROM " + TABLE);

        List<Object> params = new ArrayList<>();
        if (year != null) {
            sql.append(" WHERE ano_emissao = ?");
            params.add(year);
        }
        sql.append(" GROUP BY ano_emissao, mes_emissao ORDER BY ano_emissao, mes_emissao");

        return dataLakeJdbcTemplate.query(sql.toString(), (rs, rowNum) -> new NfeSummaryResponse(
                getInteger(rs, "ano_emissao"),
                getInteger(rs, "mes_emissao"),
                rs.getLong("qtd_emitidas"),
                rs.getLong("qtd_recebidas"),
                rs.getLong("qtd_canceladas"),
                rs.getLong("qtd_total"),
                applyAnonymization(rs.getBigDecimal("vlr_emitidas")),
                applyAnonymization(rs.getBigDecimal("vlr_recebidas"))
        ), params.toArray());
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(NfePageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.accessKey() != null) {
            conditions.add("chave_acesso = ?");
            params.add(request.accessKey());
        }
        if (request.invoiceNumber() != null) {
            conditions.add("numero_nf = ?");
            params.add(request.invoiceNumber());
        }
        if (request.series() != null) {
            conditions.add("serie = ?");
            params.add(request.series());
        }
        if (request.invoiceType() != null) {
            conditions.add("tipo_nf = ?");
            params.add(request.invoiceType());
        }
        if (request.status() != null) {
            conditions.add("status = ?");
            params.add(request.status());
        }
        if (request.canceled() != null) {
            conditions.add("cancelada = ?");
            params.add(request.canceled());
        }
        if (request.issuerTaxId() != null) {
            conditions.add("cnpj_emitente = ?");
            params.add(request.issuerTaxId());
        }
        if (request.recipientTaxId() != null) {
            conditions.add("cnpj_destinatario = ?");
            params.add(request.recipientTaxId());
        }
        if (request.issuerName() != null) {
            conditions.add("nome_emitente ILIKE ?");
            params.add("%" + request.issuerName() + "%");
        }
        if (request.recipientName() != null) {
            conditions.add("nome_destinatario ILIKE ?");
            params.add("%" + request.recipientName() + "%");
        }
        if (request.issueYear() != null) {
            conditions.add("ano_emissao = ?");
            params.add(request.issueYear());
        }
        if (request.issueMonth() != null) {
            conditions.add("mes_emissao = ?");
            params.add(request.issueMonth());
        }
        if (request.issueDateFrom() != null) {
            conditions.add("data_emissao >= ?");
            params.add(request.issueDateFrom());
        }
        if (request.issueDateTo() != null) {
            conditions.add("data_emissao <= ?");
            params.add(request.issueDateTo());
        }
        if (request.minAmount() != null) {
            conditions.add("vlr_total_nf >= ?");
            params.add(request.minAmount());
        }
        if (request.maxAmount() != null) {
            conditions.add("vlr_total_nf <= ?");
            params.add(request.maxAmount());
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

    private Nfe applyAnonymization(Nfe nfe) {
        return anonymizeData ? NfeAnonymizer.anonymize(nfe) : nfe;
    }

    private BigDecimal applyAnonymization(BigDecimal amount) {
        return anonymizeData ? NfeAnonymizer.randomizeValue(amount) : amount;
    }

    // =========================================================================
    // MAPEAMENTO RESULTSET -> ENTITY
    // =========================================================================

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
    }

    /**
     * evento_cancelamento_emissao é uma coluna text que guarda a data no
     * formato ISO (yyyy-MM-dd). Valor fora do padrão vira null em vez de
     * derrubar a consulta inteira.
     */
    private static LocalDate getLocalDateFromText(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record WhereClause(String sql, List<Object> params) {}
}
