package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.AccountReceivable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "Response.AccountReceivable", description = "Dados de um AccountReceivable")
public record AccountReceivableResponse(
        String titleCode,
        String linkCode,
        String generatedTitleCode,

        String companyCode,
        String companyName,

        String clientCode,
        String creditClientCode,
        String clientName,
        String clientTradeName,
        String clientCnpj,
        String clientCpf,

        String sellerCode,
        String sellerName,

        Integer receiptStatus,
        String receiptStatusName,

        String documentType,
        String documentTypeName,

        String costCenterCode,
        String costCenterName,

        String subCostCenterCode,
        String subCostCenterName,

        String accountPlan,
        String accountPlanName,

        String contract,

        LocalDateTime emissionDate,
        LocalDateTime dueDate,
        LocalDateTime originalDueDate,
        LocalDateTime confirmationDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        BigDecimal titleValue,
        BigDecimal receivedValue,
        BigDecimal grossValue,
        BigDecimal discountValue,
        BigDecimal surchargeValue,
        BigDecimal movementValue,
        BigDecimal benefitDiscountValue,

        Boolean isProvision,
        Boolean isProvision2,

        String titleStatus,
        String titleStatusName,
        Integer titleType,

        String referenceMonth,
        Integer daysOverdue,

        String description,
        String notes,

        String issuerName,
        String issuerTradeName,
        String bankAgencyNumber,
        String bankAccountNumber,
        String checkNumber,
        String processNumber,
        String creditCode,
        String authorizationCode,
        String telemetry,
        String fiscalDocumentNumber,
        String serasa,
        Integer batchNumber,

        String createdBy,
        String updatedBy,
        String confirmedBy,

        LocalDateTime snapshotDatetime
) {
    public static AccountReceivableResponse from(AccountReceivable ar) {
        return new AccountReceivableResponse(
                ar.getTitleCode(),
                ar.getLinkCode(),
                ar.getGeneratedTitleCode(),
                ar.getCompanyCode(),
                ar.getCompanyName(),
                ar.getClientCode(),
                ar.getCreditClientCode(),
                ar.getClientName(),
                ar.getClientTradeName(),
                ar.getClientCnpj(),
                ar.getClientCpf(),
                ar.getSellerCode(),
                ar.getSellerName(),
                ar.getReceiptStatus(),
                ar.getReceiptStatusName(),
                ar.getDocumentType(),
                ar.getDocumentTypeName(),
                ar.getCostCenterCode(),
                ar.getCostCenterName(),
                ar.getSubCostCenterCode(),
                ar.getSubCostCenterName(),
                ar.getAccountPlan(),
                ar.getAccountPlanName(),
                ar.getContract(),
                ar.getEmissionDate(),
                ar.getDueDate(),
                ar.getOriginalDueDate(),
                ar.getConfirmationDate(),
                ar.getCreatedAt(),
                ar.getUpdatedAt(),
                ar.getTitleValue(),
                ar.getReceivedValue(),
                ar.getGrossValue(),
                ar.getDiscountValue(),
                ar.getSurchargeValue(),
                ar.getMovementValue(),
                ar.getBenefitDiscountValue(),
                ar.getIsProvision(),
                ar.getIsProvision2(),
                ar.getTitleStatus(),
                ar.getTitleStatusName(),
                ar.getTitleType(),
                ar.getReferenceMonth(),
                ar.getDaysOverdue(),
                ar.getDescription(),
                ar.getNotes(),
                ar.getIssuerName(),
                ar.getIssuerTradeName(),
                ar.getBankAgencyNumber(),
                ar.getBankAccountNumber(),
                ar.getCheckNumber(),
                ar.getProcessNumber(),
                ar.getCreditCode(),
                ar.getAuthorizationCode(),
                ar.getTelemetry(),
                ar.getFiscalDocumentNumber(),
                ar.getSerasa(),
                ar.getBatchNumber(),
                ar.getCreatedBy(),
                ar.getUpdatedBy(),
                ar.getConfirmedBy(),
                ar.getSnapshotDatetime()
        );
    }
}
