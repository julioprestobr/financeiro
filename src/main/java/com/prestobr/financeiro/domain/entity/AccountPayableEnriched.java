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
public class AccountPayableEnriched {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private String codigoTitulo;
    private String codigoCompra;

    // =========================================================================
    // EMPRESA
    // =========================================================================
    private String codEmpresa;
    private String nomeEmpresa;

    // =========================================================================
    // FORNECEDOR
    // =========================================================================
    private String codFornecedor;
    private String nomeFornecedor;
    private String fantasiaFornecedor;
    private String cnpjFornecedor;
    private String cpfFornecedor;

    // =========================================================================
    // TRANSPORTADOR
    // =========================================================================
    private String transportador;
    private String nomeTransportador;

    // =========================================================================
    // PRESTADOR
    // =========================================================================
    private String prestador;
    private String nomePrestador;

    // =========================================================================
    // STATUS
    // =========================================================================
    private String statusPagamento;
    private String nomeStatus;

    // =========================================================================
    // TIPO DOCUMENTO
    // =========================================================================
    private String tipoDocumento;
    private String nomeTipoDocumento;

    // =========================================================================
    // CENTRO DE CUSTO
    // =========================================================================
    private String codCentroCusto;
    private String nomeCentroCusto;

    // =========================================================================
    // SUBCENTRO DE CUSTO
    // =========================================================================
    private String codSubcentroCusto;
    private String nomeSubcentroCusto;

    // =========================================================================
    // PLANO DE CONTA
    // =========================================================================
    private String planoConta;
    private String nomePlanoConta;

    // =========================================================================
    // SETOR E CONTRATO
    // =========================================================================
    private String codSetor;
    private String contrato;

    // =========================================================================
    // DATAS
    // =========================================================================
    private LocalDateTime dataEmissao;
    private LocalDateTime dataVencimento;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAlteracao;

    // =========================================================================
    // VALORES
    // =========================================================================
    private BigDecimal valorTitulo;
    private BigDecimal valorPago;
    private BigDecimal valorSaldo;
    private BigDecimal valorBruto;
    private BigDecimal valorDesconto;
    private BigDecimal valorAcrescimo;
    private BigDecimal valorMovimento;
    private BigDecimal valorOutras;
    private BigDecimal atualizacaoMonetaria;

    // =========================================================================
    // FLAGS
    // =========================================================================
    private Boolean isPagoTotal;
    private Boolean isProvisao;

    // =========================================================================
    // CLASSIFICAÇÃO
    // =========================================================================
    private String situacaoTitulo;
    private String tipoTitulo;
    private String operacao;
    private String formaPagamento;
    private String opcaoPagamento;

    // =========================================================================
    // PARCELA / COMPETÊNCIA
    // =========================================================================
    private String numeroParcela;
    private String mesCompetencia;
    private String periodo;
    private String periodoApuracao;
    private String periodoReferencia;
    private Integer anoCalculo;
    private Integer diasAtraso;

    // =========================================================================
    // TEXTO / HISTÓRICO
    // =========================================================================
    private String historico;
    private String observacao;

    // =========================================================================
    // FISCAL
    // =========================================================================
    private String documentoContribuinte;
    private String inscricaoEstadual;
    private String codMunicipio;
    private String uf;

    // =========================================================================
    // AUDITORIA
    // =========================================================================
    private Integer contadorPagamento;
    private String operadorCadastro;
    private String operadorAlteracao;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;
}