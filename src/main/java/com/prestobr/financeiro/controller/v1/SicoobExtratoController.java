package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.SicoobExtratoPageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.SicoobExtratoResponse;
import com.prestobr.financeiro.dto.response.SicoobExtratoSummaryResponse;
import com.prestobr.financeiro.service.SicoobExtratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/sicoob-extrato")
@RequiredArgsConstructor
@Tag(name = "Sicoob - Extrato", description = "Consulta de extrato de conta Sicoob do Data Lake")
public class SicoobExtratoController {

    private final SicoobExtratoService sicoobExtratoService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): movementDate, amount, entryType, description, id. Direções disponíveis: asc, desc. Exemplo: [\"movementDate,desc\",\"amount,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca lançamentos do extrato com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<SicoobExtratoResponse> search(@RequestBody SicoobExtratoPageRequest request) {
        return sicoobExtratoService.search(request);
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Resumo do extrato por ano e mês",
            description = "Retorna contagens, totais de crédito e débito, movimento líquido, saldo de abertura e saldo de fechamento calculado. O fechamento é calculado como abertura mais movimento líquido, porque a coluna saldo_atual guarda o saldo final do extrato replicado em todas as linhas."
    )
    public List<SicoobExtratoSummaryResponse> summary(
            @Parameter(description = "Ano do movimento. Se omitido, retorna todos os anos")
            @RequestParam(value = "ano", required = false) Integer year,
            @Parameter(description = "Mês do movimento (1 a 12). Se omitido, retorna todos os meses")
            @RequestParam(value = "mes", required = false) Integer month) {
        return sicoobExtratoService.summary(year, month);
    }

}
