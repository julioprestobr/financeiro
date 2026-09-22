package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.Faturamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

public class FaturamentoAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static Faturamento anonymize(Faturamento original) {
        if (original == null) {
            return null;
        }

        return Faturamento.builder()
                // Identificação - mantém
                .saleCode(original.getSaleCode())
                .invoiceNumber(original.getInvoiceNumber())

                // Empresa - mantém código, anonimiza nome
                .companyCode(original.getCompanyCode())
                .companyName(anonymizeText("Empresa"))

                // Cliente - mantém código, anonimiza nome e grupo
                .customerCode(original.getCustomerCode())
                .customerName(anonymizeText("Cliente"))
                .economicGroup(original.getEconomicGroup() == null ? null : anonymizeText("Grupo"))

                // Classificação - mantém, não identifica ninguém
                .operationCode(original.getOperationCode())
                .type(original.getType())
                .category(original.getCategory())
                .subcategory(original.getSubcategory())

                // Lançamento - mantém data, randomiza valor
                .saleDate(original.getSaleDate())
                .amount(randomizeValue(original.getAmount()))
                .build();
    }

    private static String anonymizeText(String prefix) {
        return prefix + " Anonimizado #" + random.nextInt(10000);
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
