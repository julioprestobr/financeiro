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
public class ProductStock {

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String companyCode;
    private String companyName;

    // =========================================================================
    // PRODUTO
    // =========================================================================
    private String productCode;
    private String productName;
    private String shortName;
    private String reference;
    private String barcode;
    private String groupCode;
    private String groupName;
    private String subgroupCode;
    private String subgroupName;
    private String brand;
    private String unit;
    private String fiscalClassification;
    private String productType;
    private String status;
    private String location;
    private String shelf;

    // =========================================================================
    // FORNECEDOR
    // =========================================================================
    private String vendorCode;
    private String vendorName;
    private String vendorTradeName;

    // =========================================================================
    // ESTOQUE
    // =========================================================================
    private BigDecimal stockQuantity;
    private BigDecimal blockedQuantity;
    private BigDecimal incomingQuantity;
    private BigDecimal outgoingQuantity;
    private BigDecimal pendingQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;

    // =========================================================================
    // LIMITES DE ESTOQUE
    // =========================================================================
    private BigDecimal maxStock;
    private BigDecimal minStock;
    private BigDecimal safetyStock;
    private Boolean belowMinimum;
    private Boolean aboveMaximum;

    // =========================================================================
    // CUSTO / PREÇO
    // =========================================================================
    private BigDecimal cost;
    private BigDecimal averageCost;
    private BigDecimal purchaseCost;
    private BigDecimal averagePurchaseCost;
    private BigDecimal salePrice;
    private BigDecimal minSalePrice;
    private BigDecimal markup;
    private BigDecimal stockValue;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDateTime inventoryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
