package com.prestobr.financeiro.domain.util;

import com.prestobr.financeiro.domain.entity.AccountPayable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;

public class AccountPayableAnonymizer {

    private static final SecureRandom random = new SecureRandom();

    public static AccountPayable anonymize(AccountPayable original) {
        if (original == null) {
            return null;
        }

        return AccountPayable.builder()
                // Identificação - gera novos códigos
                .codigoTitulo(generateCode("TIT"))
                .codigoCompra(generateCode("CMP"))

                // Vínculos - mantém estrutura mas anonimiza
                .codEmpresa(original.getCodEmpresa())
                .codFornecedor(generateCode("FOR"))
                .codCentroCusto(original.getCodCentroCusto())
                .codSubcentroCusto(original.getCodSubcentroCusto())
                .codSetor(original.getCodSetor())
                .planoConta(original.getPlanoConta())
                .contrato(generateCode("CTR"))
                .prestador(anonymizeText("Prestador"))

                // Datas - mantém as originais
                .dataEmissao(original.getDataEmissao())
                .dataVencimento(original.getDataVencimento())
                .dataEntrada(original.getDataEntrada())
                .dataCadastro(original.getDataCadastro())
                .dataAlteracao(original.getDataAlteracao())

                // Texto / Histórico - anonimiza completamente
                .historico(anonymizeText("Histórico"))
                .observacao(anonymizeText("Observação"))

                // Classificação - mantém
                .tipoDocumento(original.getTipoDocumento())
                .tipoTitulo(original.getTipoTitulo())
                .operacao(original.getOperacao())
                .formaPagamento(original.getFormaPagamento())
                .opcaoPagamento(original.getOpcaoPagamento())

                // Status - mantém
                .situacaoTitulo(original.getSituacaoTitulo())
                .statusPagamento(original.getStatusPagamento())
                .isProvisao(original.getIsProvisao())

                // Valores - randomiza
                .valorTitulo(randomizeValue(original.getValorTitulo()))
                .valorPago(randomizeValue(original.getValorPago()))
                .valorSaldo(randomizeValue(original.getValorSaldo()))
                .valorBruto(randomizeValue(original.getValorBruto()))
                .valorDesconto(randomizeValue(original.getValorDesconto()))
                .valorAcrescimo(randomizeValue(original.getValorAcrescimo()))
                .valorMovimento(randomizeValue(original.getValorMovimento()))
                .valorOutras(randomizeValue(original.getValorOutras()))
                .atualizacaoMonetaria(randomizeValue(original.getAtualizacaoMonetaria()))

                // Parcela / Competência - mantém
                .numeroParcela(original.getNumeroParcela())
                .mesCompetencia(original.getMesCompetencia())
                .periodo(original.getPeriodo())
                .periodoApuracao(original.getPeriodoApuracao())
                .periodoReferencia(original.getPeriodoReferencia())
                .anoCalculo(original.getAnoCalculo())
                .diasAtraso(original.getDiasAtraso())

                // Fiscal - anonimiza dados sensíveis
                .documentoContribuinte(anonymizeCpfCnpj(original.getDocumentoContribuinte()))
                .inscricaoEstadual(anonymizeInscricao(original.getInscricaoEstadual()))
                .codMunicipio(original.getCodMunicipio())
                .uf(original.getUf())

                // Auditoria - anonimiza operadores
                .contadorPagamento(original.getContadorPagamento())
                .operadorCadastro(anonymizeText("Operador"))
                .operadorAlteracao(anonymizeText("Operador"))

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
                .isPagoTotal(original.getIsPagoTotal())
                .build();
    }

    private static String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String anonymizeText(String prefix) {
        return prefix + " Anonimizado #" + random.nextInt(10000);
    }

    private static BigDecimal randomizeValue(BigDecimal original) {
        if (original == null) {
            return null;
        }
        double factor = 0.5 + random.nextDouble(); // entre 0.5x e 1.5x
        return original.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String anonymizeCpfCnpj(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        int length = documento.replaceAll("\\D", "").length();
        if (length == 11) {
            return String.format("%03d.***.**%d-%02d",
                    random.nextInt(1000), random.nextInt(10), random.nextInt(100));
        }
        return String.format("%02d.***.***/%04d-%02d",
                random.nextInt(100), random.nextInt(10000), random.nextInt(100));
    }

    private static String anonymizeInscricao(String inscricao) {
        if (inscricao == null || inscricao.isBlank()) {
            return null;
        }
        return "ISENTO";
    }
}