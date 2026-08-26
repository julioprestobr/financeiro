package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "SicoobExtratoPageRequest", description = "Parâmetros de filtro, paginação e ordenação para consultas de extrato Sicoob")
public record SicoobExtratoPageRequest(
        @Schema(description = "Identificador único do lançamento (coluna codigo_historico)", example = "6CDE3D-CA610D-274F62-627CE9-565EE1")
        String entryId,

        @Schema(description = "Número do documento. Atenção: não é chave única, há duplicidades")
        String documentNumber,

        @Schema(description = "Tipo do lançamento: DEBITO ou CREDITO", example = "DEBITO")
        String entryType,

        @Schema(description = "Histórico do lançamento (busca parcial)", example = "PIX")
        String description,

        @Schema(description = "Data inicial do movimento", example = "2026-07-01")
        LocalDate movementDateFrom,

        @Schema(description = "Data final do movimento", example = "2026-08-31")
        LocalDate movementDateTo,

        @Schema(description = "Valor mínimo do lançamento")
        BigDecimal minAmount,

        @Schema(description = "Valor máximo do lançamento")
        BigDecimal maxAmount,

        @Schema(description = "Ano do movimento", example = "2026")
        Integer movementYear,

        @Schema(description = "Mês do movimento (1 a 12)", example = "8")
        @Min(1) @Max(12)
        Integer movementMonth,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: movementDate, amount, entryType, description, id. Direções: asc, desc",
                example = "[\"movementDate,desc\", \"amount,desc\"]"
        )
        List<String> sort
) {
    public SicoobExtratoPageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}
