package com.prestobr.financeiro.config;

import com.prestobr.financeiro.client.DataLakeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ClientConfig {

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${datalake.gold-account-payable-base-prefix}")
    private String goldAccountPayableBasePrefix;

    // ==================== DATALAKE CLIENT ====================

    @Bean
    public DataLakeClient dataLakeClient(S3Client s3Client) {

        return new DataLakeClient(
                s3Client,
                bucketName,
                goldAccountPayableBasePrefix
        );
    }

}