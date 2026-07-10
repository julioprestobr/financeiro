package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.AccountReceipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;

public class AccountReceiptAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static AccountReceipt anonymize(AccountReceipt original) {
        if (original == null) {
            return null;
        }

        return AccountReceipt.builder()
                // Identificação
                .documentNumber(generateCode("DOC"))

                // Conta Bancária - mantém código, anonimiza nome
                .accountCode(original.getAccountCode())
                .accountName(anonymizeText("Conta"))
                .bankCode(original.getBankCode())

                // Empresa - mantém código, anonimiza nome
                .companyCode(original.getCompanyCode())
                .companyName(anonymizeText("Empresa"))

                // Classificação - mantém
                .type(original.getType())
                .isSettled(original.getIsSettled())

                // Datas - mantém
                .movementDate(original.getMovementDate())
                .createdAt(original.getCreatedAt())
                .updatedAt(original.getUpdatedAt())

                // Valores - randomiza
                .documentValue(randomizeValue(original.getDocumentValue()))
                .currentBalance(randomizeValue(original.getCurrentBalance()))
                .settledBalance(randomizeValue(original.getSettledBalance()))

                // Vendas vinculadas
                .sale1(generateCode("VND"))
                .sale2(generateCode("VND"))
                .sale3(generateCode("VND"))
                .sale4(generateCode("VND"))
                .sale5(generateCode("VND"))

                // Texto / Histórico - anonimiza
                .description(anonymizeText("Histórico"))
                .destination(anonymizeText("Destino"))
                .notes(anonymizeText("Observação"))

                // Auditoria - anonimiza operadores
                .createdBy(anonymizeText("Operador"))
                .updatedBy(anonymizeText("Operador"))

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
                .build();
    }

    private static String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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