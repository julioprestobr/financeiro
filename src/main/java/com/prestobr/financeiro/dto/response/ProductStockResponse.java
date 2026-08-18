package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.ProductStock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "Response.ProductStock", description = "Dados de um ProductStock")
public record ProductStockResponse(
        String companyCode,
        String companyName,

        String productCode,
        String productName,
        String shortName,
        String reference,
        String barcode,
        String groupCode,
        String groupName,
        String subgroupCode,
        String subgroupName,
        String brand,
        String unit,
        String fiscalClassification,
        String productType,
        String status,
        String location,
        String shelf,

        String vendorCode,
        String vendorName,
        String vendorTradeName,

        BigDecimal stockQuantity,
        BigDecimal blockedQuantity,
        BigDecimal incomingQuantity,
        BigDecimal outgoingQuantity,
        BigDecimal pendingQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,

        BigDecimal maxStock,
        BigDecimal minStock,
        BigDecimal safetyStock,
        Boolean belowMinimum,
        Boolean aboveMaximum,

        BigDecimal cost,
        BigDecimal averageCost,
        BigDecimal purchaseCost,
        BigDecimal averagePurchaseCost,
        BigDecimal salePrice,
        BigDecimal minSalePrice,
        BigDecimal markup,
        BigDecimal stockValue,

        LocalDateTime inventoryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        String createdBy,
        String updatedBy,

        LocalDateTime snapshotDatetime
) {
    public static ProductStockResponse from(ProductStock ps) {
        return new ProductStockResponse(
                ps.getCompanyCode(),
                ps.getCompanyName(),
                ps.getProductCode(),
                ps.getProductName(),
                ps.getShortName(),
                ps.getReference(),
                ps.getBarcode(),
                ps.getGroupCode(),
                ps.getGroupName(),
                ps.getSubgroupCode(),
                ps.getSubgroupName(),
                ps.getBrand(),
                ps.getUnit(),
                ps.getFiscalClassification(),
                ps.getProductType(),
                ps.getStatus(),
                ps.getLocation(),
                ps.getShelf(),
                ps.getVendorCode(),
                ps.getVendorName(),
                ps.getVendorTradeName(),
                ps.getStockQuantity(),
                ps.getBlockedQuantity(),
                ps.getIncomingQuantity(),
                ps.getOutgoingQuantity(),
                ps.getPendingQuantity(),
                ps.getReservedQuantity(),
                ps.getAvailableQuantity(),
                ps.getMaxStock(),
                ps.getMinStock(),
                ps.getSafetyStock(),
                ps.getBelowMinimum(),
                ps.getAboveMaximum(),
                ps.getCost(),
                ps.getAverageCost(),
                ps.getPurchaseCost(),
                ps.getAveragePurchaseCost(),
                ps.getSalePrice(),
                ps.getMinSalePrice(),
                ps.getMarkup(),
                ps.getStockValue(),
                ps.getInventoryDate(),
                ps.getCreatedAt(),
                ps.getUpdatedAt(),
                ps.getCreatedBy(),
                ps.getUpdatedBy(),
                ps.getSnapshotDatetime()
        );
    }
}
