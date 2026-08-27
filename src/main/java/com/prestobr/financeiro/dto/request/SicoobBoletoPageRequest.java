package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "SicoobBoletoPageRequest", description = "Parâmetros de filtro, paginação e ordenação para consultas de boletos Sicoob")
public record SicoobBoletoPageRequest(
        @Schema(description = "Código de barras do boleto (44 dígitos)")
        String barcode,

        @Schema(description = "Nosso número")
        String ourNumber,

        @Schema(description = "Número do documento. Atenção: não é chave única, há duplicidades e 6% de nulos")
        String documentNumber,

        @Schema(description = "Código da situação: 1 = Em Aberto, 23 = Liquidado, 18 = Baixado", example = "1")
        Integer statusCode,

        @Schema(description = "Descrição da situação: Em Aberto, Liquidado ou Baixado", example = "Em Aberto")
        String statusDescription,

        @Schema(description = "CNPJ do beneficiário, somente dígitos")
        String payeeTaxId,

        @Schema(description = "Nome do beneficiário (busca parcial)")
        String payeeName,

        @Schema(description = "Data inicial de emissão", example = "2026-07-01")
        LocalDate issueDateFrom,

        @Schema(description = "Data final de emissão", example = "2026-08-31")
        LocalDate issueDateTo,

        @Schema(description = "Data inicial de vencimento", example = "2026-07-01")
        LocalDate dueDateFrom,

        @Schema(description = "Data final de vencimento", example = "2026-12-31")
        LocalDate dueDateTo,

        @Schema(description = "Data inicial de pagamento", example = "2026-07-01")
        LocalDate paymentDateFrom,

        @Schema(description = "Data final de pagamento", example = "2026-08-31")
        LocalDate paymentDateTo,

        @Schema(description = "Se o boleto já foi pago. true retorna apenas com data de pagamento preenchida")
        Boolean paid,

        @Schema(description = "Valor mínimo do boleto")
        BigDecimal minAmount,

        @Schema(description = "Valor máximo do boleto")
        BigDecimal maxAmount,

        @Schema(description = "Ano de emissão", example = "2026")
        Integer issueYear,

        @Schema(description = "Mês de emissão (1 a 12)", example = "8")
        @Min(1) @Max(12)
        Integer issueMonth,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: dueDate, issueDate, paymentDate, amount, paidAmount, statusCode, daysToDueDate, id. Direções: asc, desc",
                example = "[\"dueDate,asc\", \"amount,desc\"]"
        )
        List<String> sort
) {
    public SicoobBoletoPageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}
