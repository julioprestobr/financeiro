package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.SicoobExtrato;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

public class SicoobExtratoAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static SicoobExtrato anonymize(SicoobExtrato original) {
        if (original == null) {
            return null;
        }

        return SicoobExtrato.builder()
                // Identificação - mantém id, mascara identificador do lançamento
                .id(original.getId())
                .entryId(maskKeepingLast(original.getEntryId(), 4))
                .documentNumber(maskKeepingLast(original.getDocumentNumber(), 4))

                // Lançamento - mantém data, histórico e tipo
                .movementDate(original.getMovementDate())
                .description(original.getDescription())
                .entryType(original.getEntryType())
                .amount(randomizeValue(original.getAmount()))

                // Saldos - randomiza
                .openingBalance(randomizeValue(original.getOpeningBalance()))
                .closingBalance(randomizeValue(original.getClosingBalance()))

                // Período - mantém
                .movementYear(original.getMovementYear())
                .movementMonth(original.getMovementMonth())

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
                .build();
    }

    private static String maskKeepingLast(String value, int visible) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= visible) {
            return "*".repeat(value.length());
        }
        return "*".repeat(value.length() - visible) + value.substring(value.length() - visible);
    }

    public static BigDecimal randomizeValue(BigDecimal original) {
        if (original == null) {
            return null;
        }
        double factor = 0.5 + random.nextDouble();
        return original.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
