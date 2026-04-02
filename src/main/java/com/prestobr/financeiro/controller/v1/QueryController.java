package com.prestobr.financeiro.controller.v1;

import com.prestobr.financeiro.dto.request.QueryRequest;
import com.prestobr.financeiro.dto.response.QueryResponse;
import com.prestobr.financeiro.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Query")
public class QueryController {

    private final QueryService queryService;

    @PostMapping("/query")
    @Operation(summary = "Execute SQL query on dataset")
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return queryService.execute(request.getQuery(), request.getS3Path());
    }

    @DeleteMapping("/query/cache")
    @Operation(summary = "Clear DuckDB connection cache")
    public ResponseEntity<String> clearCache() {
        queryService.clearCache();
        return ResponseEntity.ok("Cache cleared successfully");
    }
}