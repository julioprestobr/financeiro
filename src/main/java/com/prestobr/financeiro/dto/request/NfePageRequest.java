package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "NfePageRequest", description = "Parâmetros de filtro, paginação e ordenação para consultas de notas fiscais eletrônicas")
public record NfePageRequest(
        @Schema(description = "Chave de acesso da NFe (44 dígitos)", example = "31200122527311000146550020000176761361858509")
        String accessKey,

        @Schema(description = "Número da nota fiscal")
        String invoiceNumber,

        @Schema(description = "Série da nota. Atenção: é texto com zero à esquerda", example = "002")
        String series,

        @Schema(description = "Tipo da nota: Emitida ou Recebida", example = "Emitida")
        String invoiceType,

        @Schema(description = "Situação da nota: Autorizada, Cancelada, Carta de Correcao ou Denegada", example = "Autorizada")
        String status,

        @Schema(description = "Se a nota foi cancelada. Independente do filtro status: existe registro com status Cancelada e este campo false")
        Boolean canceled,

        @Schema(description = "CNPJ ou CPF do emitente (somente dígitos, sem máscara)", example = "22527311000146")
        String issuerTaxId,

        @Schema(description = "CNPJ ou CPF do destinatário (somente dígitos, sem máscara)")
        String recipientTaxId,

        @Schema(description = "Nome do emitente (busca parcial). A base tem grafias divergentes para o mesmo CNPJ, prefira filtrar por issuerTaxId")
        String issuerName,

        @Schema(description = "Nome do destinatário (busca parcial). A base tem grafias divergentes para o mesmo CNPJ, prefira filtrar por recipientTaxId")
        String recipientName,

        @Schema(description = "Ano de emissão", example = "2026")
        Integer issueYear,

        @Schema(description = "Mês de emissão (1 a 12)", example = "8")
        @Min(1) @Max(12)
        Integer issueMonth,

        @Schema(description = "Data inicial de emissão", example = "2026-01-01")
        LocalDate issueDateFrom,

        @Schema(description = "Data final de emissão", example = "2026-08-31")
        LocalDate issueDateTo,

        @Schema(description = "Valor total mínimo da nota")
        BigDecimal minAmount,

        @Schema(description = "Valor total máximo da nota")
        BigDecimal maxAmount,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: issueDate, totalAmount, invoiceNumber, importDate, issueYear, daysUntilImport, accessKey. Direções: asc, desc",
                example = "[\"issueDate,desc\", \"totalAmount,desc\"]"
        )
        List<String> sort
) {
    public NfePageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}
