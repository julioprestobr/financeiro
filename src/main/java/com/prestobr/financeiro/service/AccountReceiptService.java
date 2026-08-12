package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountReceipt;
import com.prestobr.financeiro.domain.util.AccountReceiptAnonymizer;
import com.prestobr.financeiro.dto.request.AccountReceiptPageRequest;
import com.prestobr.financeiro.dto.response.AccountReceiptResponse;
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
public class AccountReceiptService {

    private static final String TABLE = "recebimento";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "movementDate", "data_movimentacao",
            "createdAt", "data_cadastro",
            "updatedAt", "data_alteracao",
            "documentValue", "vlr_documento",
            "currentBalance", "saldo_atual"
    );

    private static final RowMapper<AccountReceipt> ROW_MAPPER = (rs, rowNum) -> AccountReceipt.builder()
            .documentNumber(rs.getString("num_documento"))
            .accountCode(rs.getString("cod_conta"))
            .accountName(rs.getString("nome_conta"))
            .bankCode(rs.getString("cod_banco"))
            .companyCode(rs.getString("cod_empresa"))
            .companyName(rs.getString("nome_empresa"))
            .type(rs.getString("tipo"))
            .isSettled(getBoolean(rs, "is_compensado"))
            .movementDate(getLocalDateTime(rs, "data_movimentacao"))
            .createdAt(getLocalDateTime(rs, "data_cadastro"))
            .updatedAt(getLocalDateTime(rs, "data_alteracao"))
            .documentValue(rs.getBigDecimal("vlr_documento"))
            .currentBalance(rs.getBigDecimal("saldo_atual"))
            .settledBalance(rs.getBigDecimal("saldo_compensado"))
            .sale1(rs.getString("venda_1"))
            .sale2(rs.getString("venda_2"))
            .sale3(rs.getString("venda_3"))
            .sale4(rs.getString("venda_4"))
            .sale5(rs.getString("venda_5"))
            .description(rs.getString("historico"))
            .destination(rs.getString("destino"))
            .notes(rs.getString("observacao"))
            .createdBy(rs.getString("operador_cadastro"))
            .updatedBy(rs.getString("operador_alteracao"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.account-receipt.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountReceiptResponse> search(AccountReceiptPageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<AccountReceiptResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(AccountReceiptResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public AccountReceiptResponse getByNumeroDocumento(String numeroDocumento) {
        String sql = "SELECT * FROM " + TABLE + " WHERE num_documento = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, numeroDocumento)
                .stream()
                .findFirst()
                .map(this::applyAnonymization)
                .map(AccountReceiptResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Documento não encontrado: " + numeroDocumento
                ));
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(AccountReceiptPageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.accountCode() != null) {
            conditions.add("cod_conta = ?");
            params.add(request.accountCode());
        }
        if (request.companyCode() != null) {
            conditions.add("cod_empresa = ?");
            params.add(request.companyCode());
        }
        if (request.documentNumber() != null) {
            conditions.add("num_documento = ?");
            params.add(request.documentNumber());
        }
        if (request.type() != null) {
            conditions.add("UPPER(tipo) = UPPER(?)");
            params.add(request.type());
        }
        if (request.isSettled() != null) {
            conditions.add("is_compensado = ?");
            params.add(request.isSettled());
        }
        if (request.description() != null) {
            conditions.add("historico ILIKE ?");
            params.add("%" + request.description() + "%");
        }
        if (request.movementDateFrom() != null) {
            conditions.add("CAST(data_movimentacao AS DATE) >= ?");
            params.add(request.movementDateFrom());
        }
        if (request.movementDateTo() != null) {
            conditions.add("CAST(data_movimentacao AS DATE) <= ?");
            params.add(request.movementDateTo());
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

    private AccountReceipt applyAnonymization(AccountReceipt accountReceipt) {
        return anonymizeData
                ? AccountReceiptAnonymizer.anonymize(accountReceipt)
                : accountReceipt;
    }

    // =========================================================================
    // MAPEAMENTO RESULTSET -> ENTITY
    // =========================================================================

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
