package com.prestobr.financeiro.client;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.ColumnInfo;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AthenaQueryClient {

    private static final long POLL_INTERVAL_MS = 500;
    private static final int MAX_POLL_ATTEMPTS = 120; // ~60s de timeout

    private final AthenaClient athenaClient;
    private final String database;
    private final String workgroup;
    private final String outputLocation;

    public AthenaQueryClient(AthenaClient athenaClient, String database, String workgroup, String outputLocation) {
        this.athenaClient = athenaClient;
        this.database = database;
        this.workgroup = workgroup;
        this.outputLocation = outputLocation;
    }

    public List<Map<String, String>> runQuery(String sql) {
        String queryExecutionId = startQuery(sql);
        waitForCompletion(queryExecutionId);
        return fetchResults(queryExecutionId);
    }

    private String startQuery(String sql) {
        StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .workGroup(workgroup)
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        StartQueryExecutionResponse response = athenaClient.startQueryExecution(request);
        log.info("Query Athena iniciada: {}", response.queryExecutionId());
        return response.queryExecutionId();
    }

    private void waitForCompletion(String queryExecutionId) {
        GetQueryExecutionRequest request = GetQueryExecutionRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            var status = athenaClient.getQueryExecution(request).queryExecution().status();
            QueryExecutionState state = status.state();

            if (state == QueryExecutionState.SUCCEEDED) {
                return;
            }
            if (state == QueryExecutionState.FAILED || state == QueryExecutionState.CANCELLED) {
                throw new RuntimeException("Query Athena falhou (" + state + "): " + status.stateChangeReason());
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Execução da query Athena interrompida", e);
            }
        }

        throw new RuntimeException("Timeout aguardando conclusão da query Athena: " + queryExecutionId);
    }

    private List<Map<String, String>> fetchResults(String queryExecutionId) {
        GetQueryResultsRequest request = GetQueryResultsRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        GetQueryResultsIterable resultsIterable = athenaClient.getQueryResultsPaginator(request);

        List<String> columns = null;
        List<Map<String, String>> rows = new ArrayList<>();
        boolean firstPage = true;

        for (GetQueryResultsResponse page : resultsIterable) {
            List<Row> pageRows = page.resultSet().rows();

            if (columns == null) {
                columns = page.resultSet().resultSetMetadata().columnInfo().stream()
                        .map(ColumnInfo::name)
                        .toList();
            }

            // Na primeira página, a primeira linha é o cabeçalho (nomes das colunas), não dado.
            int startIndex = firstPage ? 1 : 0;
            for (int i = startIndex; i < pageRows.size(); i++) {
                rows.add(toRowMap(columns, pageRows.get(i)));
            }
            firstPage = false;
        }

        log.debug("Query Athena {} retornou {} registros", queryExecutionId, rows.size());
        return rows;
    }

    private Map<String, String> toRowMap(List<String> columns, Row row) {
        List<Datum> data = row.data();
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            map.put(columns.get(i), i < data.size() ? data.get(i).varCharValue() : null);
        }
        return map;
    }
}