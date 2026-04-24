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
public class AccountPayable {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private String titleCode;
    private String purchaseCode;

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String companyCode;
    private String companyName;

    // =========================================================================
    // FORNECEDOR
    // =========================================================================
    private String vendorCode;
    private String vendorName;
    private String vendorTradeName;
    private String vendorCnpj;
    private String vendorCpf;

    // =========================================================================
    // TRANSPORTADOR
    // =========================================================================
    private String carrierCode;
    private String carrierName;
    private String carrierTradeName;
    private String carrierCnpj;
    private String carrierCpf;

    // =========================================================================
    // PRESTADOR
    // =========================================================================
    private String providerCode;
    private String providerName;
    private String providerTradeName;
    private String providerCnpj;
    private String providerCpf;

    // =========================================================================
    // STATUS
    // =========================================================================
    private String paymentStatus;
    private String statusName;

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
    // SETOR E CONTRATO
    // =========================================================================
    private String departmentCode;
    private String contract;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDateTime emissionDate;
    private LocalDateTime dueDate;
    private LocalDateTime entryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================================================================
    // VALORES
    // =========================================================================
    private BigDecimal titleValue;
    private BigDecimal paidValue;
    private BigDecimal balanceValue;
    private BigDecimal grossValue;
    private BigDecimal discountValue;
    private BigDecimal surchargeValue;
    private BigDecimal movementValue;
    private BigDecimal otherValues;
    private BigDecimal monetaryCorrection;

    // =========================================================================
    // FLAGS
    // =========================================================================
    private Boolean isFullyPaid;
    private Boolean isProvision;

    // =========================================================================
    // CLASSIFICAÇÃO
    // =========================================================================
    private String titleStatus;
    private String titleType;
    private String operation;
    private String paymentMethod;
    private String paymentOption;

    // =========================================================================
    // PARCELA / COMPETÊNCIA
    // =========================================================================
    private String installmentNumber;
    private String referenceMonth;
    private String period;
    private String assessmentPeriod;
    private String referencePeriod;
    private Integer calculationYear;
    private Integer daysOverdue;

    // =========================================================================
    // TEXTO / HISTÓRICO
    // =========================================================================
    private String description;
    private String notes;

    // =========================================================================
    // FISCAL
    // =========================================================================
    private String taxpayerDocument;
    private String stateRegistration;
    private String cityCode;
    private String state;

    // =========================================================================
    // AUDITORIA
    // =========================================================================
    private Integer paymentCounter;
    private String createdBy;
    private String updatedBy;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;
}