package com.prestobr.financeiro.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "Response.SicoobExtratoSummary", description = "Resumo do extrato Sicoob agrupado por ano e mês de movimento")
public record SicoobExtratoSummaryResponse(
        Integer year,
        Integer month,

        long entryCount,
        long creditCount,
        long debitCount,

        BigDecimal creditAmount,
        BigDecimal debitAmount,

        @Schema(description = "Créditos menos débitos do período")
        BigDecimal netMovement,

        @Schema(description = "Saldo de abertura do período, vindo da coluna saldo_anterior")
        BigDecimal openingBalance,

        @Schema(description = "Saldo de abertura mais o movimento líquido do período. Calculado, e não lido da coluna saldo_atual, que guarda o saldo final do extrato replicado em todas as linhas")
        BigDecimal calculatedClosingBalance
) {}
