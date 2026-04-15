package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
import com.prestobr.financeiro.dto.request.QueryRequest;
import com.prestobr.financeiro.dto.response.AccountPayableEnrichedResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.QueryResponse;
import com.prestobr.financeiro.service.AccountPayableEnrichedService;
import com.prestobr.financeiro.service.AccountPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Consulta de contas a pagar do Data Lake")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;
    private final AccountPayableEnrichedService accountPayableEnrichedService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): dataEmissao, dataVencimento, dataEntrada, dataCadastro, dataAlteracao, valorTitulo, numeroParcela, diasAtraso. Direções disponíveis: asc, desc. Exemplo: [\"dataVencimento,asc\",\"valorTitulo,asc\",\"diasAtraso,desc\"]";

    @PostMapping("/enriched/search")
    @Operation(
            summary = "Busca contas a pagar enriquecidas com filtros (Gold)",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayableEnrichedResponse> enrichedSearch(@RequestBody AccountPayablePageRequest request) {
        return accountPayableEnrichedService.search(request);
    }

    @GetMapping("/enriched/title/{titleCode}")
    @Operation(summary = "Busca uma conta a pagar enriquecida pelo código do título (Gold)")
    public AccountPayableEnrichedResponse getEnrichedByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {
        return accountPayableEnrichedService.getByCodigoTitulo(codigoTitulo);
    }

    @DeleteMapping("/enriched/cache")
    @Operation(summary = "Limpa o cache de contas a pagar enriquecidas (Gold)")
    public String clearEnrichedCache() {
        accountPayableEnrichedService.clearCache();
        return "Cache enriquecido limpo";
    }

    @PostMapping("/query")
    @Operation(summary = "Executa query dinâmica no Data Lake")
    public QueryResponse query(@RequestBody QueryRequest request) {
        return accountPayableEnrichedService.executeQuery(request.getQuery());
    }
}