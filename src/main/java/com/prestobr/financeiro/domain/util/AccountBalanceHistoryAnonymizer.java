package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.AccountBalanceHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

public class AccountBalanceHistoryAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static AccountBalanceHistory anonymize(AccountBalanceHistory original) {
        if (original == null) {
            return null;
        }

        return AccountBalanceHistory.builder()
                // Conta Bancária - mantém código, anonimiza nome
                .accountCode(original.getAccountCode())
                .accountName(anonymizeText("Conta"))
                .bankCode(original.getBankCode())

                // Empresa - mantém código, anonimiza nome
                .companyCode(original.getCompanyCode())
                .companyName(anonymizeText("Empresa"))

                // Data - mantém
                .referenceDate(original.getReferenceDate())
                .dayOfWeek(original.getDayOfWeek())
                .isBusinessDay(original.getIsBusinessDay())

                // Movimento do dia - randomiza
                .dailyCredits(randomizeValue(original.getDailyCredits()))
                .dailyDebits(randomizeValue(original.getDailyDebits()))
                .netDailyMovement(randomizeValue(original.getNetDailyMovement()))
                .dailyMovementCount(original.getDailyMovementCount())

                // Saldo - randomiza
                .accumulatedBalance(randomizeValue(original.getAccumulatedBalance()))

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
                .build();
    }

    private static String anonymizeText(String prefix) {
        return prefix + " Anonimizado #" + random.nextInt(10000);
    }

    private static BigDecimal randomizeValue(BigDecimal original) {
        if (original == null) {
            return null;
        }
        double factor = 0.5 + random.nextDouble();
        return original.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
