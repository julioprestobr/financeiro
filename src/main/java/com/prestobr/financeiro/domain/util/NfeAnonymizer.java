package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.Nfe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

public class NfeAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static Nfe anonymize(Nfe original) {
        if (original == null) {
            return null;
        }

        return Nfe.builder()
                // Identificação - mascara o CNPJ embutido na chave de acesso
                .accessKey(maskAccessKey(original.getAccessKey()))
                .invoiceNumber(original.getInvoiceNumber())
                .series(original.getSeries())
                .invoiceType(original.getInvoiceType())

                // Situação - mantém
                .status(original.getStatus())
                .canceled(original.getCanceled())
                .cancellationDate(original.getCancellationDate())

                // Partes - mascara documento e anonimiza nome
                .issuerTaxId(maskTaxId(original.getIssuerTaxId()))
                .issuerName(anonymizeText("Emitente"))
                .recipientTaxId(maskTaxId(original.getRecipientTaxId()))
                .recipientName(anonymizeText("Destinatário"))

                // Datas - mantém
                .issueDate(original.getIssueDate())
                .issueYear(original.getIssueYear())
                .issueMonth(original.getIssueMonth())
                .importDate(original.getImportDate())
                .daysUntilImport(original.getDaysUntilImport())

                // Valor - randomiza
                .totalAmount(randomizeValue(original.getTotalAmount()))

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
                .build();
    }

    // A chave de acesso de 44 dígitos carrega o CNPJ do emitente nas posições
    // 6 a 19: cUF(2) + AAMM(4) + CNPJ(14) + mod(2) + serie(3) + nNF(9) +
    // tpEmis(1) + cNF(8) + cDV(1). Sem mascarar esse trecho, anonimizar
    // issuerTaxId não serve para nada, porque o documento continua legível na
    // chave ao lado.
    private static final int ACCESS_KEY_LENGTH = 44;
    private static final int TAX_ID_OFFSET = 6;
    private static final int TAX_ID_LENGTH = 14;

    private static String maskAccessKey(String accessKey) {
        if (accessKey == null || accessKey.length() != ACCESS_KEY_LENGTH) {
            return accessKey;
        }
        return accessKey.substring(0, TAX_ID_OFFSET)
                + "*".repeat(TAX_ID_LENGTH)
                + accessKey.substring(TAX_ID_OFFSET + TAX_ID_LENGTH);
    }

    /**
     * Mascara CNPJ ou CPF preservando o comprimento e os 4 últimos dígitos.
     * O comprimento importa: a coluna guarda CNPJ (14) e também CPF (11) de
     * pessoa física, e essa distinção precisa sobreviver à anonimização.
     */
    private static String maskTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return taxId;
        }
        if (taxId.length() <= 4) {
            return "*".repeat(taxId.length());
        }
        return "*".repeat(taxId.length() - 4) + taxId.substring(taxId.length() - 4);
    }

    private static String anonymizeText(String prefix) {
        return prefix + " Anonimizado #" + random.nextInt(10000);
    }

    public static BigDecimal randomizeValue(BigDecimal original) {
        if (original == null) {
            return null;
        }
        double factor = 0.5 + random.nextDouble();
        return original.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
