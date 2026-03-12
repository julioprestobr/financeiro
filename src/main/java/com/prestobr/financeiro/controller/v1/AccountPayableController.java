package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.dto.request.PageableRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Consulta de contas a pagar do Data Lake Silver")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;
    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): dataEmissao, dataVencimento, dataEntrada, dataCadastro, dataAlteracao, valorTitulo, numeroParcela, diasAtraso. Direções disponíveis: asc, desc. Exemplo: [\"dataVencimento,asc\",\"valorTitulo,asc\",\"diasAtraso,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Lista todas as contas a pagar da run mais recente do Data Lake Silver",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> getAll(@RequestBody PageableRequest request) {
        return accountPayableService.getLatestAccountsPayable(buildPageable(request));
    }

    @GetMapping("/title/{titleCode}")
    @Operation(summary = "Busca uma conta a pagar pelo código do título")
    public AccountPayable getByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {
        return accountPayableService.getByCodigoTitulo(codigoTitulo);
    }

    @PostMapping("/vendor/{vendorCode}/search")
    @Operation(
            summary = "Busca contas a pagar pelo código do fornecedor",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> getByFornecedor(
            @Parameter(description = "Código do fornecedor")
            @PathVariable("vendorCode") String codFornecedor,
            @RequestBody PageableRequest request) {
        return accountPayableService.getByFornecedor(codFornecedor, buildPageable(request));
    }

    @PostMapping("/pending/search")
    @Operation(
            summary = "Lista todas as contas a pagar pendentes (não pagas totalmente)",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPayable> getPendentes(@RequestBody PageableRequest request) {
        return accountPayableService.getPendentes(buildPageable(request));
    }

    private Pageable buildPageable(PageableRequest request) {
        if (request.sort() == null || request.sort().isEmpty()) {
            return PageRequest.of(request.page(), request.size());
        }

        List<Sort.Order> orders = request.sort().stream()
                .map(s -> {
                    String[] parts = s.split(",");
                    String field = parts[0];
                    Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;
                    return new Sort.Order(direction, field);
                })
                .toList();

        return PageRequest.of(request.page(), request.size(), Sort.by(orders));
    }
}