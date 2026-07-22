package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.AccountReceiptPageRequest;
import com.prestobr.financeiro.dto.response.AccountReceiptResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/account-receipts")
@RequiredArgsConstructor
@Tag(name = "Recebimentos", description = "Consulta de recebimentos (movimentação bancária) do Data Lake")
public class AccountReceiptController {

    private final AccountReceiptService accountReceiptService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): movementDate, createdAt, updatedAt, documentValue, currentBalance. Direções disponíveis: asc, desc. Exemplo: [\"movementDate,asc\",\"documentValue,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca recebimentos com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountReceiptResponse> search(@RequestBody AccountReceiptPageRequest request) {
        return accountReceiptService.search(request);
    }

    @GetMapping("/document")
    @Operation(summary = "Busca um recebimento pelo número do documento")
    public AccountReceiptResponse getByNumeroDocumento(
            @Parameter(description = "Número do documento")
            @RequestParam("numero") String numeroDocumento) {
        return accountReceiptService.getByNumeroDocumento(numeroDocumento);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Limpa o cache de recebimentos")
    public String clearCache() {
        accountReceiptService.clearCache();
        return "Cache limpo";
    }

}