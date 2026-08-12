package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.AccountBalanceHistoryPageRequest;
import com.prestobr.financeiro.dto.response.AccountBalanceHistoryResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountBalanceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/account-balance-history")
@RequiredArgsConstructor
@Tag(name = "Histórico de Saldo", description = "Consulta de histórico diário de saldo de contas bancárias do Data Lake")
public class AccountBalanceHistoryController {

    private final AccountBalanceHistoryService accountBalanceHistoryService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): referenceDate, accumulatedBalance, dailyCredits, dailyDebits, netDailyMovement. Direções disponíveis: asc, desc. Exemplo: [\"referenceDate,asc\",\"accumulatedBalance,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca histórico de saldo com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<AccountBalanceHistoryResponse> search(@RequestBody AccountBalanceHistoryPageRequest request) {
        return accountBalanceHistoryService.search(request);
    }

    @GetMapping("/account/{accountCode}")
    @Operation(summary = "Busca o histórico de saldo de uma conta")
    public List<AccountBalanceHistoryResponse> getByAccountCode(
            @Parameter(description = "Código da conta bancária")
            @PathVariable("accountCode") String accountCode) {
        return accountBalanceHistoryService.getByAccountCode(accountCode);
    }

}
