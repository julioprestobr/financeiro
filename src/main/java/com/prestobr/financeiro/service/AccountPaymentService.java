package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.DataLakeClient;
import com.prestobr.financeiro.domain.entity.AccountPayment;
import com.prestobr.financeiro.domain.util.AccountPaymentAnonymizer;
import com.prestobr.financeiro.dto.request.AccountPaymentPageRequest;
import com.prestobr.financeiro.dto.response.AccountPaymentResponse;
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

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.prestobr.financeiro.util.ParquetUtils.*;

@Slf4j
@Service
public class AccountPaymentService {

    private final DataLakeClient dataLakeClient;
    private final ApplicationContext applicationContext;

    @Value("${financeiro.account-payment.anonymize-data:false}")
    private boolean anonymizeData;

    @Value("${datalake.gold-account-payment-base-prefix}")
    private String goldAccountPaymentBasePrefix;

    public AccountPaymentService(DataLakeClient dataLakeClient, ApplicationContext applicationContext) {
        this.dataLakeClient = dataLakeClient;
        this.applicationContext = applicationContext;
    }

    private AccountPaymentService self() {
        return applicationContext.getBean(AccountPaymentService.class);
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountPaymentResponse> search(AccountPaymentPageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountPayment> filtered = self().loadAll().stream()
                .filter(payment -> matchesFilters(payment, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    public AccountPaymentResponse getByNumeroDocumento(String numeroDocumento) {
        return self().loadAll().stream()
                .filter(payment -> numeroDocumento.equals(payment.getDocumentNumber()))
                .findFirst()
                .map(AccountPaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Documento não encontrado: " + numeroDocumento
                ));
    }

    @CacheEvict(value = "accounts-payment", allEntries = true)
    public void clearCache() {
        log.info("Cache de pagamentos limpo");
    }

    // =========================================================================
    // CARREGAMENTO DE DADOS
    // =========================================================================

    @Cacheable("accounts-payment")
    public List<AccountPayment> loadAll() {
        List<String> latestRunKeys = dataLakeClient.findLatestRunParquetKeysFromPrefix(goldAccountPaymentBasePrefix);

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Gold");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente (Gold)", latestRunKeys.size());

        List<AccountPayment> all = new ArrayList<>();
        for (String key : latestRunKeys) {
            all.addAll(readParquetFile(key));
        }

        return all;
    }

    private List<AccountPayment> readParquetFile(String s3Key) {
        List<AccountPayment> payments = new ArrayList<>();
        File tempFile = null;

        try {
            tempFile = dataLakeClient.downloadToTempFile(s3Key);

            Configuration hadoopConf = new Configuration();
            Path parquetPath = new Path(tempFile.getAbsolutePath());

            try (ParquetReader<GenericRecord> reader = AvroParquetReader
                    .<GenericRecord>builder(HadoopInputFile.fromPath(parquetPath, hadoopConf))
                    .build()) {

                GenericRecord record;
                while ((record = reader.read()) != null) {
                    payments.add(mapToEntity(record));
                }
            }

            log.debug("Lidos {} registros de {}", payments.size(), s3Key);

        } catch (Exception e) {
            log.error("Erro ao ler arquivo Parquet {}: {}", s3Key, e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return payments;
    }

    // =========================================================================
    // MAPEAMENTO PARQUET -> ENTITY
    // =========================================================================

    private AccountPayment mapToEntity(GenericRecord record) {
        AccountPayment original = AccountPayment.builder()
                // Identificação
                .documentNumber(getString(record, "num_documento"))

                // Conta Bancária
                .accountCode(getString(record, "cod_conta"))
                .accountName(getString(record, "nome_conta"))
                .bankCode(getString(record, "cod_banco"))

                // Empresa
                .companyCode(getString(record, "cod_empresa"))
                .companyName(getString(record, "nome_empresa"))

                // Classificação
                .type(getString(record, "tipo"))
                .isSettled(getBoolean(record, "is_compensado"))

                // Datas
                .movementDate(getLocalDateTime(record, "data_movimentacao"))
                .createdAt(getLocalDateTime(record, "data_cadastro"))
                .updatedAt(getLocalDateTime(record, "data_alteracao"))

                // Valores
                .documentValue(getBigDecimal(record, "vlr_documento"))
                .currentBalance(getBigDecimal(record, "saldo_atual"))
                .settledBalance(getBigDecimal(record, "saldo_compensado"))

                // Vendas vinculadas
                .sale1(getString(record, "venda_1"))
                .sale2(getString(record, "venda_2"))
                .sale3(getString(record, "venda_3"))
                .sale4(getString(record, "venda_4"))
                .sale5(getString(record, "venda_5"))

                // Texto / Histórico
                .description(getString(record, "historico"))
                .destination(getString(record, "destino"))
                .notes(getString(record, "observacao"))

                // Auditoria
                .createdBy(getString(record, "operador_cadastro"))
                .updatedBy(getString(record, "operador_alteracao"))

                // Metadados
                .snapshotDatetime(getLocalDateTime(record, "snapshot_datetime"))
                .build();

        return anonymizeData
                ? AccountPaymentAnonymizer.anonymize(original)
                : original;
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private boolean matchesFilters(AccountPayment payment, AccountPaymentPageRequest request) {
        if (request.accountCode() != null && !request.accountCode().equals(payment.getAccountCode())) {
            return false;
        }

        if (request.companyCode() != null && !request.companyCode().equals(payment.getCompanyCode())) {
            return false;
        }

        if (request.documentNumber() != null && !request.documentNumber().equals(payment.getDocumentNumber())) {
            return false;
        }

        if (request.type() != null && !request.type().equalsIgnoreCase(payment.getType())) {
            return false;
        }

        if (request.isSettled() != null && !request.isSettled().equals(payment.getIsSettled())) {
            return false;
        }

        if (request.description() != null && payment.getDescription() != null
                && !payment.getDescription().toLowerCase().contains(request.description().toLowerCase())) {
            return false;
        }

        if (request.movementDateFrom() != null && payment.getMovementDate() != null) {
            if (payment.getMovementDate().toLocalDate().isBefore(request.movementDateFrom())) {
                return false;
            }
        }

        if (request.movementDateTo() != null && payment.getMovementDate() != null) {
            if (payment.getMovementDate().toLocalDate().isAfter(request.movementDateTo())) {
                return false;
            }
        }

        return true;
    }

    // =========================================================================
    // PAGINAÇÃO E ORDENAÇÃO
    // =========================================================================

    private Pageable buildPageable(AccountPaymentPageRequest request) {
        if (request.sort() == null || request.sort().isEmpty()) {
            return PageRequest.of(request.page(), request.size());
        }

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

    private List<AccountPayment> applySorting(List<AccountPayment> list, Sort sort) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<AccountPayment> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<AccountPayment> fieldComparator = getComparator(order.getProperty());

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

    private Comparator<AccountPayment> getComparator(String field) {
        return switch (field) {
            case "movementDate" -> Comparator.comparing(AccountPayment::getMovementDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdAt" -> Comparator.comparing(AccountPayment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(AccountPayment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "documentValue" -> Comparator.comparing(AccountPayment::getDocumentValue, Comparator.nullsLast(Comparator.naturalOrder()));
            case "currentBalance" -> Comparator.comparing(AccountPayment::getCurrentBalance, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountPayment> toPage(List<AccountPayment> list, Pageable pageable) {
        List<AccountPayment> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountPaymentResponse> toPageResponse(Page<AccountPayment> page) {
        List<AccountPaymentResponse> content = page.getContent().stream()
                .map(AccountPaymentResponse::from)
                .toList();

        return new PageResponse<>(
                new Pagination(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                ),
                content
        );
    }

}
