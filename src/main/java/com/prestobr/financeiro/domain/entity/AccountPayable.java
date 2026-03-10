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
public class AccountPayable {

    // Identificação
    private String codigoTitulo;
    private String codigoCompra;

    // Vínculos
    private String codEmpresa;
    private String codFornecedor;
    private String codCentroCusto;
    private String codSubcentroCusto;
    private String codSetor;
    private String planoConta;
    private String contrato;
    private String prestador;

    // Datas
    private LocalDateTime dataEmissao;
    private LocalDateTime dataVencimento;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAlteracao;

    // Texto / Histórico
    private String historico;
    private String observacao;

    // Classificação
    private String tipoDocumento;
    private String tipoTitulo;
    private String operacao;
    private String formaPagamento;
    private String opcaoPagamento;

    // Status
    private String situacaoTitulo;
    private String statusPagamento;
    private Boolean isProvisao;

    // Valores
    private BigDecimal valorTitulo;
    private BigDecimal valorPago;
    private BigDecimal valorSaldo;
    private BigDecimal valorBruto;
    private BigDecimal valorDesconto;
    private BigDecimal valorAcrescimo;
    private BigDecimal valorMovimento;
    private BigDecimal valorOutras;
    private BigDecimal atualizacaoMonetaria;

    // Parcela / Competência
    private String numeroParcela;
    private String mesCompetencia;
    private String periodo;
    private String periodoApuracao;
    private String periodoReferencia;
    private Integer anoCalculo;
    private Integer diasAtraso;

    // Fiscal
    private String documentoContribuinte;
    private String inscricaoEstadual;
    private String codMunicipio;
    private String uf;

    // Auditoria
    private Integer contadorPagamento;
    private String operadorCadastro;
    private String operadorAlteracao;

    // Metadados
    private LocalDateTime snapshotDatetime;
    private Boolean isPagoTotal;
}
