package com.prestobr.financeiro.service;

import com.prestobr.financeiro.dto.response.QueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class QueryService {

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

    public QueryResponse execute(String query, String s3Path) {
        long startTime = System.currentTimeMillis();

        String fullPath = "s3://" + bucketName + "/" + s3Path + "/**/*.parquet";

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            Statement stmt = conn.createStatement();

            // Configura acesso ao S3
            stmt.execute("INSTALL httpfs;");
            stmt.execute("LOAD httpfs;");
            stmt.execute("SET s3_region='" + region + "';");
            stmt.execute("SET s3_access_key_id='" + accessKey + "';");
            stmt.execute("SET s3_secret_access_key='" + secretKey + "';");
            stmt.execute("SET s3_endpoint='" + endpointUrl.replace("https://", "").replace("http://", "") + "';");

            // Cria view temporária apontando pro Parquet
            stmt.execute("CREATE VIEW dados AS SELECT * FROM read_parquet('" + fullPath + "', union_by_name=true);");

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
}