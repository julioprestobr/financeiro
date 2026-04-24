package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "Response.AccountPayable", description = "Dados de um AccountPayable")
public record AccountPayableResponse(
                String titleCode,
                String purchaseCode,

                String companyCode,
                String companyName,

                String vendorCode,
                String vendorName,
                String vendorTradeName,
                String vendorCnpj,
                String vendorCpf,

                String carrierCode,
                String carrierName,
                String carrierTradeName,
                String carrierCnpj,
                String carrierCpf,

                String providerCode,
                String nomePrestador,
                String providerTradeName,
                String providerCnpj,
                String providerCpf,

                String paymentStatus,
                String statusName,

                String documentType,
                String documentTypeName,

                String costCenterCode,
                String costCenterName,

                String subCostCenterCode,
                String subCostCenterName,

                String accountPlan,
                String accountPlanName,

                String departmentCode,
                String contract,

                LocalDateTime emissionDate,
                LocalDateTime dueDate,
                LocalDateTime entryDate,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,

                BigDecimal titleValue,
                BigDecimal paidValue,
                BigDecimal balanceValue,
                BigDecimal grossValue,
                BigDecimal discountValue,
                BigDecimal surchargeValue,
                BigDecimal movementValue,
                BigDecimal otherValues,
                BigDecimal monetaryCorrection,

                Boolean isFullyPaid,
                Boolean isProvision,

                String titleStatus,
                String titleType,
                String operation,
                String paymentMethod,
                String paymentOption,

                String installmentNumber,
                String referenceMonth,
                String period,
                String assessmentPeriod,
                String referencePeriod,
                Integer calculationYear,
                Integer daysOverdue,

                String description,
                String notes,

                String taxpayerDocument,
                String stateRegistration,
                String cityCode,
                String state,

                Integer paymentCounter,
                String createdBy,
                String updatedBy,

                LocalDateTime snapshotDatetime
) {
    public static AccountPayableResponse from(AccountPayable ap) {
        return new AccountPayableResponse(
                ap.getTitleCode(),
                ap.getPurchaseCode(),
                ap.getCompanyCode(),
                ap.getCompanyName(),
                ap.getVendorCode(),
                ap.getVendorName(),
                ap.getVendorTradeName(),
                ap.getVendorCnpj(),
                ap.getVendorCpf(),
                ap.getCarrierCode(),
                ap.getCarrierName(),
                ap.getCarrierTradeName(),
                ap.getCarrierCnpj(),
                ap.getCarrierCpf(),
                ap.getProviderCode(),
                ap.getProviderName(),
                ap.getProviderTradeName(),
                ap.getProviderCnpj(),
                ap.getProviderCpf(),
                ap.getPaymentStatus(),
                ap.getStatusName(),
                ap.getDocumentType(),
                ap.getDocumentTypeName(),
                ap.getCostCenterCode(),
                ap.getCostCenterName(),
                ap.getSubCostCenterCode(),
                ap.getSubCostCenterName(),
                ap.getAccountPlan(),
                ap.getAccountPlanName(),
                ap.getDepartmentCode(),
                ap.getContract(),
                ap.getEmissionDate(),
                ap.getDueDate(),
                ap.getEntryDate(),
                ap.getCreatedAt(),
                ap.getUpdatedAt(),
                ap.getTitleValue(),
                ap.getPaidValue(),
                ap.getBalanceValue(),
                ap.getGrossValue(),
                ap.getDiscountValue(),
                ap.getSurchargeValue(),
                ap.getMovementValue(),
                ap.getOtherValues(),
                ap.getMonetaryCorrection(),
                ap.getIsFullyPaid(),
                ap.getIsProvision(),
                ap.getTitleStatus(),
                ap.getTitleType(),
                ap.getOperation(),
                ap.getPaymentMethod(),
                ap.getPaymentOption(),
                ap.getInstallmentNumber(),
                ap.getReferenceMonth(),
                ap.getPeriod(),
                ap.getAssessmentPeriod(),
                ap.getReferencePeriod(),
                ap.getCalculationYear(),
                ap.getDaysOverdue(),
                ap.getDescription(),
                ap.getNotes(),
                ap.getTaxpayerDocument(),
                ap.getStateRegistration(),
                ap.getCityCode(),
                ap.getState(),
                ap.getPaymentCounter(),
                ap.getCreatedBy(),
                ap.getUpdatedBy(),
                ap.getSnapshotDatetime()
        );
    }
}
