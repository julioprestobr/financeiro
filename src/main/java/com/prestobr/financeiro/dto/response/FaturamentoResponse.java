package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.Faturamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "Response.Faturamento", description = "Lançamento de faturamento")
public record FaturamentoResponse(
        @Schema(description = "Código da venda, chave única da tabela")
        String saleCode,

        @Schema(description = "Número da nota. Não é único e pode ser nulo")
        String invoiceNumber,

        String companyCode,
        String companyName,

        Integer customerCode,
        String customerName,

        @Schema(description = "Grupo econômico do cliente, nulo em 56% dos lançamentos")
        String economicGroup,

        String operationCode,
        String type,

        @Schema(description = "Categoria do lançamento. Só 'Receita' é faturamento de fato: as demais são custo, remessa, baixa e controle interno")
        String category,

        String subcategory,

        LocalDateTime saleDate,
        BigDecimal amount
) {
    public static FaturamentoResponse from(Faturamento f) {
        return new FaturamentoResponse(
                f.getSaleCode(),
                f.getInvoiceNumber(),
                f.getCompanyCode(),
                f.getCompanyName(),
                f.getCustomerCode(),
                f.getCustomerName(),
                f.getEconomicGroup(),
                f.getOperationCode(),
                f.getType(),
                f.getCategory(),
                f.getSubcategory(),
                f.getSaleDate(),
                f.getAmount()
        );
    }
}
