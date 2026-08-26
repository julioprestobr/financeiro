package com.prestobr.financeiro.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SicoobBoleto {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private Integer id;
    private String barcode;
    private String ourNumber;
    private String documentNumber;

    // =========================================================================
    // SITUAÇÃO
    // =========================================================================
    private Integer statusCode;
    private String statusDescription;

    // =========================================================================
    // PARTES
    // =========================================================================
    private String payeeName;
    private String payeeTaxId;
    private String payerName;
    private String payerTaxId;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private LocalDate paymentDeadline;
    private Integer daysToDueDate;
    private Integer issueYear;
    private Integer issueMonth;

    // =========================================================================
    // VALORES
    // =========================================================================
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;
    private BigDecimal interestAmount;
    private BigDecimal fineAmount;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;

    // Colunas nao mapeadas e o motivo:
    //
    // esta_liquidado / esta_baixado: false em 100% das linhas, inclusive nas
    //   197 com situacao Liquidado e nas 33 com Baixado. Nao sao confiaveis.
    // esta_aberto: unica das tres flags correta, mas derivavel de statusCode
    //   e parte de um trio quebrado. Filtre por statusCode, que tem indice.
    // situacao_consultada / situacao_descricao: codificacao paralela com
    //   correlacao 1:1 com situacao/descricao_situacao (1->1, 3->23, 4->18),
    //   mesmos rotulos a menos de caixa. Redundante.
    // data_inicial_consulta / data_final_consulta: janela da extracao, nao
    //   dado de negocio.
    // created_at / updated_at: auditoria de ETL, como nos demais dominios.
}
