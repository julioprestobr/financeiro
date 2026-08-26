package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.SicoobBoleto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

public class SicoobBoletoAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static SicoobBoleto anonymize(SicoobBoleto original) {
        if (original == null) {
            return null;
        }

        return SicoobBoleto.builder()
                // Identificação - mascara codigo de barras e nosso numero
                .id(original.getId())
                .barcode(maskKeepingLast(original.getBarcode(), 4))
                .ourNumber(maskKeepingLast(original.getOurNumber(), 4))
                .documentNumber(original.getDocumentNumber())

                // Situação - mantém
                .statusCode(original.getStatusCode())
                .statusDescription(original.getStatusDescription())

                // Partes - mascara documento e anonimiza nome
                .payeeName(anonymizeText("Beneficiário"))
                .payeeTaxId(maskKeepingLast(original.getPayeeTaxId(), 4))
                .payerName(anonymizeText("Pagador"))
                .payerTaxId(maskKeepingLast(original.getPayerTaxId(), 4))

                // Datas - mantém
                .issueDate(original.getIssueDate())
                .dueDate(original.getDueDate())
                .paymentDate(original.getPaymentDate())
                .paymentDeadline(original.getPaymentDeadline())
                .daysToDueDate(original.getDaysToDueDate())
                .issueYear(original.getIssueYear())
                .issueMonth(original.getIssueMonth())

                // Valores - randomiza
                .amount(randomizeValue(original.getAmount()))
                .paidAmount(randomizeValue(original.getPaidAmount()))
                .discountAmount(randomizeValue(original.getDiscountAmount()))
                .interestAmount(randomizeValue(original.getInterestAmount()))
                .fineAmount(randomizeValue(original.getFineAmount()))

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
                .build();
    }

    /**
     * O codigo de barras do boleto carrega o valor e a data de vencimento
     * embutidos, alem de identificar o titulo. Mascarar preservando apenas os
     * ultimos digitos mantem o comprimento sem entregar o conteudo.
     */
    private static String maskKeepingLast(String value, int visible) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= visible) {
            return "*".repeat(value.length());
        }
        return "*".repeat(value.length() - visible) + value.substring(value.length() - visible);
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
