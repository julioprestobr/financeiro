package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.domain.util.AccountPayableAnonymizer;
import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
import com.prestobr.financeiro.dto.response.AccountPayableResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountPayableService {

    private static final String TABLE = "cp_titulos_enriquecidos";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "emissionDate", "data_emissao",
            "dueDate", "data_vencimento",
            "entryDate", "data_entrada",
            "createdAt", "data_cadastro",
            "updatedAt", "data_alteracao",
            "titleValue", "valor_titulo",
            "installmentNumber", "numero_parcela",
            "daysOverdue", "dias_atraso"
    );

    private static final RowMapper<AccountPayable> ROW_MAPPER = (rs, rowNum) -> AccountPayable.builder()
            .titleCode(rs.getString("codigo_titulo"))
            .purchaseCode(rs.getString("codigo_compra"))
            .companyCode(rs.getString("cod_empresa"))
            .companyName(rs.getString("nome_empresa"))
            .vendorCode(rs.getString("cod_fornecedor"))
            .vendorName(rs.getString("nome_fornecedor"))
            .vendorTradeName(rs.getString("fantasia_fornecedor"))
            .vendorCnpj(rs.getString("cnpj_fornecedor"))
            .vendorCpf(rs.getString("cpf_fornecedor"))
            .carrierCode(rs.getString("transportador"))
            .carrierName(rs.getString("nome_transportador"))
            .carrierTradeName(rs.getString("fantasia_transportador"))
            .carrierCnpj(rs.getString("cnpj_transportador"))
            .carrierCpf(rs.getString("cpf_transportador"))
            .providerCode(rs.getString("prestador"))
            .providerName(rs.getString("nome_prestador"))
            .providerTradeName(rs.getString("fantasia_prestador"))
            .providerCnpj(rs.getString("cnpj_prestador"))
            .providerCpf(rs.getString("cpf_prestador"))
            .paymentStatus(rs.getString("status_pagamento"))
            .statusName(rs.getString("nome_status"))
            .documentType(rs.getString("tipo_documento"))
            .documentTypeName(rs.getString("nome_tipo_documento"))
            .costCenterCode(rs.getString("cod_centro_custo"))
            .costCenterName(rs.getString("nome_centro_custo"))
            .subCostCenterCode(rs.getString("cod_subcentro_custo"))
            .subCostCenterName(rs.getString("nome_subcentro_custo"))
            .accountPlan(rs.getString("plano_conta"))
            .accountPlanName(rs.getString("nome_plano_conta"))
            .departmentCode(rs.getString("cod_setor"))
            .contract(rs.getString("contrato"))
            .emissionDate(getLocalDateTime(rs, "data_emissao"))
            .dueDate(getLocalDateTime(rs, "data_vencimento"))
            .entryDate(getLocalDateTime(rs, "data_entrada"))
            .createdAt(getLocalDateTime(rs, "data_cadastro"))
            .updatedAt(getLocalDateTime(rs, "data_alteracao"))
            .titleValue(rs.getBigDecimal("valor_titulo"))
            .paidValue(rs.getBigDecimal("valor_pago"))
            .balanceValue(rs.getBigDecimal("valor_saldo"))
            .grossValue(rs.getBigDecimal("valor_bruto"))
            .discountValue(rs.getBigDecimal("valor_desconto"))
            .surchargeValue(rs.getBigDecimal("valor_acrescimo"))
            .movementValue(rs.getBigDecimal("valor_movimento"))
            .otherValues(rs.getBigDecimal("valor_outras"))
            .monetaryCorrection(rs.getBigDecimal("atualizacao_monetaria"))
            .isFullyPaid(getBoolean(rs, "is_pago_total"))
            .isProvision(getBoolean(rs, "is_provisao"))
            .titleStatus(rs.getString("situacao_titulo"))
            .titleType(rs.getString("tipo_titulo"))
            .operation(rs.getString("operacao"))
            .paymentMethod(rs.getString("forma_pagamento"))
            .paymentOption(rs.getString("opcao_pagamento"))
            .installmentNumber(rs.getString("numero_parcela"))
            .referenceMonth(rs.getString("mes_competencia"))
            .period(rs.getString("periodo"))
            .assessmentPeriod(rs.getString("periodo_apuracao"))
            .referencePeriod(rs.getString("periodo_referencia"))
            .calculationYear(getIntegerFromText(rs, "ano_calculo"))
            .daysOverdue(getInteger(rs, "dias_atraso"))
            .description(rs.getString("historico"))
            .notes(rs.getString("observacao"))
            .taxpayerDocument(rs.getString("documento_contribuinte"))
            .stateRegistration(rs.getString("inscricao_estadual"))
            .cityCode(rs.getString("cod_municipio"))
            .state(rs.getString("uf"))
            .paymentCounter(getInteger(rs, "contador_pagamento"))
            .createdBy(rs.getString("operador_cadastro"))
            .updatedBy(rs.getString("operador_alteracao"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.account-payable.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountPayableResponse> search(AccountPayablePageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<AccountPayableResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(AccountPayableResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public AccountPayableResponse getByCodigoTitulo(String codigoTitulo) {
        String sql = "SELECT * FROM " + TABLE + " WHERE codigo_titulo = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, codigoTitulo)
                .stream()
                .findFirst()
                .map(this::applyAnonymization)
                .map(AccountPayableResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Título não encontrado: " + codigoTitulo
                ));
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(AccountPayablePageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if ("PENDING".equalsIgnoreCase(request.paymentStatus())) {
            conditions.add("COALESCE(is_pago_total, false) = false");
        } else if ("PAID".equalsIgnoreCase(request.paymentStatus())) {
            conditions.add("is_pago_total = true");
        }

        if (request.titleCode() != null) {
            conditions.add("codigo_titulo = ?");
            params.add(request.titleCode());
        }
        if (request.purchaseCode() != null) {
            conditions.add("codigo_compra = ?");
            params.add(request.purchaseCode());
        }
        if (request.companyCode() != null) {
            conditions.add("cod_empresa = ?");
            params.add(request.companyCode());
        }
        if (request.vendorCode() != null) {
            conditions.add("cod_fornecedor = ?");
            params.add(request.vendorCode());
        }
        if (request.vendorCnpj() != null) {
            conditions.add("regexp_replace(cnpj_fornecedor, '\\D', '', 'g') = ?");
            params.add(request.vendorCnpj().replaceAll("\\D", ""));
        }
        if (request.providerCode() != null) {
            conditions.add("prestador = ?");
            params.add(request.providerCode());
        }
        if (request.providerCnpj() != null) {
            conditions.add("regexp_replace(cnpj_prestador, '\\D', '', 'g') = ?");
            params.add(request.providerCnpj().replaceAll("\\D", ""));
        }
        if (request.installmentNumber() != null) {
            conditions.add("numero_parcela = ?");
            params.add(request.installmentNumber());
        }
        if (request.costCenterCode() != null) {
            conditions.add("cod_centro_custo = ?");
            params.add(request.costCenterCode());
        }
        if (request.subCostCenterCode() != null) {
            conditions.add("cod_subcentro_custo = ?");
            params.add(request.subCostCenterCode());
        }
        if (request.departmentCode() != null) {
            conditions.add("cod_setor = ?");
            params.add(request.departmentCode());
        }
        if (request.accountPlan() != null) {
            conditions.add("plano_conta = ?");
            params.add(request.accountPlan());
        }
        if (request.description() != null) {
            conditions.add("historico ILIKE ?");
            params.add("%" + request.description() + "%");
        }
        if (request.documentType() != null) {
            conditions.add("tipo_documento = ?");
            params.add(request.documentType());
        }
        if (request.titleType() != null) {
            conditions.add("CAST(tipo_titulo AS TEXT) = ?");
            params.add(request.titleType());
        }
        if (request.operation() != null) {
            conditions.add("operacao = ?");
            params.add(request.operation());
        }
        if (request.paymentMethod() != null) {
            conditions.add("forma_pagamento = ?");
            params.add(request.paymentMethod());
        }
        if (request.paymentOption() != null) {
            conditions.add("opcao_pagamento = ?");
            params.add(request.paymentOption());
        }
        if (request.emissionDateFrom() != null) {
            conditions.add("CAST(data_emissao AS DATE) >= ?");
            params.add(request.emissionDateFrom());
        }
        if (request.emissionDateTo() != null) {
            conditions.add("CAST(data_emissao AS DATE) <= ?");
            params.add(request.emissionDateTo());
        }
        if (request.dueDateFrom() != null) {
            conditions.add("CAST(data_vencimento AS DATE) >= ?");
            params.add(request.dueDateFrom());
        }
        if (request.dueDateTo() != null) {
            conditions.add("CAST(data_vencimento AS DATE) <= ?");
            params.add(request.dueDateTo());
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

    private AccountPayable applyAnonymization(AccountPayable accountPayable) {
        return anonymizeData
                ? AccountPayableAnonymizer.anonymize(accountPayable)
                : accountPayable;
    }

    // =========================================================================
    // MAPEAMENTO RESULTSET -> ENTITY
    // =========================================================================

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer getIntegerFromText(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record WhereClause(String sql, List<Object> params) {}
}
