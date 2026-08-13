package com.prestobr.financeiro.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "Response.NfeSummary", description = "Resumo de notas fiscais agrupado por ano e mês de emissão")
public record NfeSummaryResponse(
        Integer year,
        Integer month,

        long issuedCount,
        long receivedCount,
        long canceledCount,
        long totalCount,

        BigDecimal issuedAmount,
        BigDecimal receivedAmount
) {}
