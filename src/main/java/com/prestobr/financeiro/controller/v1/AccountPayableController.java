package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
import com.prestobr.financeiro.dto.request.QueryRequest;
import com.prestobr.financeiro.dto.response.AccountPayableResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.QueryResponse;
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

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): dataEmissao, dataVencimento, dataEntrada, dataCadastro, dataAlteracao, valorTitulo, numeroParcela, diasAtraso. Direções disponíveis: asc, desc. Exemplo: [\"dataVencimento,asc\",\"valorTitulo,asc\",\"diasAtraso,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca contas a pagar com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayableResponse> search(@RequestBody AccountPayablePageRequest request) {
        return accountPayableService.search(request);
    }

    @GetMapping("/title/{titleCode}")
    @Operation(summary = "Busca uma conta a pagar pelo código do título")
    public AccountPayableResponse getByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {
        return accountPayableService.getByCodigoTitulo(codigoTitulo);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Limpa o cache de contas a pagar")
    public String clearCache() {
        accountPayableService.clearCache();
        return "Cache limpo";
    }

    @PostMapping("/query")
    @Operation(summary = "Executa query dinâmica no Data Lake")
    public QueryResponse query(@RequestBody QueryRequest request) {
        return accountPayableService.executeQuery(request.getQuery());
    }
}