package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.SicoobExtrato;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "Response.SicoobExtrato", description = "Lançamento do extrato Sicoob")
public record SicoobExtratoResponse(
        Integer id,

        @Schema(description = "Identificador único do lançamento (coluna codigo_historico)")
        String entryId,

        String documentNumber,

        LocalDate movementDate,
        String description,

        @Schema(description = "DEBITO ou CREDITO. O valor em amount é sempre positivo: o sinal do lançamento está aqui")
        String entryType,

        @Schema(description = "Valor do lançamento, sempre positivo")
        BigDecimal amount,

        @Schema(description = "Saldo de abertura do mês do lançamento, não o saldo antes desta linha")
        BigDecimal openingBalance,

        @Schema(description = "Saldo final da conta no extrato, replicado em todas as linhas. Não é saldo progressivo por lançamento: use o endpoint /summary para a leitura correta dos saldos por período")
        BigDecimal closingBalance,

        Integer movementYear,
        Integer movementMonth,

        LocalDateTime snapshotDatetime
) {
    public static SicoobExtratoResponse from(SicoobExtrato e) {
        return new SicoobExtratoResponse(
                e.getId(),
                e.getEntryId(),
                e.getDocumentNumber(),
                e.getMovementDate(),
                e.getDescription(),
                e.getEntryType(),
                e.getAmount(),
                e.getOpeningBalance(),
                e.getClosingBalance(),
                e.getMovementYear(),
                e.getMovementMonth(),
                e.getSnapshotDatetime()
        );
    }
}
