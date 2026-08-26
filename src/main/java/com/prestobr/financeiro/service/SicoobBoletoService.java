package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.SicoobBoleto;
import com.prestobr.financeiro.domain.util.SicoobBoletoAnonymizer;
import com.prestobr.financeiro.dto.request.SicoobBoletoPageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import com.prestobr.financeiro.dto.response.SicoobBoletoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SicoobBoletoService {

    private static final String TABLE = "sicoob_boletos";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "dueDate", "data_vencimento",
            "issueDate", "data_emissao",
            "paymentDate", "data_pagamento",
            "amount", "valor",
            "paidAmount", "valor_pago",
            "statusCode", "situacao",
            "daysToDueDate", "dias_para_vencimento",
            "id", "id"
    );

    private static final RowMapper<SicoobBoleto> ROW_MAPPER = (rs, rowNum) -> SicoobBoleto.builder()
            .id(getInteger(rs, "id"))
            .barcode(rs.getString("codigo_barras"))
            .ourNumber(rs.getString("nosso_numero"))
            .documentNumber(rs.getString("numero_documento"))
            .statusCode(getInteger(rs, "situacao"))
            .statusDescription(rs.getString("descricao_situacao"))
            .payeeName(rs.getString("beneficiario_nome"))
            .payeeTaxId(rs.getString("beneficiario_cpf_cnpj"))
            .payerName(rs.getString("pagador_nome"))
            .payerTaxId(rs.getString("pagador_cpf_cnpj"))
            .issueDate(getLocalDate(rs, "data_emissao"))
            .dueDate(getLocalDate(rs, "data_vencimento"))
            .paymentDate(getLocalDate(rs, "data_pagamento"))
            .paymentDeadline(getLocalDate(rs, "data_limite_pagamento"))
            .daysToDueDate(getInteger(rs, "dias_para_vencimento"))
            .issueYear(getInteger(rs, "ano_emissao"))
            .issueMonth(getInteger(rs, "mes_emissao"))
            .amount(rs.getBigDecimal("valor"))
            .paidAmount(rs.getBigDecimal("valor_pago"))
            .discountAmount(rs.getBigDecimal("valor_desconto"))
            .interestAmount(rs.getBigDecimal("valor_juros"))
            .fineAmount(rs.getBigDecimal("valor_multa"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.sicoob-boleto.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<SicoobBoletoResponse> search(SicoobBoletoPageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<SicoobBoletoResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(SicoobBoletoResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public SicoobBoletoResponse getByBarcode(String barcode) {
        String sql = "SELECT * FROM " + TABLE + " WHERE codigo_barras = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, barcode)
                .stream()
                .findFirst()
                .map(this::applyAnonymization)
                .map(SicoobBoletoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Boleto não encontrado para o código de barras: " + barcode
                ));
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(SicoobBoletoPageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.barcode() != null) {
            conditions.add("codigo_barras = ?");
            params.add(request.barcode());
        }
        if (request.ourNumber() != null) {
            conditions.add("nosso_numero = ?");
            params.add(request.ourNumber());
        }
        if (request.documentNumber() != null) {
            conditions.add("numero_documento = ?");
            params.add(request.documentNumber());
        }
        if (request.statusCode() != null) {
            conditions.add("situacao = ?");
            params.add(request.statusCode());
        }
        if (request.statusDescription() != null) {
            conditions.add("descricao_situacao = ?");
            params.add(request.statusDescription());
        }
        if (request.payeeTaxId() != null) {
            conditions.add("beneficiario_cpf_cnpj = ?");
            params.add(request.payeeTaxId());
        }
        if (request.payeeName() != null) {
            conditions.add("beneficiario_nome ILIKE ?");
            params.add("%" + request.payeeName() + "%");
        }
        if (request.issueDateFrom() != null) {
            conditions.add("data_emissao >= ?");
            params.add(request.issueDateFrom());
        }
        if (request.issueDateTo() != null) {
            conditions.add("data_emissao <= ?");
            params.add(request.issueDateTo());
        }
        if (request.dueDateFrom() != null) {
            conditions.add("data_vencimento >= ?");
            params.add(request.dueDateFrom());
        }
        if (request.dueDateTo() != null) {
            conditions.add("data_vencimento <= ?");
            params.add(request.dueDateTo());
        }
        if (request.paymentDateFrom() != null) {
            conditions.add("data_pagamento >= ?");
            params.add(request.paymentDateFrom());
        }
        if (request.paymentDateTo() != null) {
            conditions.add("data_pagamento <= ?");
            params.add(request.paymentDateTo());
        }
        if (request.paid() != null) {
            conditions.add(request.paid() ? "data_pagamento IS NOT NULL" : "data_pagamento IS NULL");
        }
        if (request.minAmount() != null) {
            conditions.add("valor >= ?");
            params.add(request.minAmount());
        }
        if (request.maxAmount() != null) {
            conditions.add("valor <= ?");
            params.add(request.maxAmount());
        }
        if (request.issueYear() != null) {
            conditions.add("ano_emissao = ?");
            params.add(request.issueYear());
        }
        if (request.issueMonth() != null) {
            conditions.add("mes_emissao = ?");
            params.add(request.issueMonth());
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

    private SicoobBoleto applyAnonymization(SicoobBoleto boleto) {
        return anonymizeData ? SicoobBoletoAnonymizer.anonymize(boleto) : boleto;
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
