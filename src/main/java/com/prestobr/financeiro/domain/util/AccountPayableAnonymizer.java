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
                // Identificação
                .codigoTitulo(generateCode("TIT"))
                .codigoCompra(generateCode("CMP"))

                // Empresa - mantém código, anonimiza nome
                .codEmpresa(original.getCodEmpresa())
                .nomeEmpresa(anonymizeText("Empresa"))

                // Fornecedor - anonimiza tudo
                .codFornecedor(generateCode("FOR"))
                .nomeFornecedor(anonymizeText("Fornecedor"))
                .fantasiaFornecedor(anonymizeText("Fantasia"))
                .cnpjFornecedor(anonymizeCnpj())
                .cpfFornecedor(anonymizeCpf())

                // Transportador
                .transportador(generateCode("TRA"))
                .nomeTransportador(anonymizeText("Transportador"))

                // Prestador
                .prestador(generateCode("PRE"))
                .nomePrestador(anonymizeText("Prestador"))

                // Status - mantém código, anonimiza nome
                .statusPagamento(original.getStatusPagamento())
                .nomeStatus(original.getNomeStatus())

                // Tipo Documento - mantém
                .tipoDocumento(original.getTipoDocumento())
                .nomeTipoDocumento(original.getNomeTipoDocumento())

                // Centro de Custo - mantém código, anonimiza nome
                .codCentroCusto(original.getCodCentroCusto())
                .nomeCentroCusto(anonymizeText("Centro Custo"))

                // Subcentro de Custo - mantém código, anonimiza nome
                .codSubcentroCusto(original.getCodSubcentroCusto())
                .nomeSubcentroCusto(anonymizeText("Subcentro"))

                // Plano de Conta - mantém código, anonimiza nome
                .planoConta(original.getPlanoConta())
                .nomePlanoConta(anonymizeText("Plano Conta"))

                // Setor e Contrato
                .codSetor(original.getCodSetor())
                .contrato(generateCode("CTR"))

                // Datas - mantém
                .dataEmissao(original.getDataEmissao())
                .dataVencimento(original.getDataVencimento())
                .dataEntrada(original.getDataEntrada())
                .dataCadastro(original.getDataCadastro())
                .dataAlteracao(original.getDataAlteracao())

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

                // Flags - mantém
                .isPagoTotal(original.getIsPagoTotal())
                .isProvisao(original.getIsProvisao())

                // Classificação - mantém
                .situacaoTitulo(original.getSituacaoTitulo())
                .tipoTitulo(original.getTipoTitulo())
                .operacao(original.getOperacao())
                .formaPagamento(original.getFormaPagamento())
                .opcaoPagamento(original.getOpcaoPagamento())

                // Parcela / Competência - mantém
                .numeroParcela(original.getNumeroParcela())
                .mesCompetencia(original.getMesCompetencia())
                .periodo(original.getPeriodo())
                .periodoApuracao(original.getPeriodoApuracao())
                .periodoReferencia(original.getPeriodoReferencia())
                .anoCalculo(original.getAnoCalculo())
                .diasAtraso(original.getDiasAtraso())

                // Texto / Histórico - anonimiza
                .historico(anonymizeText("Histórico"))
                .observacao(anonymizeText("Observação"))

                // Fiscal - anonimiza
                .documentoContribuinte(anonymizeCpfCnpj(original.getDocumentoContribuinte()))
                .inscricaoEstadual("ISENTO")
                .codMunicipio(original.getCodMunicipio())
                .uf(original.getUf())

                // Auditoria - anonimiza operadores
                .contadorPagamento(original.getContadorPagamento())
                .operadorCadastro(anonymizeText("Operador"))
                .operadorAlteracao(anonymizeText("Operador"))

                // Metadados - mantém
                .snapshotDatetime(original.getSnapshotDatetime())
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
        double factor = 0.5 + random.nextDouble();
        return original.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String anonymizeCpf() {
        return String.format("%03d.***.**%d-%02d",
                random.nextInt(1000), random.nextInt(10), random.nextInt(100));
    }

    private static String anonymizeCnpj() {
        return String.format("%02d.***.***/%04d-%02d",
                random.nextInt(100), random.nextInt(10000), random.nextInt(100));
    }

    private static String anonymizeCpfCnpj(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        int length = documento.replaceAll("\\D", "").length();
        return length == 11 ? anonymizeCpf() : anonymizeCnpj();
    }
}