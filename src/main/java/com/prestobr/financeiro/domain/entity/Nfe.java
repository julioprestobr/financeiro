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
public class Nfe {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private String accessKey;
    private String invoiceNumber;
    private String series;
    private String invoiceType;

    // =========================================================================
    // SITUAÇÃO
    // =========================================================================
    private String status;
    private Boolean canceled;
    private LocalDate cancellationDate;

    // =========================================================================
    // PARTES
    // =========================================================================
    private String issuerTaxId;
    private String issuerName;
    private String recipientTaxId;
    private String recipientName;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDate issueDate;
    private Integer issueYear;
    private Integer issueMonth;
    private LocalDate importDate;
    private Integer daysUntilImport;

    // =========================================================================
    // VALOR
    // =========================================================================
    private BigDecimal totalAmount;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;

    // As colunas url_download e evento_cancelamento_url_download não são
    // mapeadas: são URLs pré-assinadas do S3 com validade de 1h a partir da
    // carga do Data Lake, ou seja, já chegam expiradas na API.
}
