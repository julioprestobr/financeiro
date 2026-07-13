package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.AccountBalanceHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "Response.AccountBalanceHistory", description = "Dados de um AccountBalanceHistory")
public record AccountBalanceHistoryResponse(
        String accountCode,
        String accountName,
        String bankCode,

        String companyCode,
        String companyName,

        LocalDate referenceDate,
        Integer dayOfWeek,
        Boolean isBusinessDay,

        BigDecimal dailyCredits,
        BigDecimal dailyDebits,
        BigDecimal netDailyMovement,
        Integer dailyMovementCount,

        BigDecimal accumulatedBalance,

        LocalDateTime snapshotDatetime
) {
    public static AccountBalanceHistoryResponse from(AccountBalanceHistory abh) {
        return new AccountBalanceHistoryResponse(
                abh.getAccountCode(),
                abh.getAccountName(),
                abh.getBankCode(),
                abh.getCompanyCode(),
                abh.getCompanyName(),
                abh.getReferenceDate(),
                abh.getDayOfWeek(),
                abh.getIsBusinessDay(),
                abh.getDailyCredits(),
                abh.getDailyDebits(),
                abh.getNetDailyMovement(),
                abh.getDailyMovementCount(),
                abh.getAccumulatedBalance(),
                abh.getSnapshotDatetime()
        );
    }
}
