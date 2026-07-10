package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.AccountReceivablePageRequest;
import com.prestobr.financeiro.dto.response.AccountReceivableResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts-receivable")
@RequiredArgsConstructor
@Tag(name = "Contas a receber", description = "Consulta de contas a receber do Data Lake")
public class AccountReceivableController {

    private final AccountReceivableService accountReceivableService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): emissionDate, dueDate, originalDueDate, createdAt, updatedAt, titleValue, daysOverdue. Direções disponíveis: asc, desc. Exemplo: [\"dueDate,asc\",\"titleValue,asc\",\"daysOverdue,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca contas a receber com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountReceivableResponse> search(@RequestBody AccountReceivablePageRequest request) {
        return accountReceivableService.search(request);
    }

    @GetMapping("/title/{titleCode}")
    @Operation(summary = "Busca uma conta a receber pelo código do título")
    public AccountReceivableResponse getByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {
        return accountReceivableService.getByCodigoTitulo(codigoTitulo);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Limpa o cache de contas a receber")
    public String clearCache() {
        accountReceivableService.clearCache();
        return "Cache limpo";
    }

}