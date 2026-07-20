package com.prestobr.financeiro.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// Classe de configuração responsável por criar a conexão com o Postgres do Data Lake

@Configuration
public class DataLakePostgresConfig {

    @Value("${datalake.postgres.host}")
    private String host;

    @Value("${datalake.postgres.port}")
    private String port;

    @Value("${datalake.postgres.database}")
    private String database;

    @Value("${datalake.postgres.user}")
    private String user;

    @Value("${datalake.postgres.password}")
    private String password;

    @Bean
    public DataSource dataLakeDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(5);
        dataSource.setPoolName("datalake-postgres-pool");
        return dataSource;
    }

    @Bean
    public JdbcTemplate dataLakeJdbcTemplate(DataSource dataLakeDataSource) {
        return new JdbcTemplate(dataLakeDataSource);
    }
}