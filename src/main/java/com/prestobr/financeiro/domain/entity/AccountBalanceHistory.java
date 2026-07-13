package com.prestobr.financeiro.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceHistory {

    // =========================================================================
    // CONTA BANCÁRIA
    // =========================================================================
    private String accountCode;
    private String accountName;
    private String bankCode;

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String companyCode;
    private String companyName;

    // =========================================================================
    // DATA
    // =========================================================================
    private LocalDate referenceDate;
    private Integer dayOfWeek;
    private Boolean isBusinessDay;

    // =========================================================================
    // MOVIMENTO DO DIA
    // =========================================================================
    private BigDecimal dailyCredits;
    private BigDecimal dailyDebits;
    private BigDecimal netDailyMovement;
    private Integer dailyMovementCount;

    // =========================================================================
    // SALDO
    // =========================================================================
    private BigDecimal accumulatedBalance;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;
}
