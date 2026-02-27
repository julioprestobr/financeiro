package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/accounts_payable")
@Tag(name = "Contas a Pagar")
@RequiredArgsConstructor
public class AccountPayableController {

    @Operation(summary = "Lista todas as contas a pagar do DataClassic")
    @GetMapping("/dataclassic")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccountPayable> listAll() {
        return List.of(
                AccountPayable.builder()
                        .id(1L)
                        .description("Invoice 1234 - Supplier ABC")
                        .amount(new BigDecimal("1500.75"))
                        .build(),

                AccountPayable.builder()
                        .id(2L)
                        .description("IT Services - January")
                        .amount(new BigDecimal("3200.00"))
                        .build(),

                AccountPayable.builder()
                        .id(3L)
                        .description("Office Supplies")
                        .amount(new BigDecimal("450.30"))
                        .build()
        );
    }
}
