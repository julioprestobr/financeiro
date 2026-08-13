package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.Nfe;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "Response.Nfe", description = "Dados de uma nota fiscal eletrônica consolidada")
public record NfeResponse(
        String accessKey,
        String invoiceNumber,
        String series,
        String invoiceType,

        String status,
        Boolean canceled,
        LocalDate cancellationDate,

        String issuerTaxId,
        String issuerName,
        String recipientTaxId,
        String recipientName,

        LocalDate issueDate,
        Integer issueYear,
        Integer issueMonth,
        LocalDate importDate,
        Integer daysUntilImport,

        BigDecimal totalAmount,

        LocalDateTime snapshotDatetime
) {
    public static NfeResponse from(Nfe nfe) {
        return new NfeResponse(
                nfe.getAccessKey(),
                nfe.getInvoiceNumber(),
                nfe.getSeries(),
                nfe.getInvoiceType(),
                nfe.getStatus(),
                nfe.getCanceled(),
                nfe.getCancellationDate(),
                nfe.getIssuerTaxId(),
                nfe.getIssuerName(),
                nfe.getRecipientTaxId(),
                nfe.getRecipientName(),
                nfe.getIssueDate(),
                nfe.getIssueYear(),
                nfe.getIssueMonth(),
                nfe.getImportDate(),
                nfe.getDaysUntilImport(),
                nfe.getTotalAmount(),
                nfe.getSnapshotDatetime()
        );
    }
}
