package com.prestobr.financeiro.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReceivable {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private String titleCode;
    private String linkCode;
    private String generatedTitleCode;

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String companyCode;
    private String companyName;

    // =========================================================================
    // CLIENTE
    // =========================================================================
    private String clientCode;
    private String creditClientCode;
    private String clientName;
    private String clientTradeName;
    private String clientCnpj;
    private String clientCpf;

    // =========================================================================
    // VENDEDOR
    // =========================================================================
    private String sellerCode;
    private String sellerName;

    // =========================================================================
    // STATUS
    // =========================================================================
    private Integer receiptStatus;
    private String receiptStatusName;

    // =========================================================================
    // TIPO DOCUMENTO
    // =========================================================================
    private String documentType;
    private String documentTypeName;

    // =========================================================================
    // CENTRO DE CUSTO
    // =========================================================================
    private String costCenterCode;
    private String costCenterName;

    // =========================================================================
    // SUBCENTRO DE CUSTO
    // =========================================================================
    private String subCostCenterCode;
    private String subCostCenterName;

    // =========================================================================
    // PLANO DE CONTA
    // =========================================================================
    private String accountPlan;
    private String accountPlanName;

    // =========================================================================
    // CONTRATO
    // =========================================================================
    private String contract;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDateTime emissionDate;
    private LocalDateTime dueDate;
    private LocalDateTime originalDueDate;
    private LocalDateTime confirmationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================================================================
    // VALORES
    // =========================================================================
    private BigDecimal titleValue;
    private BigDecimal receivedValue;
    private BigDecimal grossValue;
    private BigDecimal discountValue;
    private BigDecimal surchargeValue;
    private BigDecimal movementValue;
    private BigDecimal benefitDiscountValue;

    // =========================================================================
    // FLAGS
    // =========================================================================
    private Boolean isProvision;
    private Boolean isProvision2;

    // =========================================================================
    // CLASSIFICAÇÃO
    // =========================================================================
    private String titleStatus;
    private String titleStatusName;
    private Integer titleType;

    // =========================================================================
    // COMPETÊNCIA
    // =========================================================================
    private String referenceMonth;
    private Integer daysOverdue;

    // =========================================================================
    // TEXTO / HISTÓRICO
    // =========================================================================
    private String description;
    private String notes;

    // =========================================================================
    // EMITENTE / DADOS BANCÁRIOS
    // =========================================================================
    private String issuerName;
    private String issuerTradeName;
    private String bankAgencyNumber;
    private String bankAccountNumber;
    private String checkNumber;
    private String processNumber;
    private String creditCode;
    private String authorizationCode;
    private String telemetry;
    private String fiscalDocumentNumber;
    private String serasa;
    private Integer batchNumber;

    // =========================================================================
    // AUDITORIA
    // =========================================================================
    private String createdBy;
    private String updatedBy;
    private String confirmedBy;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;
}
