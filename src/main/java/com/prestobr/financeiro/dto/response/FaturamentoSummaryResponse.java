package com.prestobr.financeiro.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        name = "Response.FaturamentoSummary",
        description = "Resumo de faturamento por ano e mês. Separa receita do total porque a tabela mistura faturamento com custo, remessa e baixa"
)
public record FaturamentoSummaryResponse(
        Integer year,
        Integer month,

        @Schema(description = "Lançamentos de qualquer categoria no período")
        long entryCount,

        @Schema(description = "Soma de todos os lançamentos, incluindo custo, remessa e baixa. NÃO é faturamento")
        BigDecimal totalAmount,

        @Schema(description = "Lançamentos com categoria Receita")
        long revenueCount,

        @Schema(description = "Faturamento de fato: soma apenas dos lançamentos com categoria Receita")
        BigDecimal revenueAmount,

        @Schema(description = "Clientes distintos com lançamento de receita no período")
        long revenueCustomerCount
) {}
