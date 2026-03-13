package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.domain.util.AccountPayableAnonymizer;
import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.prestobr.financeiro.util.ParquetUtils.*;

@Slf4j
@Service
public class AccountPayableService {

    private final S3Client s3Client;
    private final ApplicationContext applicationContext;

    @Value("${datalake.bucket}")
    private String bucketName;

    @Value("${datalake.silver-account-payable-base-prefix}")
    private String silverPrefix;

    @Value("${financeiro.account-payable.anonymize-data:false}")
    private boolean anonymizeData;

    public AccountPayableService(S3Client s3Client, ApplicationContext applicationContext) {
        this.s3Client = s3Client;
        this.applicationContext = applicationContext;
    }

    // retorna o proxy do spring para que chamadas internas passem pelo cache
    private AccountPayableService self() {
        return applicationContext.getBean(AccountPayableService.class);
    }

    public PageResponse<AccountPayable> search(AccountPayablePageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountPayable> filtered = self().loadAllAccountsPayable().stream()
                .filter(ap -> matchesFilters(ap, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
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

    @Cacheable("accounts-payable")
    public List<AccountPayable> loadAllAccountsPayable() {
        List<String> latestRunKeys = findLatestRunParquetKeys();

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Silver");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente", latestRunKeys.size());

        List<AccountPayable> allAccountsPayable = new ArrayList<>();
        for (String key : latestRunKeys) {
            allAccountsPayable.addAll(readParquetFile(key));
        }

        return allAccountsPayable;
    }

    // Converte uma request em um pageable do spring.
    private Pageable buildPageable(AccountPayablePageRequest request) {
        // sem ordenação
        if (request.sort() == null || request.sort().isEmpty()) {
            return PageRequest.of(request.page(), request.size());
        }

        // com ordenação
        List<Sort.Order> orders = request.sort().stream()
                .map(s -> {
                    String[] parts = s.split(",");
                    String field = parts[0];
                    Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;
                    return new Sort.Order(direction, field);
                })
                .toList();

        return PageRequest.of(request.page(), request.size(), Sort.by(orders));
    }

    private boolean matchesFilters(AccountPayable ap, AccountPayablePageRequest request) {
        if ("PENDING".equalsIgnoreCase(request.paymentStatus()) && Boolean.TRUE.equals(ap.getIsPagoTotal())) {
            return false;
        }
        if ("PAID".equalsIgnoreCase(request.paymentStatus()) && !Boolean.TRUE.equals(ap.getIsPagoTotal())) {
            return false;
        }

        if (request.titleCode() != null && !request.titleCode().equals(ap.getCodigoTitulo())) {
            return false;
        }

        if (request.vendorCode() != null && !request.vendorCode().equals(ap.getCodFornecedor())) {
            return false;
        }

        if (request.providerCode() != null && !request.providerCode().equals(ap.getPrestador())) {
            return false;
        }

        if (request.installmentNumber() != null && !request.installmentNumber().equals(ap.getNumeroParcela())) {
            return false;
        }

        if (request.costCenterCode() != null && !request.costCenterCode().equals(ap.getCodCentroCusto())) {
            return false;
        }

        if (request.subCostCenterCode() != null && !request.subCostCenterCode().equals(ap.getCodSubcentroCusto())) {
            return false;
        }

        if (request.departmentCode() != null && !request.departmentCode().equals(ap.getCodSetor())) {
            return false;
        }

        if (request.accountPlan() != null && !request.accountPlan().equals(ap.getPlanoConta())) {
            return false;
        }

        if (request.description() != null && ap.getHistorico() != null
                && !ap.getHistorico().toLowerCase().contains(request.description().toLowerCase())) {
            return false;
        }

        if (request.documentType() != null && !request.documentType().equals(ap.getTipoDocumento())) {
            return false;
        }

        if (request.titleType() != null && !request.titleType().equals(ap.getTipoTitulo())) {
            return false;
        }

        if (request.operation() != null && !request.operation().equals(ap.getOperacao())) {
            return false;
        }

        if (request.paymentMethod() != null && !request.paymentMethod().equals(ap.getFormaPagamento())) {
            return false;
        }

        if (request.paymentOption() != null && !request.paymentOption().equals(ap.getOpcaoPagamento())) {
            return false;
        }

        if (request.emissionDateFrom() != null && ap.getDataEmissao() != null) {
            if (ap.getDataEmissao().toLocalDate().isBefore(request.emissionDateFrom())) {
                return false;
            }
        }

        if (request.emissionDateTo() != null && ap.getDataEmissao() != null) {
            if (ap.getDataEmissao().toLocalDate().isAfter(request.emissionDateTo())) {
                return false;
            }
        }

        if (request.dueDateFrom() != null && ap.getDataVencimento() != null) {
            if (ap.getDataVencimento().toLocalDate().isBefore(request.dueDateFrom())) {
                return false;
            }
        }

        if (request.dueDateTo() != null && ap.getDataVencimento() != null) {
            if (ap.getDataVencimento().toLocalDate().isAfter(request.dueDateTo())) {
                return false;
            }
        }

        return true;
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
        return anonymizeData
                ? AccountPayableAnonymizer.anonymize(original)
                : original;
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

    // recebe a lista completa e retorna apenas a página spring dela
    private Page<AccountPayable> toPage(List<AccountPayable> list, Pageable pageable) {
        List<AccountPayable> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    // converte page do spring para meu PageResponse
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

    @CacheEvict(value = "accounts-payable", allEntries = true)
    public void clearCache() {
        log.info("Cache de contas a pagar limpo");
    }
}