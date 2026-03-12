package com.prestobr.financeiro.dto.response;

import java.util.List;

public record PageResponse<T>(
        Pagination pagination,
        List<T> data
) {}