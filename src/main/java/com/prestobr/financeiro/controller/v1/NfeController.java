package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.NfePageRequest;
import com.prestobr.financeiro.dto.response.NfeResponse;
import com.prestobr.financeiro.dto.response.NfeSummaryResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.service.NfeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/nfe")
@RequiredArgsConstructor
@Tag(name = "Notas Fiscais", description = "Consulta de notas fiscais eletrônicas consolidadas do Data Lake")
public class NfeController {

    private final NfeService nfeService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): issueDate, totalAmount, invoiceNumber, importDate, issueYear, daysUntilImport, accessKey. Direções disponíveis: asc, desc. Exemplo: [\"issueDate,desc\",\"totalAmount,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca notas fiscais com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<NfeResponse> search(@RequestBody NfePageRequest request) {
        return nfeService.search(request);
    }

    @GetMapping("/access-key")
    @Operation(summary = "Busca uma nota fiscal pela chave de acesso")
    public NfeResponse getByAccessKey(
            @Parameter(description = "Chave de acesso da NFe (44 dígitos)")
            @RequestParam("chave") String accessKey) {
        return nfeService.getByAccessKey(accessKey);
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo de notas fiscais agrupado por ano e mês de emissão")
    public List<NfeSummaryResponse> summary(
            @Parameter(description = "Ano de emissão. Se omitido, retorna todos os anos")
            @RequestParam(value = "ano", required = false) Integer year) {
        return nfeService.summary(year);
    }

}
