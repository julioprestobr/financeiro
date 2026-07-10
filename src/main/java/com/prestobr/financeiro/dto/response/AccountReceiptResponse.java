package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.AccountReceipt;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "Response.AccountReceipt", description = "Dados de um AccountReceipt")
public record AccountReceiptResponse(
        String documentNumber,

        String accountCode,
        String accountName,
        String bankCode,

        String companyCode,
        String companyName,

        String type,
        Boolean isSettled,

        LocalDateTime movementDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        BigDecimal documentValue,
        BigDecimal currentBalance,
        BigDecimal settledBalance,

        String sale1,
        String sale2,
        String sale3,
        String sale4,
        String sale5,

        String description,
        String destination,
        String notes,

        String createdBy,
        String updatedBy,

        LocalDateTime snapshotDatetime
) {
    public static AccountReceiptResponse from(AccountReceipt ar) {
        return new AccountReceiptResponse(
                ar.getDocumentNumber(),
                ar.getAccountCode(),
                ar.getAccountName(),
                ar.getBankCode(),
                ar.getCompanyCode(),
                ar.getCompanyName(),
                ar.getType(),
                ar.getIsSettled(),
                ar.getMovementDate(),
                ar.getCreatedAt(),
                ar.getUpdatedAt(),
                ar.getDocumentValue(),
                ar.getCurrentBalance(),
                ar.getSettledBalance(),
                ar.getSale1(),
                ar.getSale2(),
                ar.getSale3(),
                ar.getSale4(),
                ar.getSale5(),
                ar.getDescription(),
                ar.getDestination(),
                ar.getNotes(),
                ar.getCreatedBy(),
                ar.getUpdatedBy(),
                ar.getSnapshotDatetime()
        );
    }
}