package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "AccountReceiptPageRequest", description = "Parâmetros de paginação e ordenação para consultas de recebimentos")
public record AccountReceiptPageRequest(
        @Schema(description = "Código da conta bancária")
        String accountCode,

        @Schema(description = "Código da empresa")
        String companyCode,

        @Schema(description = "Número do documento")
        String documentNumber,

        @Schema(description = "Tipo do lançamento: credito, debito")
        String type,

        @Schema(description = "Se o lançamento está compensado")
        Boolean isSettled,

        @Schema(description = "Histórico")
        String description,

        @Schema(description = "Data inicial da movimentação", example = "2026-01-01")
        @NotNull
        LocalDate movementDateFrom,

        @Schema(description = "Data final da movimentação", example = "2026-03-31")
        LocalDate movementDateTo,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: movementDate, createdAt, updatedAt, documentValue, currentBalance. Direções: asc, desc",
                example = "[\"movementDate,asc\", \"documentValue,desc\"]"
        )
        List<String> sort
) {
    public AccountReceiptPageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}