package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "AccountPayablePageRequest", description = "Parâmetros de paginação e ordenação para consultas de contas a pagar")
public record AccountPayablePageRequest(
        @Schema(description = "Status do pagamento: PENDING, PAID, ALL", example = "PENDING")
        String paymentStatus,

        @Schema(description = "Código do título")
        String titleCode,

        @Schema(description = "Código da compra")
        String purchaseCode,

        @Schema(description = "Código da empresa")
        String companyCode,

        @Schema(description = "Código do fornecedor")
        String vendorCode,

        @Schema(description = "CNPJ do fornecedor (com ou sem máscara)")
        String vendorCnpj,

        @Schema(description = "Código do providerCode")
        String providerCode,

        @Schema(description = "CNPJ do prestador (com ou sem máscara)")
        String providerCnpj,

        @Schema(description = "Número da parcela")
        String installmentNumber,

        @Schema(description = "Dias de atraso")
        String daysOverdue,

        @Schema(description = "Código do centro de custo")
        String costCenterCode,

        @Schema(description = "Código do subcentro de custo")
        String subCostCenterCode,

        @Schema(description = "Código do departamento")
        String departmentCode,

        @Schema(description = "Plano de conta")
        String accountPlan,

        @Schema(description = "Histórico")
        String description,

        @Schema(description = "Tipo de documento")
        String documentType,

        @Schema(description = "Tipo de título")
        String titleType,

        @Schema(description = "Operação")
        String operation,

        @Schema(description = "Forma de pagamento")
        String paymentMethod,

        @Schema(description = "Opção de pagamento")
        String paymentOption,

        @Schema(description = "Data inicial da emissão", example = "2026-01-01")
        @NotNull
        LocalDate emissionDateFrom,

        @Schema(description = "Data final da emissão", example = "2026-03-31")
        LocalDate emissionDateTo,

        @Schema(description = "Data inicial do vencimento", example = "2026-01-01")
        @NotNull
        LocalDate dueDateFrom,

        @Schema(description = "Data final do vencimento", example = "2026-03-31")
        LocalDate dueDateTo,

        @Schema(description = "Página (começa em 0)", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Itens por página", defaultValue = "100")
        @Min(1) @Max(100)
        Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: emissionDate, dueDate, entryDate, createdAt, updatedAt, titleValue, installmentNumber, daysOverdue. Direções: asc, desc",
                example = "[\"emissionDate,asc\", \"titleValue,desc\"]"
        )
        List<String> sort
) {
    public AccountPayablePageRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}