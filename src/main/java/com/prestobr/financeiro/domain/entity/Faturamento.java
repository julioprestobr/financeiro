package com.prestobr.financeiro.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Faturamento {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private String saleCode;
    private String invoiceNumber;

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String companyCode;
    private String companyName;

    // =========================================================================
    // CLIENTE
    // =========================================================================
    private Integer customerCode;
    private String customerName;
    private String economicGroup;

    // =========================================================================
    // CLASSIFICAÇÃO
    // =========================================================================
    private String operationCode;
    private String type;
    private String category;
    private String subcategory;

    // =========================================================================
    // LANÇAMENTO
    // =========================================================================
    private LocalDateTime saleDate;
    private BigDecimal amount;

    // Observacoes sobre a tabela faturamento_base:
    //
    // Nem toda linha e faturamento. Apenas 29.645 das 116.505 linhas tem
    // categoria Receita; o restante e custo, remessa, baixa e controle
    // interno. Somar amount sem filtrar categoria da R$ 103 milhoes, contra
    // R$ 40,6 milhoes de receita real. O endpoint de summary separa os dois.
    //
    // A tabela nao tem snapshot_datetime, diferente de todos os outros
    // dominios, entao nao ha como saber por aqui quando foi a ultima carga.
    //
    // saleDate e LocalDateTime porque data tem hora real em 116.484 das
    // 116.505 linhas, ao contrario das demais tabelas do data lake.
}
