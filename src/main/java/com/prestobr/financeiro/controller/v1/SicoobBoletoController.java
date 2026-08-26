package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.SicoobBoletoPageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.SicoobBoletoResponse;
import com.prestobr.financeiro.service.SicoobBoletoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/sicoob-boletos")
@RequiredArgsConstructor
@Tag(name = "Sicoob - Boletos", description = "Consulta de boletos Sicoob do Data Lake")
public class SicoobBoletoController {

    private final SicoobBoletoService sicoobBoletoService;

    private static final String SORT_DESCRIPTION = "Campos disponíveis para ordenação (sort): dueDate, issueDate, paymentDate, amount, paidAmount, statusCode, daysToDueDate, id. Direções disponíveis: asc, desc. Exemplo: [\"dueDate,asc\",\"amount,desc\"]";

    @PostMapping("/search")
    @Operation(
            summary = "Busca boletos Sicoob com filtros",
            description = SORT_DESCRIPTION
    )
    public PageResponse<SicoobBoletoResponse> search(@RequestBody SicoobBoletoPageRequest request) {
        return sicoobBoletoService.search(request);
    }

    @GetMapping("/barcode")
    @Operation(summary = "Busca um boleto pelo código de barras")
    public SicoobBoletoResponse getByBarcode(
            @Parameter(description = "Código de barras do boleto (44 dígitos)")
            @RequestParam("codigo") String barcode) {
        return sicoobBoletoService.getByBarcode(barcode);
    }

}
