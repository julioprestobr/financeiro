package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@Schema(name = "ProductStockPageRequest", description = "Parâmetros de paginação e ordenação para consultas de estoque de produto")
public record ProductStockPageRequest(
        @Schema(description = "Código da empresa")
        String companyCode,

        @Schema(description = "Código do produto")
        String productCode,

        @Schema(description = "Código de barras")
        String barcode,

        @Schema(description = "Código do fornecedor")
        String vendorCode,

        @Schema(description = "Código do grupo")
        String groupCode,

        @Schema(description = "Código do subgrupo")
        String subgroupCode,

        @Schema(description = "Marca")
        String brand,

        @Schema(description = "Situação do produto")
        String status,

        @Schema(description = "Nome do produto (busca parcial)")
        String productName,

        @Schema(description = "Se o estoque está abaixo do mínimo")
        Boolean belowMinimum,

        @Schema(description = "Se o estoque está acima do máximo")
        Boolean aboveMaximum,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: stockQuantity, availableQuantity, stockValue, cost, salePrice, createdAt, updatedAt, inventoryDate. Direções: asc, desc",
                example = "[\"stockQuantity,asc\", \"stockValue,desc\"]"
        )
        List<String> sort
) {
    public ProductStockPageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}
