package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountReceivable;
import com.prestobr.financeiro.domain.util.AccountReceivableAnonymizer;
import com.prestobr.financeiro.dto.request.AccountReceivablePageRequest;
import com.prestobr.financeiro.dto.response.AccountReceivableResponse;
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
public class AccountReceivableService {

    private static final String TABLE = "contas_a_receber_titulos";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "emissionDate", "data_emissao",
            "dueDate", "data_vencimento",
            "originalDueDate", "data_vencimento_original",
            "createdAt", "data_cadastro",
            "updatedAt", "data_alteracao",
            "titleValue", "valor_titulo",
            "daysOverdue", "dias_atraso"
    );

    private static final RowMapper<AccountReceivable> ROW_MAPPER = (rs, rowNum) -> AccountReceivable.builder()
            .titleCode(rs.getString("codigo_titulo"))
            .linkCode(rs.getString("codigo_vinculo"))
            .generatedTitleCode(rs.getString("titulo_gerado"))
            .companyCode(rs.getString("cod_empresa"))
            .companyName(rs.getString("nome_empresa"))
            .clientCode(rs.getString("cod_cliente"))
            .creditClientCode(rs.getString("cod_cliente_credito"))
            .clientName(rs.getString("nome_cliente"))
            .clientTradeName(rs.getString("fantasia_cliente"))
            .clientCnpj(rs.getString("cnpj_cliente"))
            .clientCpf(rs.getString("cpf_cliente"))
            .sellerCode(rs.getString("cod_vendedor"))
            .sellerName(rs.getString("nome_vendedor"))
            .receiptStatus(getInteger(rs, "status_recebimento"))
            .receiptStatusName(rs.getString("nome_status_recebimento"))
            .documentType(rs.getString("tipo_documento"))
            .documentTypeName(rs.getString("nome_tipo_documento"))
            .costCenterCode(rs.getString("cod_centro_custo"))
            .costCenterName(rs.getString("nome_centro_custo"))
            .subCostCenterCode(rs.getString("cod_subcentro_custo"))
            .subCostCenterName(rs.getString("nome_subcentro_custo"))
            .accountPlan(rs.getString("plano_conta"))
            .accountPlanName(rs.getString("nome_plano_conta"))
            .contract(rs.getString("contrato"))
            .emissionDate(getLocalDateTime(rs, "data_emissao"))
            .dueDate(getLocalDateTime(rs, "data_vencimento"))
            .originalDueDate(getLocalDateTime(rs, "data_vencimento_original"))
            .confirmationDate(getLocalDateTime(rs, "data_confirmacao"))
            .createdAt(getLocalDateTime(rs, "data_cadastro"))
            .updatedAt(getLocalDateTime(rs, "data_alteracao"))
            .titleValue(rs.getBigDecimal("valor_titulo"))
            .receivedValue(rs.getBigDecimal("valor_recebido"))
            .grossValue(rs.getBigDecimal("valor_bruto"))
            .discountValue(rs.getBigDecimal("valor_desconto"))
            .surchargeValue(rs.getBigDecimal("valor_acrescimo"))
            .movementValue(rs.getBigDecimal("valor_movimento"))
            .benefitDiscountValue(rs.getBigDecimal("valor_desconto_benef"))
            .isProvision(getBoolean(rs, "is_provisao"))
            .isProvision2(getBoolean(rs, "is_provisao2"))
            .titleStatus(rs.getString("situacao_titulo"))
            .titleStatusName(rs.getString("nome_situacao_titulo"))
            .titleType(getInteger(rs, "tipo_titulo"))
            .referenceMonth(rs.getString("mes_competencia"))
            .daysOverdue(getInteger(rs, "dias_atraso"))
            .description(rs.getString("historico"))
            .notes(rs.getString("observacao"))
            .issuerName(rs.getString("nome_emitente"))
            .issuerTradeName(rs.getString("nome_fantasia"))
            .bankAgencyNumber(rs.getString("numero_agencia"))
            .bankAccountNumber(rs.getString("numero_conta"))
            .checkNumber(rs.getString("numero_cheque"))
            .processNumber(rs.getString("numero_processo"))
            .creditCode(rs.getString("codigo_credito"))
            .authorizationCode(rs.getString("codigo_autorizacao"))
            .telemetry(rs.getString("telemetria"))
            .fiscalDocumentNumber(rs.getString("numero_documento_fiscal"))
            .serasa(rs.getString("serasa"))
            .batchNumber(getInteger(rs, "lote"))
            .createdBy(rs.getString("operador_cadastro"))
            .updatedBy(rs.getString("operador_alteracao"))
            .confirmedBy(rs.getString("operador_confirmacao"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.account-receivable.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountReceivableResponse> search(AccountReceivablePageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<AccountReceivableResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(AccountReceivableResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public AccountReceivableResponse getByCodigoTitulo(String codigoTitulo) {
        String sql = "SELECT * FROM " + TABLE + " WHERE codigo_titulo = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, codigoTitulo)
                .stream()
                .findFirst()
                .map(this::applyAnonymization)
                .map(AccountReceivableResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Título não encontrado: " + codigoTitulo
                ));
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(AccountReceivablePageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.titleCode() != null) {
            conditions.add("codigo_titulo = ?");
            params.add(request.titleCode());
        }
        if (request.companyCode() != null) {
            conditions.add("cod_empresa = ?");
            params.add(request.companyCode());
        }
        if (request.clientCode() != null) {
            conditions.add("cod_cliente = ?");
            params.add(request.clientCode());
        }
        if (request.costCenterCode() != null) {
            conditions.add("cod_centro_custo = ?");
            params.add(request.costCenterCode());
        }
        if (request.subCostCenterCode() != null) {
            conditions.add("cod_subcentro_custo = ?");
            params.add(request.subCostCenterCode());
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

    private AccountReceivable applyAnonymization(AccountReceivable accountReceivable) {
        return anonymizeData
                ? AccountReceivableAnonymizer.anonymize(accountReceivable)
                : accountReceivable;
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

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record WhereClause(String sql, List<Object> params) {}
}