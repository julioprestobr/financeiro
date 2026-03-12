package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.domain.util.AccountPayableAnonymizer;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
public class AccountPayableService {

    private final S3Client s3Client;
    private final ApplicationContext applicationContext;

    @Value("${datalake.bucket}")
    private String bucketName;

    @Value("${datalake.silver-account-payable-base-prefix}")
    private String silverPrefix;

    public AccountPayableService(S3Client s3Client, ApplicationContext applicationContext) {
        this.s3Client = s3Client;
        this.applicationContext = applicationContext;
    }

    private AccountPayableService self() {
        return applicationContext.getBean(AccountPayableService.class);
    }

    public PageResponse<AccountPayable> getLatestAccountsPayable(Pageable pageable) {
        List<AccountPayable> all = self().loadAllAccountsPayable();
        return toPageResponse(toPage(all, pageable));
    }

    public AccountPayable getByCodigoTitulo(String codigoTitulo) {
        return self().loadAllAccountsPayable().stream()
                .filter(ap -> codigoTitulo.equals(ap.getCodigoTitulo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Título não encontrado: " + codigoTitulo
                ));
    }

    public PageResponse<AccountPayable> getByFornecedor(String codFornecedor, Pageable pageable) {
        List<AccountPayable> filtered = self().loadAllAccountsPayable().stream()
                .filter(ap -> codFornecedor.equals(ap.getCodFornecedor()))
                .collect(Collectors.toList());
        return toPageResponse(toPage(filtered, pageable));    }

    public PageResponse<AccountPayable> getPendentes(Pageable pageable) {
        List<AccountPayable> filtered = self().loadAllAccountsPayable().stream()
                .filter(ap -> !Boolean.TRUE.equals(ap.getIsPagoTotal()))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    @Cacheable("accounts-payable")
    public List<AccountPayable> loadAllAccountsPayable() {
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

        log.info("Objetos encontrados no prefix '{}': {}", silverPrefix, allObjects.size());
        allObjects.forEach(obj -> log.info("  -> {}", obj.key()));

        List<S3Object> parquetFiles = allObjects.stream()
                .filter(obj -> obj.key().endsWith(".parquet"))
                .collect(Collectors.toList());

        if (parquetFiles.isEmpty()) {
            return Collections.emptyList();
        }

        Pattern runPattern = Pattern.compile("run-(\\d{8}_\\d{6})");

        Map<String, List<S3Object>> filesByRun = parquetFiles.stream()
                .collect(Collectors.groupingBy(obj -> {
                    Matcher matcher = runPattern.matcher(obj.key());
                    return matcher.find() ? matcher.group(1) : "unknown";
                }));

        filesByRun.remove("unknown");

        if (filesByRun.isEmpty()) {
            return parquetFiles.stream()
                    .sorted(Comparator.comparing(S3Object::lastModified).reversed())
                    .limit(10)
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        }

        String latestRun = filesByRun.keySet().stream()
                .max(Comparator.naturalOrder())
                .orElseThrow();

        log.info("Run mais recente identificada: {}", latestRun);

        return filesByRun.get(latestRun).stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    private List<AccountPayable> readParquetFile(String s3Key) {
        List<AccountPayable> accounts = new ArrayList<>();
        File tempFile = null;

        try {
            tempFile = Files.createTempFile("parquet_", ".parquet").toFile();
            tempFile.deleteOnExit();
            tempFile.delete();

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

    private AccountPayable mapToAccountPayable(GenericRecord record) {
        AccountPayable original = AccountPayable.builder()
                .codigoTitulo(getString(record, "codigo_titulo"))
                .codigoCompra(getString(record, "codigo_compra"))
                .codEmpresa(getString(record, "cod_empresa"))
                .codFornecedor(getString(record, "cod_fornecedor"))
                .codCentroCusto(getString(record, "cod_centro_custo"))
                .codSubcentroCusto(getString(record, "cod_subcentro_custo"))
                .codSetor(getString(record, "cod_setor"))
                .planoConta(getString(record, "plano_conta"))
                .contrato(getString(record, "contrato"))
                .prestador(getString(record, "prestador"))
                .dataEmissao(getLocalDateTime(record, "data_emissao"))
                .dataVencimento(getLocalDateTime(record, "data_vencimento"))
                .dataEntrada(getLocalDateTime(record, "data_entrada"))
                .dataCadastro(getLocalDateTime(record, "data_cadastro"))
                .dataAlteracao(getLocalDateTime(record, "data_alteracao"))
                .historico(getString(record, "historico"))
                .observacao(getString(record, "observacao"))
                .tipoDocumento(getString(record, "tipo_documento"))
                .tipoTitulo(getString(record, "tipo_titulo"))
                .operacao(getString(record, "operacao"))
                .formaPagamento(getString(record, "forma_pagamento"))
                .opcaoPagamento(getString(record, "opcao_pagamento"))
                .situacaoTitulo(getString(record, "situacao_titulo"))
                .statusPagamento(getString(record, "status_pagamento"))
                .isProvisao(getBoolean(record, "is_provisao"))
                .valorTitulo(getBigDecimal(record, "valor_titulo"))
                .valorPago(getBigDecimal(record, "valor_pago"))
                .valorSaldo(getBigDecimal(record, "valor_saldo"))
                .valorBruto(getBigDecimal(record, "valor_bruto"))
                .valorDesconto(getBigDecimal(record, "valor_desconto"))
                .valorAcrescimo(getBigDecimal(record, "valor_acrescimo"))
                .valorMovimento(getBigDecimal(record, "valor_movimento"))
                .valorOutras(getBigDecimal(record, "valor_outras"))
                .atualizacaoMonetaria(getBigDecimal(record, "atualizacao_monetaria"))
                .numeroParcela(getString(record, "numero_parcela"))
                .mesCompetencia(getString(record, "mes_competencia"))
                .periodo(getString(record, "periodo"))
                .periodoApuracao(getString(record, "periodo_apuracao"))
                .periodoReferencia(getString(record, "periodo_referencia"))
                .anoCalculo(getInteger(record, "ano_calculo"))
                .diasAtraso(getInteger(record, "dias_atraso"))
                .documentoContribuinte(getString(record, "documento_contribuinte"))
                .inscricaoEstadual(getString(record, "inscricao_estadual"))
                .codMunicipio(getString(record, "cod_municipio"))
                .uf(getString(record, "uf"))
                .contadorPagamento(getInteger(record, "contador_pagamento"))
                .operadorCadastro(getString(record, "operador_cadastro"))
                .operadorAlteracao(getString(record, "operador_alteracao"))
                .snapshotDatetime(getLocalDateTime(record, "snapshot_datetime"))
                .isPagoTotal(getBoolean(record, "is_pago_total"))
                .build();
        return AccountPayableAnonymizer.anonymize(original);
    }

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

            if (value instanceof Long) {
                long micros = (Long) value;
                long millis = micros / 1000;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            }

            if (value instanceof CharSequence) {
                return LocalDateTime.parse(value.toString());
            }

            return null;
        } catch (Exception e) {
            log.trace("Erro ao converter campo {} para LocalDateTime: {}", field, e.getMessage());
            return null;
        }
    }

    private List<AccountPayable> applySorting(List<AccountPayable> list, Sort sort) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<AccountPayable> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<AccountPayable> fieldComparator = getComparator(order.getProperty());

            if (fieldComparator != null) {
                if (order.isDescending()) {
                    fieldComparator = fieldComparator.reversed();
                }
                comparator = (comparator == null) ? fieldComparator : comparator.thenComparing(fieldComparator);
            }
        }

        if (comparator == null) {
            return list;
        }

        return list.stream().sorted(comparator).collect(Collectors.toList());
    }

    private Comparator<AccountPayable> getComparator(String field) {
        return switch (field) {
            case "dataEmissao" -> Comparator.comparing(AccountPayable::getDataEmissao, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataVencimento" -> Comparator.comparing(AccountPayable::getDataVencimento, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataEntrada" -> Comparator.comparing(AccountPayable::getDataEntrada, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataCadastro" -> Comparator.comparing(AccountPayable::getDataCadastro, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataAlteracao" -> Comparator.comparing(AccountPayable::getDataAlteracao, Comparator.nullsLast(Comparator.naturalOrder()));
            case "valorTitulo" -> Comparator.comparing(AccountPayable::getValorTitulo, Comparator.nullsLast(Comparator.naturalOrder()));
            case "numeroParcela" -> Comparator.comparing(AccountPayable::getNumeroParcela, Comparator.nullsLast(Comparator.naturalOrder()));
            case "diasAtraso" -> Comparator.comparing(AccountPayable::getDiasAtraso, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountPayable> toPage(List<AccountPayable> list, Pageable pageable) {
        log.info("=== DEBUG SORT ===");
        log.info("Sort recebido: {}", pageable.getSort());
        log.info("Sort is unsorted: {}", pageable.getSort().isUnsorted());

        List<AccountPayable> sorted = applySorting(list, pageable.getSort());

        if (!sorted.isEmpty()) {
            log.info("Primeiro item dataEmissao: {}", sorted.get(0).getDataEmissao());
            log.info("Último item dataEmissao: {}", sorted.get(sorted.size() - 1).getDataEmissao());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountPayable> toPageResponse(Page<AccountPayable> page) {
        return new PageResponse<>(
                new Pagination(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                ),
                page.getContent()
        );
    }
}