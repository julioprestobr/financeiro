package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountPayableService {

    private final S3Client s3Client;

    @Value("${datalake.bucket}")
    private String bucketName;

    @Value("${datalake.silver.contas-a-pagar.prefix}")
    private String silverPrefix;

    /**
     * Busca todas as contas a pagar da run mais recente do Data Lake Silver.
     */
    public List<AccountPayable> getLatestAccountsPayable() {
        List<String> latestRunKeys = findLatestRunParquetKeys();

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Silver");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente", latestRunKeys.size());

        List<AccountPayable> allAccounts = new ArrayList<>();
        for (String key : latestRunKeys) {
            allAccounts.addAll(readParquetFile(key));
        }

        return allAccounts;
    }

    /**
     * Busca uma conta a pagar específica por código do título.
     */
    public Optional<AccountPayable> getByCodigoTitulo(String codigoTitulo) {
        return getLatestAccountsPayable().stream()
                .filter(ap -> codigoTitulo.equals(ap.getCodigoTitulo()))
                .findFirst();
    }

    /**
     * Busca contas a pagar por código do fornecedor.
     */
    public List<AccountPayable> getByFornecedor(String codFornecedor) {
        return getLatestAccountsPayable().stream()
                .filter(ap -> codFornecedor.equals(ap.getCodFornecedor()))
                .collect(Collectors.toList());
    }

    /**
     * Busca contas a pagar por documento do contribuinte (CPF/CNPJ).
     */
    public List<AccountPayable> getByDocumentoContribuinte(String documento) {
        return getLatestAccountsPayable().stream()
                .filter(ap -> documento.equals(ap.getDocumentoContribuinte()))
                .collect(Collectors.toList());
    }

    /**
     * Busca contas a pagar pendentes (não pagas totalmente).
     */
    public List<AccountPayable> getPendentes() {
        return getLatestAccountsPayable().stream()
                .filter(ap -> !Boolean.TRUE.equals(ap.getIsPagoTotal()))
                .collect(Collectors.toList());
    }

    /**
     * Encontra as chaves dos arquivos Parquet da run mais recente.
     *
     * Estrutura esperada:
     * silver/financeiro/contas_a_pagar/system=databit/build_type=full/year=YYYY/month=MM/day=DD/
     *   contas_a_pagar_silver_YYYYMMDD_HHMMSS_run-YYYYMMDD_HHMMSS_part-XXXX.parquet
     */
    private List<String> findLatestRunParquetKeys() {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(silverPrefix)
                .build();

        List<S3Object> allObjects = new ArrayList<>();
        ListObjectsV2Response response;

        do {
            response = s3Client.listObjectsV2(listRequest);
            allObjects.addAll(response.contents());
            listRequest = listRequest.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();
        } while (response.isTruncated());

        List<S3Object> parquetFiles = allObjects.stream()
                .filter(obj -> obj.key().endsWith(".parquet"))
                .collect(Collectors.toList());

        if (parquetFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // Agrupa por "run" extraindo o identificador da run do nome do arquivo
        // Padrão: contas_a_pagar_silver_YYYYMMDD_HHMMSS_run-YYYYMMDD_HHMMSS_part-XXXX.parquet
        Pattern runPattern = Pattern.compile("run-(\\d{8}_\\d{6})");

        Map<String, List<S3Object>> filesByRun = parquetFiles.stream()
                .collect(Collectors.groupingBy(obj -> {
                    Matcher matcher = runPattern.matcher(obj.key());
                    return matcher.find() ? matcher.group(1) : "unknown";
                }));

        filesByRun.remove("unknown");

        if (filesByRun.isEmpty()) {
            // Fallback: pega os arquivos mais recentes por lastModified
            return parquetFiles.stream()
                    .sorted(Comparator.comparing(S3Object::lastModified).reversed())
                    .limit(10)
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        }

        // Encontra a run mais recente (maior valor = mais recente)
        String latestRun = filesByRun.keySet().stream()
                .max(Comparator.naturalOrder())
                .orElseThrow();

        log.info("Run mais recente identificada: {}", latestRun);

        return filesByRun.get(latestRun).stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Lê um arquivo Parquet do S3 e converte para lista de AccountPayable.
     */
    private List<AccountPayable> readParquetFile(String s3Key) {
        List<AccountPayable> accounts = new ArrayList<>();
        File tempFile = null;

        try {
            tempFile = Files.createTempFile("parquet_", ".parquet").toFile();
            tempFile.deleteOnExit();

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.getObject(getRequest, ResponseTransformer.toFile(tempFile));

            Configuration hadoopConf = new Configuration();
            Path parquetPath = new Path(tempFile.getAbsolutePath());

            try (ParquetReader<GenericRecord> reader = AvroParquetReader
                    .<GenericRecord>builder(HadoopInputFile.fromPath(parquetPath, hadoopConf))
                    .build()) {

                GenericRecord record;
                while ((record = reader.read()) != null) {
                    accounts.add(mapToAccountPayable(record));
                }
            }

            log.debug("Lidos {} registros de {}", accounts.size(), s3Key);

        } catch (Exception e) {
            log.error("Erro ao ler arquivo Parquet {}: {}", s3Key, e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return accounts;
    }

    /**
     * Mapeia um registro Avro/Parquet para AccountPayable.
     * Os nomes dos campos seguem o schema do silver (snake_case).
     */
    private AccountPayable mapToAccountPayable(GenericRecord record) {
        return AccountPayable.builder()
                // Identificação
                .codigoTitulo(getString(record, "codigo_titulo"))
                .codigoCompra(getString(record, "codigo_compra"))

                // Vínculos
                .codEmpresa(getString(record, "cod_empresa"))
                .codFornecedor(getString(record, "cod_fornecedor"))
                .codCentroCusto(getString(record, "cod_centro_custo"))
                .codSubcentroCusto(getString(record, "cod_subcentro_custo"))
                .codSetor(getString(record, "cod_setor"))
                .planoConta(getString(record, "plano_conta"))
                .contrato(getString(record, "contrato"))
                .prestador(getString(record, "prestador"))

                // Datas
                .dataEmissao(getLocalDateTime(record, "data_emissao"))
                .dataVencimento(getLocalDateTime(record, "data_vencimento"))
                .dataEntrada(getLocalDateTime(record, "data_entrada"))
                .dataCadastro(getLocalDateTime(record, "data_cadastro"))
                .dataAlteracao(getLocalDateTime(record, "data_alteracao"))

                // Texto / Histórico
                .historico(getString(record, "historico"))
                .observacao(getString(record, "observacao"))

                // Classificação
                .tipoDocumento(getString(record, "tipo_documento"))
                .tipoTitulo(getString(record, "tipo_titulo"))
                .operacao(getString(record, "operacao"))
                .formaPagamento(getString(record, "forma_pagamento"))
                .opcaoPagamento(getString(record, "opcao_pagamento"))

                // Status
                .situacaoTitulo(getString(record, "situacao_titulo"))
                .statusPagamento(getString(record, "status_pagamento"))
                .isProvisao(getBoolean(record, "is_provisao"))

                // Valores
                .valorTitulo(getBigDecimal(record, "valor_titulo"))
                .valorPago(getBigDecimal(record, "valor_pago"))
                .valorSaldo(getBigDecimal(record, "valor_saldo"))
                .valorBruto(getBigDecimal(record, "valor_bruto"))
                .valorDesconto(getBigDecimal(record, "valor_desconto"))
                .valorAcrescimo(getBigDecimal(record, "valor_acrescimo"))
                .valorMovimento(getBigDecimal(record, "valor_movimento"))
                .valorOutras(getBigDecimal(record, "valor_outras"))
                .atualizacaoMonetaria(getBigDecimal(record, "atualizacao_monetaria"))

                // Parcela / Competência
                .numeroParcela(getString(record, "numero_parcela"))
                .mesCompetencia(getString(record, "mes_competencia"))
                .periodo(getString(record, "periodo"))
                .periodoApuracao(getString(record, "periodo_apuracao"))
                .periodoReferencia(getString(record, "periodo_referencia"))
                .anoCalculo(getInteger(record, "ano_calculo"))
                .diasAtraso(getInteger(record, "dias_atraso"))

                // Fiscal
                .documentoContribuinte(getString(record, "documento_contribuinte"))
                .inscricaoEstadual(getString(record, "inscricao_estadual"))
                .codMunicipio(getString(record, "cod_municipio"))
                .uf(getString(record, "uf"))

                // Auditoria
                .contadorPagamento(getInteger(record, "contador_pagamento"))
                .operadorCadastro(getString(record, "operador_cadastro"))
                .operadorAlteracao(getString(record, "operador_alteracao"))

                // Metadados
                .snapshotDatetime(getLocalDateTime(record, "snapshot_datetime"))
                .isPagoTotal(getBoolean(record, "is_pago_total"))

                .build();
    }

    // ==================== Helpers para extração segura de valores ====================

    private String getString(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInteger(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Long) return ((Long) value).intValue();
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLong(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;
            if (value instanceof Long) return (Long) value;
            if (value instanceof Number) return ((Number) value).longValue();
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;
            if (value instanceof BigDecimal) return (BigDecimal) value;
            // Parquet pode armazenar decimals como bytes ou strings
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getBoolean(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;
            if (value instanceof Boolean) return (Boolean) value;
            String strValue = value.toString().toLowerCase();
            return "true".equals(strValue) || "s".equals(strValue) || "1".equals(strValue);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTime(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;

            // Parquet armazena timestamps como microssegundos desde epoch
            if (value instanceof Long) {
                long micros = (Long) value;
                // Converte microssegundos para milissegundos
                long millis = micros / 1000;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            }

            // Se for string ISO
            if (value instanceof CharSequence) {
                return LocalDateTime.parse(value.toString());
            }

            return null;
        } catch (Exception e) {
            log.trace("Erro ao converter campo {} para LocalDateTime: {}", field, e.getMessage());
            return null;
        }
    }
}
