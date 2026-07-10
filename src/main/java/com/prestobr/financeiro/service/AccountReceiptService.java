package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.DataLakeClient;
import com.prestobr.financeiro.domain.entity.AccountReceipt;
import com.prestobr.financeiro.domain.util.AccountReceiptAnonymizer;
import com.prestobr.financeiro.dto.request.AccountReceiptPageRequest;
import com.prestobr.financeiro.dto.response.AccountReceiptResponse;
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
public class AccountReceiptService {

    private final DataLakeClient dataLakeClient;
    private final ApplicationContext applicationContext;

    @Value("${financeiro.account-receipt.anonymize-data:false}")
    private boolean anonymizeData;

    @Value("${datalake.gold-account-receipt-base-prefix}")
    private String goldAccountReceiptBasePrefix;

    public AccountReceiptService(DataLakeClient dataLakeClient, ApplicationContext applicationContext) {
        this.dataLakeClient = dataLakeClient;
        this.applicationContext = applicationContext;
    }

    private AccountReceiptService self() {
        return applicationContext.getBean(AccountReceiptService.class);
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountReceiptResponse> search(AccountReceiptPageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountReceipt> filtered = self().loadAll().stream()
                .filter(receipt -> matchesFilters(receipt, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    public AccountReceiptResponse getByNumeroDocumento(String numeroDocumento) {
        return self().loadAll().stream()
                .filter(receipt -> numeroDocumento.equals(receipt.getDocumentNumber()))
                .findFirst()
                .map(AccountReceiptResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Documento não encontrado: " + numeroDocumento
                ));
    }

    @CacheEvict(value = "accounts-receipt", allEntries = true)
    public void clearCache() {
        log.info("Cache de recebimentos limpo");
    }

    // =========================================================================
    // CARREGAMENTO DE DADOS
    // =========================================================================

    @Cacheable("accounts-receipt")
    public List<AccountReceipt> loadAll() {
        List<String> latestRunKeys = dataLakeClient.findLatestRunParquetKeysFromPrefix(goldAccountReceiptBasePrefix);

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Gold");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente (Gold)", latestRunKeys.size());

        List<AccountReceipt> all = new ArrayList<>();
        for (String key : latestRunKeys) {
            all.addAll(readParquetFile(key));
        }

        return all;
    }

    private List<AccountReceipt> readParquetFile(String s3Key) {
        List<AccountReceipt> receipts = new ArrayList<>();
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
                    receipts.add(mapToEntity(record));
                }
            }

            log.debug("Lidos {} registros de {}", receipts.size(), s3Key);

        } catch (Exception e) {
            log.error("Erro ao ler arquivo Parquet {}: {}", s3Key, e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return receipts;
    }

    // =========================================================================
    // MAPEAMENTO PARQUET -> ENTITY
    // =========================================================================

    private AccountReceipt mapToEntity(GenericRecord record) {
        AccountReceipt original = AccountReceipt.builder()
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
                ? AccountReceiptAnonymizer.anonymize(original)
                : original;
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private boolean matchesFilters(AccountReceipt receipt, AccountReceiptPageRequest request) {
        if (request.accountCode() != null && !request.accountCode().equals(receipt.getAccountCode())) {
            return false;
        }

        if (request.companyCode() != null && !request.companyCode().equals(receipt.getCompanyCode())) {
            return false;
        }

        if (request.documentNumber() != null && !request.documentNumber().equals(receipt.getDocumentNumber())) {
            return false;
        }

        if (request.type() != null && !request.type().equalsIgnoreCase(receipt.getType())) {
            return false;
        }

        if (request.isSettled() != null && !request.isSettled().equals(receipt.getIsSettled())) {
            return false;
        }

        if (request.description() != null && receipt.getDescription() != null
                && !receipt.getDescription().toLowerCase().contains(request.description().toLowerCase())) {
            return false;
        }

        if (request.movementDateFrom() != null && receipt.getMovementDate() != null) {
            if (receipt.getMovementDate().toLocalDate().isBefore(request.movementDateFrom())) {
                return false;
            }
        }

        if (request.movementDateTo() != null && receipt.getMovementDate() != null) {
            if (receipt.getMovementDate().toLocalDate().isAfter(request.movementDateTo())) {
                return false;
            }
        }

        return true;
    }

    // =========================================================================
    // PAGINAÇÃO E ORDENAÇÃO
    // =========================================================================

    private Pageable buildPageable(AccountReceiptPageRequest request) {
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

    private List<AccountReceipt> applySorting(List<AccountReceipt> list, Sort sort) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<AccountReceipt> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<AccountReceipt> fieldComparator = getComparator(order.getProperty());

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

    private Comparator<AccountReceipt> getComparator(String field) {
        return switch (field) {
            case "movementDate" -> Comparator.comparing(AccountReceipt::getMovementDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdAt" -> Comparator.comparing(AccountReceipt::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(AccountReceipt::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "documentValue" -> Comparator.comparing(AccountReceipt::getDocumentValue, Comparator.nullsLast(Comparator.naturalOrder()));
            case "currentBalance" -> Comparator.comparing(AccountReceipt::getCurrentBalance, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountReceipt> toPage(List<AccountReceipt> list, Pageable pageable) {
        List<AccountReceipt> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountReceiptResponse> toPageResponse(Page<AccountReceipt> page) {
        List<AccountReceiptResponse> content = page.getContent().stream()
                .map(AccountReceiptResponse::from)
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