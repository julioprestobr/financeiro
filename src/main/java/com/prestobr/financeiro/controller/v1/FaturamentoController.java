package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.FaturamentoPageRequest;
import com.prestobr.financeiro.dto.response.FaturamentoResponse;
import com.prestobr.financeiro.dto.response.FaturamentoSummaryResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.FaturamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/faturamento")
@RequiredArgsConstructor
@Tag(name = "Faturamento", description = "Consulta de faturamento do Data Lake")
public class FaturamentoController {

    private final FaturamentoService faturamentoService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): saleDate, amount, customerCode, saleCode, invoiceNumber. Direções disponíveis: asc, desc. Exemplo: [\"saleDate,desc\",\"amount,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca lançamentos de faturamento com filtros",
            description = "Atenção: a tabela mistura faturamento com custo, remessa, baixa e controle interno. Para obter apenas faturamento, filtre category igual a Receita. " + SORT_DESCRIPTION
    )
    public PageResponse<FaturamentoResponse> search(@RequestBody FaturamentoPageRequest request) {
        return faturamentoService.search(request);
    }

    @GetMapping("/sale")
    @Operation(summary = "Busca um lançamento pelo código da venda")
    public FaturamentoResponse getBySaleCode(
            @Parameter(description = "Código da venda")
            @RequestParam("codigo") String saleCode) {
        return faturamentoService.getBySaleCode(saleCode);
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Resumo de faturamento por ano e mês",
            description = "Retorna o total de lançamentos e a receita separadamente. A separação existe porque apenas os lançamentos com categoria Receita são faturamento de fato: somar todas as categorias resulta em mais que o dobro, por incluir custo, remessa, baixa e controle interno."
    )
    public List<FaturamentoSummaryResponse> summary(
            @Parameter(description = "Ano do lançamento. Se omitido, retorna todos os anos")
            @RequestParam(value = "ano", required = false) Integer year,
            @Parameter(description = "Mês do lançamento (1 a 12). Se omitido, retorna todos os meses")
            @RequestParam(value = "mes", required = false) Integer month) {
        return faturamentoService.summary(year, month);
    }

}
