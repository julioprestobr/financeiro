package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "FaturamentoPageRequest", description = "Parâmetros de filtro, paginação e ordenação para consultas de faturamento")
public record FaturamentoPageRequest(
        @Schema(description = "Código da venda. É a chave única da tabela", example = "F18446")
        String saleCode,

        @Schema(description = "Número da nota. Atenção: não é chave única, a mesma nota aparece em várias linhas, e 4,3% dos lançamentos não têm nota")
        String invoiceNumber,

        @Schema(description = "Código da empresa: 00, 01 ou 02", example = "00")
        String companyCode,

        @Schema(description = "Código do cliente", example = "4460")
        Integer customerCode,

        @Schema(description = "Nome do cliente (busca parcial)")
        String customerName,

        @Schema(description = "Grupo econômico do cliente. 56% dos lançamentos não têm grupo", example = "PREFEITURAS")
        String economicGroup,

        @Schema(description = "Código da operação de venda", example = "19")
        String operationCode,

        @Schema(description = "Tipo do lançamento: Venda ou Serviço", example = "Venda")
        String type,

        @Schema(
                description = "Categoria do lançamento. Atenção: só 'Receita' é faturamento de fato. As demais são custo, remessa, baixa e controle interno",
                example = "Receita"
        )
        String category,

        @Schema(description = "Subcategoria do lançamento", example = "Locação")
        String subcategory,

        @Schema(description = "Data inicial do lançamento", example = "2026-01-01")
        LocalDate saleDateFrom,

        @Schema(description = "Data final do lançamento (inclusiva)", example = "2026-12-31")
        LocalDate saleDateTo,

        @Schema(description = "Ano do lançamento", example = "2026")
        Integer year,

        @Schema(description = "Mês do lançamento (1 a 12)", example = "9")
        @Min(1) @Max(12)
        Integer month,

        @Schema(description = "Valor mínimo do lançamento")
        BigDecimal minAmount,

        @Schema(description = "Valor máximo do lançamento")
        BigDecimal maxAmount,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: saleDate, amount, customerCode, saleCode, invoiceNumber. Direções: asc, desc. Atenção: invoiceNumber ordena como texto, porque 13 notas não são numéricas",
                example = "[\"saleDate,desc\", \"amount,desc\"]"
        )
        List<String> sort
) {
    public FaturamentoPageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}
