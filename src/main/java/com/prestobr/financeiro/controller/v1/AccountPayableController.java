package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Consulta de contas a pagar do Data Lake Silver")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;
    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): dataEmissao, dataVencimento, dataEntrada, dataCadastro, dataAlteracao, valorTitulo, numeroParcela, diasAtraso. Direções disponíveis: asc, desc. Exemplo: [\"dataVencimento,asc\",\"valorTitulo,asc\",\"diasAtraso,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca contas a pagar com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> search(@RequestBody AccountPayablePageRequest request) {
        return accountPayableService.search(request);
    }

    @GetMapping("/title/{titleCode}")
    @Operation(summary = "Busca uma conta a pagar pelo código do título")
    public AccountPayable getByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {
        return accountPayableService.getByCodigoTitulo(codigoTitulo);
    }
}