package com.prestobr.financeiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record PageableRequest(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        List<String> sort
) {
    public PageableRequest {
        if (page == null) page = 0;
        if (size == null) size = 50;
    }
}