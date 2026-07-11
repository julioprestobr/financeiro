package com.prestobr.financeiro.dto.response;

import com.prestobr.financeiro.domain.entity.AccountPayment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "Response.AccountPayment", description = "Dados de um AccountPayment")
public record AccountPaymentResponse(
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
    public static AccountPaymentResponse from(AccountPayment ap) {
        return new AccountPaymentResponse(
                ap.getDocumentNumber(),
                ap.getAccountCode(),
                ap.getAccountName(),
                ap.getBankCode(),
                ap.getCompanyCode(),
                ap.getCompanyName(),
                ap.getType(),
                ap.getIsSettled(),
                ap.getMovementDate(),
                ap.getCreatedAt(),
                ap.getUpdatedAt(),
                ap.getDocumentValue(),
                ap.getCurrentBalance(),
                ap.getSettledBalance(),
                ap.getSale1(),
                ap.getSale2(),
                ap.getSale3(),
                ap.getSale4(),
                ap.getSale5(),
                ap.getDescription(),
                ap.getDestination(),
                ap.getNotes(),
                ap.getCreatedBy(),
                ap.getUpdatedBy(),
                ap.getSnapshotDatetime()
        );
    }
}
