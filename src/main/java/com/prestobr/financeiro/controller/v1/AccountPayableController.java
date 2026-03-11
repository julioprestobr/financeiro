package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.service.AccountPayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Consulta de contas a pagar do Data Lake Silver")
public class AccountPayableController {

    private final AccountPayableService accountPayableService;

    @GetMapping
    @Operation(summary = "Lista todas as contas a pagar da run mais recente do Data Lake Silver")
    public ResponseEntity<List<AccountPayable>> getAll() {
        List<AccountPayable> accounts = accountPayableService.getLatestAccountsPayable();
        return ResponseEntity.ok(accounts);
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
    public ResponseEntity<List<AccountPayable>> getByFornecedor(
            @Parameter(description = "Código do fornecedor")
            @PathVariable("vendorCode") String codFornecedor) {

        List<AccountPayable> accounts = accountPayableService.getByFornecedor(codFornecedor);

        if (accounts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/document/{document}")
    @Operation(summary = "Busca contas a pagar pelo documento do contribuinte (CPF/CNPJ)")
    public ResponseEntity<List<AccountPayable>> getByDocumentoContribuinte(
            @Parameter(description = "CPF ou CNPJ do contribuinte")
            @PathVariable("document") String documento) {

        List<AccountPayable> accounts = accountPayableService.getByDocumentoContribuinte(documento);

        if (accounts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/pending")
    @Operation(summary = "Lista todas as contas a pagar pendentes (não pagas totalmente)")
    public ResponseEntity<List<AccountPayable>> getPendentes() {
        List<AccountPayable> accounts = accountPayableService.getPendentes();
        return ResponseEntity.ok(accounts);
    }
}