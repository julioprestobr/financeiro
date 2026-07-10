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
public class AccountReceipt {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private String documentNumber;

    // =========================================================================
    // CONTA BANCÁRIA
    // =========================================================================
    private String accountCode;
    private String accountName;
    private String bankCode;

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String companyCode;
    private String companyName;

    // =========================================================================
    // CLASSIFICAÇÃO
    // =========================================================================
    private String type;
    private Boolean isSettled;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDateTime movementDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================================================================
    // VALORES
    // =========================================================================
    private BigDecimal documentValue;
    private BigDecimal currentBalance;
    private BigDecimal settledBalance;

    // =========================================================================
    // VENDAS VINCULADAS
    // =========================================================================
    private String sale1;
    private String sale2;
    private String sale3;
    private String sale4;
    private String sale5;

    // =========================================================================
    // TEXTO / HISTÓRICO
    // =========================================================================
    private String description;
    private String destination;
    private String notes;

    // =========================================================================
    // AUDITORIA
    // =========================================================================
    private String createdBy;
    private String updatedBy;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;
}