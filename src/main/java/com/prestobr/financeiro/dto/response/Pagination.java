package com.prestobr.financeiro.dto.response;

public record Pagination(
        int page,
        int size,
        long total,
        int pages
) {}