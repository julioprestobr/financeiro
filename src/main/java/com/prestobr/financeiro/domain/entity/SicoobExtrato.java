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
public class SicoobExtrato {

    // =========================================================================
    // IDENTIFICAÇÃO
    // =========================================================================
    private Integer id;
    private String entryId;
    private String documentNumber;

    // =========================================================================
    // LANÇAMENTO
    // =========================================================================
    private LocalDate movementDate;
    private String description;
    private String entryType;
    private BigDecimal amount;

    // =========================================================================
    // SALDOS
    // =========================================================================
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;

    // =========================================================================
    // PERÍODO
    // =========================================================================
    private Integer movementYear;
    private Integer movementMonth;

    // =========================================================================
    // METADADOS
    // =========================================================================
    private LocalDateTime snapshotDatetime;

    // Colunas nao mapeadas e o motivo:
    //
    // valor_absoluto: identica a valor em 100% das linhas, e valor nunca e
    //   negativo. O sinal do lancamento vive em tipo_lancamento.
    // tipo_movimentacao: identica a tipo_lancamento em 100% das linhas.
    // mes_referencia / ano_referencia: identicas a mes_movimento /
    //   ano_movimento em 100% das linhas, que por sua vez batem com
    //   data_movimento.
    // created_at / updated_at: auditoria de ETL, como nos demais dominios.
    //
    // Sobre os saldos: saldo_atual e o saldo FINAL da conta replicado em todas
    // as linhas, nao um saldo progressivo por lancamento. saldo_anterior e o
    // saldo de abertura do mes. Por isso os nomes openingBalance e
    // closingBalance, e por isso o endpoint de summary existe: e nele que a
    // leitura correta dos saldos aparece.
    //
    // codigo_historico foi mapeado como entryId, nao como codigo de historico:
    // sao 843 valores distintos em 843 linhas (identificador unico). O
    // historico de verdade e a coluna historico, com 26 valores distintos.
}
