package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

@Schema(name = "AccountPayablePageRequest", description = "Parâmetros de paginação e ordenação para consultas de contas a pagar")
public record PageableRequest(
        @Schema(description = "Número da página (começa em 0)", example = "0")
        @Min(0) Integer page,

        @Schema(description = "Quantidade de itens por página", example = "50")
        @Min(1) @Max(100) Integer size,

        @Schema(
                description = "Campos de ordenação. Formato: campo,direção. Campos: dataEmissao, dataVencimento, dataEntrada, dataCadastro, dataAlteracao, valorTitulo, numeroParcela, diasAtraso. Direções: asc, desc",
                example = "[\"dataEmissao,asc\", \"valorTitulo,desc\"]"
        )
        List<String> sort
) {
    public PageableRequest {
        if (page == null) page = 0;
        if (size == null) size = 100;
    }
}