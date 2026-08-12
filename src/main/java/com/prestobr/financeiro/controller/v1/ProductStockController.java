package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.ProductStockPageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.ProductStockResponse;
import com.prestobr.financeiro.service.ProductStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/product-stock")
@RequiredArgsConstructor
@Tag(name = "Estoque de Produto", description = "Consulta de estoque de produto do Data Lake")
public class ProductStockController {

    private final ProductStockService productStockService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): stockQuantity, availableQuantity, stockValue, cost, salePrice, createdAt, updatedAt, inventoryDate. Direções disponíveis: asc, desc. Exemplo: [\"stockQuantity,asc\",\"stockValue,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca estoque de produto com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<ProductStockResponse> search(@RequestBody ProductStockPageRequest request) {
        return productStockService.search(request);
    }

    @GetMapping("/product")
    @Operation(summary = "Busca o estoque de um produto pelo código, em todas as empresas")
    public List<ProductStockResponse> getByCodigoProduto(
            @Parameter(description = "Código do produto")
            @RequestParam("codigo") String codigoProduto) {
        return productStockService.getByCodigoProduto(codigoProduto);
    }

}
