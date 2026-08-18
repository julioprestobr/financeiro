package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountBalanceHistory;
import com.prestobr.financeiro.domain.util.AccountBalanceHistoryAnonymizer;
import com.prestobr.financeiro.dto.request.AccountBalanceHistoryPageRequest;
import com.prestobr.financeiro.dto.response.AccountBalanceHistoryResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountBalanceHistoryService {

    private static final String TABLE = "historico_saldo";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "referenceDate", "data",
            "accumulatedBalance", "saldo_acumulado",
            "dailyCredits", "creditos_dia",
            "dailyDebits", "debitos_dia",
            "netDailyMovement", "movimento_liquido_dia"
    );

    private static final RowMapper<AccountBalanceHistory> ROW_MAPPER = (rs, rowNum) -> AccountBalanceHistory.builder()
            .accountCode(rs.getString("cod_conta"))
            .accountName(rs.getString("nome_conta"))
            .bankCode(rs.getString("cod_banco"))
            .companyCode(rs.getString("cod_empresa"))
            .companyName(rs.getString("nome_empresa"))
            .referenceDate(getLocalDate(rs, "data"))
            .dayOfWeek(getInteger(rs, "dia_semana"))
            .isBusinessDay(getBoolean(rs, "is_dia_util"))
            .dailyCredits(rs.getBigDecimal("creditos_dia"))
            .dailyDebits(rs.getBigDecimal("debitos_dia"))
            .netDailyMovement(rs.getBigDecimal("movimento_liquido_dia"))
            .dailyMovementCount(getInteger(rs, "qtd_movimentos_dia"))
            .accumulatedBalance(rs.getBigDecimal("saldo_acumulado"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    @Value("${financeiro.account-balance-history.anonymize-data:false}")
    private boolean anonymizeData;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountBalanceHistoryResponse> search(AccountBalanceHistoryPageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<AccountBalanceHistoryResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyAnonymization)
                .map(AccountBalanceHistoryResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public List<AccountBalanceHistoryResponse> getByAccountCode(String accountCode) {
        String sql = "SELECT * FROM " + TABLE + " WHERE cod_conta = ? ORDER BY data ASC";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, accountCode)
                .stream()
                .map(this::applyAnonymization)
                .map(AccountBalanceHistoryResponse::from)
                .toList();
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(AccountBalanceHistoryPageRequest request) {
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
        if (request.bankCode() != null) {
            conditions.add("cod_banco = ?");
            params.add(request.bankCode());
        }
        if (request.isBusinessDay() != null) {
            conditions.add("is_dia_util = ?");
            params.add(request.isBusinessDay());
        }
        if (request.referenceDateFrom() != null) {
            conditions.add("data >= ?");
            params.add(request.referenceDateFrom());
        }
        if (request.referenceDateTo() != null) {
            conditions.add("data <= ?");
            params.add(request.referenceDateTo());
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

    private AccountBalanceHistory applyAnonymization(AccountBalanceHistory accountBalanceHistory) {
        return anonymizeData
                ? AccountBalanceHistoryAnonymizer.anonymize(accountBalanceHistory)
                : accountBalanceHistory;
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
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record WhereClause(String sql, List<Object> params) {}
}
