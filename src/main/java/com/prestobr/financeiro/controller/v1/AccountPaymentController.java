package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.AccountPaymentPageRequest;
import com.prestobr.financeiro.dto.response.AccountPaymentResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/account-payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Consulta de pagamentos (movimentação bancária) do Data Lake")
public class AccountPaymentController {

    private final AccountPaymentService accountPaymentService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): movementDate, createdAt, updatedAt, documentValue, currentBalance. Direções disponíveis: asc, desc. Exemplo: [\"movementDate,asc\",\"documentValue,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca pagamentos com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountPaymentResponse> search(@RequestBody AccountPaymentPageRequest request) {
        return accountPaymentService.search(request);
    }

    @GetMapping("/document")
    @Operation(summary = "Busca um pagamento pelo número do documento")
    public AccountPaymentResponse getByNumeroDocumento(
            @Parameter(description = "Número do documento")
            @RequestParam("numero") String numeroDocumento) {
        return accountPaymentService.getByNumeroDocumento(numeroDocumento);
    }

}
