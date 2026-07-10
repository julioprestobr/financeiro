package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.AccountReceivable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;

public class AccountReceivableAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static AccountReceivable anonymize(AccountReceivable original) {
        if (original == null) {
            return null;
        }

        return AccountReceivable.builder()
                // Identificação
                .titleCode(generateCode("TIT"))
                .linkCode(original.getLinkCode())
                .generatedTitleCode(original.getGeneratedTitleCode())

                // Empresa - mantém código, anonimiza nome
                .companyCode(original.getCompanyCode())
                .companyName(anonymizeText("Empresa"))

                // Cliente - anonimiza tudo
                .clientCode(generateCode("CLI"))
                .creditClientCode(generateCode("CLI"))
                .clientName(anonymizeText("Cliente"))
                .clientTradeName(anonymizeText("Fantasia"))
                .clientCnpj(anonymizeCnpj())
                .clientCpf(anonymizeCpf())

                // Vendedor
                .sellerCode(generateCode("VEN"))
                .sellerName(anonymizeText("Vendedor"))

                // Status - mantém
                .receiptStatus(original.getReceiptStatus())
                .receiptStatusName(original.getReceiptStatusName())

                // Tipo Documento - mantém
                .documentType(original.getDocumentType())
                .documentTypeName(original.getDocumentTypeName())

                // Centro de Custo - mantém código, anonimiza nome
                .costCenterCode(original.getCostCenterCode())
                .costCenterName(anonymizeText("Centro Custo"))

                // Subcentro de Custo - mantém código, anonimiza nome
                .subCostCenterCode(original.getSubCostCenterCode())
                .subCostCenterName(anonymizeText("Subcentro"))

                // Plano de Conta - mantém código, anonimiza nome
                .accountPlan(original.getAccountPlan())
                .accountPlanName(anonymizeText("Plano Conta"))

                // Contrato
                .contract(generateCode("CTR"))

                // Datas - mantém
                .emissionDate(original.getEmissionDate())
                .dueDate(original.getDueDate())
                .originalDueDate(original.getOriginalDueDate())
                .confirmationDate(original.getConfirmationDate())
                .createdAt(original.getCreatedAt())
                .updatedAt(original.getUpdatedAt())

                // Valores - randomiza
                .titleValue(randomizeValue(original.getTitleValue()))
                .receivedValue(randomizeValue(original.getReceivedValue()))
                .grossValue(randomizeValue(original.getGrossValue()))
                .discountValue(randomizeValue(original.getDiscountValue()))
                .surchargeValue(randomizeValue(original.getSurchargeValue()))
                .movementValue(randomizeValue(original.getMovementValue()))
                .benefitDiscountValue(randomizeValue(original.getBenefitDiscountValue()))

                // Flags - mantém
                .isProvision(original.getIsProvision())
                .isProvision2(original.getIsProvision2())

                // Classificação - mantém
                .titleStatus(original.getTitleStatus())
                .titleStatusName(original.getTitleStatusName())
                .titleType(original.getTitleType())

                // Competência - mantém
                .referenceMonth(original.getReferenceMonth())
                .daysOverdue(original.getDaysOverdue())

                // Texto / Histórico - anonimiza
                .description(anonymizeText("Histórico"))
                .notes(anonymizeText("Observação"))

                // Emitente / dados bancários - anonimiza
                .issuerName(anonymizeText("Emitente"))
                .issuerTradeName(anonymizeText("Fantasia"))
                .bankAgencyNumber(generateCode("AG"))
                .bankAccountNumber(generateCode("CC"))
                .checkNumber(generateCode("CHQ"))
                .processNumber(generateCode("PROC"))
                .creditCode(original.getCreditCode())
                .authorizationCode(original.getAuthorizationCode())
                .telemetry(original.getTelemetry())
                .fiscalDocumentNumber(original.getFiscalDocumentNumber())
                .serasa(original.getSerasa())
                .batchNumber(original.getBatchNumber())

                // Auditoria - anonimiza operadores
                .createdBy(anonymizeText("Operador"))
                .updatedBy(anonymizeText("Operador"))
                .confirmedBy(anonymizeText("Operador"))

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

    private static String anonymizeCpf() {
        return String.format("%03d.***.**%d-%02d",
                random.nextInt(1000), random.nextInt(10), random.nextInt(100));
    }

    private static String anonymizeCnpj() {
        return String.format("%02d.***.***/%04d-%02d",
                random.nextInt(100), random.nextInt(10000), random.nextInt(100));
    }
}
