package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "AccountBalanceHistoryPageRequest", description = "Parâmetros de paginação e ordenação para consultas de histórico de saldo")
public record AccountBalanceHistoryPageRequest(
        @Schema(description = "Código da conta bancária")
        String accountCode,

        @Schema(description = "Código da empresa")
        String companyCode,

        @Schema(description = "Código do banco")
        String bankCode,

        @Schema(description = "Se o dia é dia útil")
        Boolean isBusinessDay,

        @Schema(description = "Data inicial de referência", example = "2026-01-01")
        @NotNull
        LocalDate referenceDateFrom,

        @Schema(description = "Data final de referência", example = "2026-03-31")
        LocalDate referenceDateTo,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: referenceDate, accumulatedBalance, dailyCredits, dailyDebits, netDailyMovement. Direções: asc, desc",
                example = "[\"referenceDate,asc\", \"accumulatedBalance,desc\"]"
        )
        List<String> sort
) {
    public AccountBalanceHistoryPageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}
