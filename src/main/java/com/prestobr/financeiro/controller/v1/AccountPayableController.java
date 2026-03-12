package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Consulta de contas a pagar do Data Lake Silver")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;
    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): dataEmissao, dataVencimento, dataEntrada, dataCadastro, dataAlteracao, valorTitulo, numeroParcela, diasAtraso. Direções disponíveis: asc, desc. Exemplo: [\"dataVencimento,asc\",\"valorTitulo,asc\",\"diasAtraso,desc\"]";

    @GetMapping
    @Operation(
            summary = "Lista todas as contas a pagar da run mais recente do Data Lake Silver",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> getAll(
            @PageableDefault(size = 50) Pageable pageable) {
        return accountPayableService.getLatestAccountsPayable(pageable);
    }

    @GetMapping("/title/{titleCode}")
    @Operation(summary = "Busca uma conta a pagar pelo código do título")
    public AccountPayable getByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {

        return accountPayableService.getByCodigoTitulo(codigoTitulo);
    }

    @GetMapping("/vendor/{vendorCode}")
    @Operation(
            summary = "Busca contas a pagar pelo código do fornecedor",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> getByFornecedor(
            @Parameter(description = "Código do fornecedor")
            @PathVariable("vendorCode") String codFornecedor,
            @PageableDefault(size = 50) Pageable pageable) {

        return accountPayableService.getByFornecedor(codFornecedor, pageable);
    }

    @GetMapping("/pending")
    @Operation(
            summary = "Lista todas as contas a pagar pendentes (não pagas totalmente)",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> getPendentes(
            @PageableDefault(size = 50) Pageable pageable) {
        return accountPayableService.getPendentes(pageable);
    }
}