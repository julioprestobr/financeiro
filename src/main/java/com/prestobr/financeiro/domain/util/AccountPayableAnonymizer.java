package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.AccountPayable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;

public class AccountPayableAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static AccountPayable anonymize(AccountPayable original) {
        if (original == null) {
            return null;
        }

        return AccountPayable.builder()
                // Identificação
                .titleCode(generateCode("TIT"))
                .purchaseCode(generateCode("CMP"))

                // Empresa - mantém código, anonimiza nome
                .companyCode(original.getCompanyCode())
                .companyName(anonymizeText("Empresa"))

                // Fornecedor - anonimiza tudo
                .vendorCode(generateCode("FOR"))
                .vendorName(anonymizeText("Fornecedor"))
                .vendorTradeName(anonymizeText("Fantasia"))
                .vendorCnpj(anonymizeCnpj())
                .vendorCpf(anonymizeCpf())

                // Transportador
                .carrierCode(generateCode("TRA"))
                .carrierName(anonymizeText("Transportador"))
                .carrierTradeName(anonymizeText("Fantasia"))
                .carrierCnpj(anonymizeCnpj())
                .carrierCpf(anonymizeCpf())

                // Prestador
                .providerCode(generateCode("PRE"))
                .providerName(anonymizeText("Prestador"))
                .providerTradeName(anonymizeText("Fantasia"))
                .providerCnpj(anonymizeCnpj())
                .providerCpf(anonymizeCpf())

                // Status - mantém código, anonimiza nome
                .paymentStatus(original.getPaymentStatus())
                .statusName(original.getStatusName())

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

                // Setor e Contrato
                .departmentCode(original.getDepartmentCode())
                .contract(generateCode("CTR"))

                // Datas - mantém
                .emissionDate(original.getEmissionDate())
                .dueDate(original.getDueDate())
                .entryDate(original.getEntryDate())
                .createdAt(original.getCreatedAt())
                .updatedAt(original.getUpdatedAt())

                // Valores - randomiza
                .titleValue(randomizeValue(original.getTitleValue()))
                .paidValue(randomizeValue(original.getPaidValue()))
                .balanceValue(randomizeValue(original.getBalanceValue()))
                .grossValue(randomizeValue(original.getGrossValue()))
                .discountValue(randomizeValue(original.getDiscountValue()))
                .surchargeValue(randomizeValue(original.getSurchargeValue()))
                .movementValue(randomizeValue(original.getMovementValue()))
                .otherValues(randomizeValue(original.getOtherValues()))
                .monetaryCorrection(randomizeValue(original.getMonetaryCorrection()))

                // Flags - mantém
                .isFullyPaid(original.getIsFullyPaid())
                .isProvision(original.getIsProvision())

                // Classificação - mantém
                .titleStatus(original.getTitleStatus())
                .titleType(original.getTitleType())
                .operation(original.getOperation())
                .paymentMethod(original.getPaymentMethod())
                .paymentOption(original.getPaymentOption())

                // Parcela / Competência - mantém
                .installmentNumber(original.getInstallmentNumber())
                .referenceMonth(original.getReferenceMonth())
                .period(original.getPeriod())
                .assessmentPeriod(original.getAssessmentPeriod())
                .referencePeriod(original.getReferencePeriod())
                .calculationYear(original.getCalculationYear())
                .daysOverdue(original.getDaysOverdue())

                // Texto / Histórico - anonimiza
                .description(anonymizeText("Histórico"))
                .notes(anonymizeText("Observação"))

                // Fiscal - anonimiza
                .taxpayerDocument(anonymizeCpfCnpj(original.getTaxpayerDocument()))
                .stateRegistration("ISENTO")
                .cityCode(original.getCityCode())
                .state(original.getState())

                // Auditoria - anonimiza operadores
                .paymentCounter(original.getPaymentCounter())
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

    private static String anonymizeCpf() {
        return String.format("%03d.***.**%d-%02d",
                random.nextInt(1000), random.nextInt(10), random.nextInt(100));
    }

    private static String anonymizeCnpj() {
        return String.format("%02d.***.***/%04d-%02d",
                random.nextInt(100), random.nextInt(10000), random.nextInt(100));
    }

    private static String anonymizeCpfCnpj(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        int length = documento.replaceAll("\\D", "").length();
        return length == 11 ? anonymizeCpf() : anonymizeCnpj();
    }
}