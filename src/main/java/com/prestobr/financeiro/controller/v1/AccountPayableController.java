package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Consulta de contas a pagar do Data Lake Silver")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;

    @GetMapping
    @Operation(summary = "Lista todas as contas a pagar da run mais recente do Data Lake Silver")
    public ResponseEntity<Page<AccountPayable>> getAll(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(accountPayableService.getLatestAccountsPayable(pageable));
    }

    @GetMapping("/title/{titleCode}")
    @Operation(summary = "Busca uma conta a pagar pelo código do título")
    public ResponseEntity<AccountPayable> getByCodigoTitulo(
            @Parameter(description = "Código do título")
            @PathVariable("titleCode") String codigoTitulo) {

        return accountPayableService.getByCodigoTitulo(codigoTitulo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vendor/{vendorCode}")
    @Operation(summary = "Busca contas a pagar pelo código do fornecedor")
    public ResponseEntity<Page<AccountPayable>> getByFornecedor(
            @Parameter(description = "Código do fornecedor")
            @PathVariable("vendorCode") String codFornecedor,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<AccountPayable> accounts = accountPayableService.getByFornecedor(codFornecedor, pageable);
        if (!accounts.hasContent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/document/{document}")
    @Operation(summary = "Busca contas a pagar pelo documento do contribuinte (CPF/CNPJ)")
    public ResponseEntity<Page<AccountPayable>> getByDocumentoContribuinte(
            @Parameter(description = "CPF ou CNPJ do contribuinte")
            @PathVariable("document") String documento,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<AccountPayable> accounts = accountPayableService.getByDocumentoContribuinte(documento, pageable);
        if (!accounts.hasContent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/pending")
    @Operation(summary = "Lista todas as contas a pagar pendentes (não pagas totalmente)")
//    public ResponseEntity<Page<AccountPayable>> getPendentes(
//            @PageableDefault(size = 50) Pageable pageable) {
//        return ResponseEntity.ok(accountPayableService.getPendentes(pageable));
//    }
    public PageResponse<AccountPayable> getPendentes(
            @PageableDefault(size = 50) Pageable pageable) {
        return accountPayableService.getPendentes(pageable);
    }
}