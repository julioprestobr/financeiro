package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.AccountBalanceHistoryPageRequest;
import com.prestobr.financeiro.dto.response.AccountBalanceHistoryResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.AccountBalanceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    @GetMapping("/account/{accountCode}/date/{referenceDate}")
    @Operation(summary = "Busca o saldo de uma conta em uma data específica")
    public AccountBalanceHistoryResponse getByAccountAndDate(
            @Parameter(description = "Código da conta bancária")
            @PathVariable("accountCode") String accountCode,
            @Parameter(description = "Data de referência", example = "2026-01-01")
            @PathVariable("referenceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        return accountBalanceHistoryService.getByAccountAndDate(accountCode, referenceDate);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Limpa o cache de histórico de saldo")
    public String clearCache() {
        accountBalanceHistoryService.clearCache();
        return "Cache limpo";
    }

}
