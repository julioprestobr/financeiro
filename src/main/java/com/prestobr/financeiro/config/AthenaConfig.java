package com.prestobr.financeiro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;

// Classe de configuração responsável por criar o cliente da AWS para o Athena

@Configuration
public class AthenaConfig {

    @Value("${s3.access-key}")
    private String accessKey;

    @Value("${s3.secret-key}")
    private String secretKey;

    @Value("${s3.region}")
    private String region;

    @Bean
    public AthenaClient athenaClient() {

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return AthenaClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .build();
    }
}