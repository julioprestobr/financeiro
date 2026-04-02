package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.DataLakeClient;
import com.prestobr.financeiro.dto.response.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final DataLakeClient dataLakeClient;

    @Value("${s3.endpoint-url}")
    private String endpointUrl;

    @Value("${s3.access-key}")
    private String accessKey;

    @Value("${s3.secret-key}")
    private String secretKey;

    @Value("${s3.region}")
    private String region;

    @Value("${s3.bucket-name}")
    private String bucketName;

    public QueryResponse execute(String dataset, String query) {
        long startTime = System.currentTimeMillis();

        List<String> parquetKeys = getParquetKeysForDataset(dataset);

        if (parquetKeys.isEmpty()) {
            throw new RuntimeException("No parquet files found for dataset: " + dataset);
        }

        // Monta lista de paths S3
        String s3Paths = parquetKeys.stream()
                .map(key -> "'s3://" + bucketName + "/" + key + "'")
                .collect(Collectors.joining(", "));

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            Statement stmt = conn.createStatement();

            // Configura acesso ao S3
            stmt.execute("INSTALL httpfs;");
            stmt.execute("LOAD httpfs;");
            stmt.execute("SET s3_region='" + region + "';");
            stmt.execute("SET s3_access_key_id='" + accessKey + "';");
            stmt.execute("SET s3_secret_access_key='" + secretKey + "';");
            stmt.execute("SET s3_endpoint='" + endpointUrl.replace("https://", "").replace("http://", "") + "';");

            // Cria view com os arquivos específicos da run mais recente
            stmt.execute("CREATE VIEW dados AS SELECT * FROM read_parquet([" + s3Paths + "], union_by_name=true);");

            // Substitui o nome da tabela na query pelo view
            String finalQuery = query.replaceAll("(?i)FROM\\s+\\w+", "FROM dados");

            log.info("Executando query: {}", finalQuery);

            ResultSet rs = stmt.executeQuery(finalQuery);

            List<String> columns = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i));
            }

            List<Map<String, Object>> data = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String col : columns) {
                    row.put(col, rs.getObject(col));
                }
                data.add(row);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            return QueryResponse.builder()
                    .columns(columns)
                    .data(data)
                    .totalRecords(data.size())
                    .executedQuery(query)
                    .executionTimeMs(executionTime)
                    .build();

        } catch (SQLException e) {
            throw new RuntimeException("Error executing query: " + e.getMessage(), e);
        }
    }

    private List<String> getParquetKeysForDataset(String dataset) {
        return switch (dataset) {
            case "accounts-payable_datalake_gold" -> dataLakeClient.findLatestRunEnrichedParquetKeys();
            case "accounts-payable_datalake_silver" -> dataLakeClient.findLatestRunParquetKeys();
            default -> throw new RuntimeException("Dataset not supported: " + dataset);
        };
    }
}