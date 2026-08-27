package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.SicoobBoleto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "Response.SicoobBoleto", description = "Dados de um boleto Sicoob")
public record SicoobBoletoResponse(
        Integer id,
        String barcode,
        String ourNumber,
        String documentNumber,

        Integer statusCode,
        String statusDescription,

        String payeeName,
        String payeeTaxId,
        String payerName,
        String payerTaxId,

        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate paymentDate,
        LocalDate paymentDeadline,

        @Schema(description = "Dias até o vencimento calculado no momento da carga. Relativo a snapshotDatetime, não à data atual: defasa a cada dia sem nova carga")
        Integer daysToDueDate,

        Integer issueYear,
        Integer issueMonth,

        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal discountAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,

        LocalDateTime snapshotDatetime
) {
    public static SicoobBoletoResponse from(SicoobBoleto b) {
        return new SicoobBoletoResponse(
                b.getId(),
                b.getBarcode(),
                b.getOurNumber(),
                b.getDocumentNumber(),
                b.getStatusCode(),
                b.getStatusDescription(),
                b.getPayeeName(),
                b.getPayeeTaxId(),
                b.getPayerName(),
                b.getPayerTaxId(),
                b.getIssueDate(),
                b.getDueDate(),
                b.getPaymentDate(),
                b.getPaymentDeadline(),
                b.getDaysToDueDate(),
                b.getIssueYear(),
                b.getIssueMonth(),
                b.getAmount(),
                b.getPaidAmount(),
                b.getDiscountAmount(),
                b.getInterestAmount(),
                b.getFineAmount(),
                b.getSnapshotDatetime()
        );
    }
}
