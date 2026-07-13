package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.DataLakeClient;
import com.prestobr.financeiro.domain.entity.AccountBalanceHistory;
import com.prestobr.financeiro.domain.util.AccountBalanceHistoryAnonymizer;
import com.prestobr.financeiro.dto.request.AccountBalanceHistoryPageRequest;
import com.prestobr.financeiro.dto.response.AccountBalanceHistoryResponse;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.prestobr.financeiro.util.ParquetUtils.*;

@Slf4j
@Service
public class AccountBalanceHistoryService {

    private final DataLakeClient dataLakeClient;
    private final ApplicationContext applicationContext;

    @Value("${financeiro.account-balance-history.anonymize-data:false}")
    private boolean anonymizeData;

    @Value("${datalake.gold-account-balance-history-base-prefix}")
    private String goldAccountBalanceHistoryBasePrefix;

    public AccountBalanceHistoryService(DataLakeClient dataLakeClient, ApplicationContext applicationContext) {
        this.dataLakeClient = dataLakeClient;
        this.applicationContext = applicationContext;
    }

    private AccountBalanceHistoryService self() {
        return applicationContext.getBean(AccountBalanceHistoryService.class);
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountBalanceHistoryResponse> search(AccountBalanceHistoryPageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountBalanceHistory> filtered = self().loadAll().stream()
                .filter(balance -> matchesFilters(balance, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    public AccountBalanceHistoryResponse getByAccountAndDate(String accountCode, LocalDate referenceDate) {
        return self().loadAll().stream()
                .filter(balance -> accountCode.equals(balance.getAccountCode())
                        && referenceDate.equals(balance.getReferenceDate()))
                .findFirst()
                .map(AccountBalanceHistoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saldo não encontrado para conta " + accountCode + " na data " + referenceDate
                ));
    }

    @CacheEvict(value = "accounts-balance-history", allEntries = true)
    public void clearCache() {
        log.info("Cache de histórico de saldo limpo");
    }

    // =========================================================================
    // CARREGAMENTO DE DADOS
    // =========================================================================

    @Cacheable("accounts-balance-history")
    public List<AccountBalanceHistory> loadAll() {
        List<String> latestRunKeys = dataLakeClient.findLatestRunParquetKeysFromPrefix(goldAccountBalanceHistoryBasePrefix);

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Gold");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente (Gold)", latestRunKeys.size());

        List<AccountBalanceHistory> all = new ArrayList<>();
        for (String key : latestRunKeys) {
            all.addAll(readParquetFile(key));
        }

        return all;
    }

    private List<AccountBalanceHistory> readParquetFile(String s3Key) {
        List<AccountBalanceHistory> balances = new ArrayList<>();
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
                    balances.add(mapToEntity(record));
                }
            }

            log.debug("Lidos {} registros de {}", balances.size(), s3Key);

        } catch (Exception e) {
            log.error("Erro ao ler arquivo Parquet {}: {}", s3Key, e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return balances;
    }

    // =========================================================================
    // MAPEAMENTO PARQUET -> ENTITY
    // =========================================================================

    private AccountBalanceHistory mapToEntity(GenericRecord record) {
        AccountBalanceHistory original = AccountBalanceHistory.builder()
                // Conta Bancária
                .accountCode(getString(record, "cod_conta"))
                .accountName(getString(record, "nome_conta"))
                .bankCode(getString(record, "cod_banco"))

                // Empresa
                .companyCode(getString(record, "cod_empresa"))
                .companyName(getString(record, "nome_empresa"))

                // Data
                .referenceDate(getLocalDate(record, "data"))
                .dayOfWeek(getInteger(record, "dia_semana"))
                .isBusinessDay(getBoolean(record, "is_dia_util"))

                // Movimento do dia
                .dailyCredits(getBigDecimal(record, "creditos_dia"))
                .dailyDebits(getBigDecimal(record, "debitos_dia"))
                .netDailyMovement(getBigDecimal(record, "movimento_liquido_dia"))
                .dailyMovementCount(getInteger(record, "qtd_movimentos_dia"))

                // Saldo
                .accumulatedBalance(getBigDecimal(record, "saldo_acumulado"))

                // Metadados
                .snapshotDatetime(getLocalDateTime(record, "snapshot_datetime"))
                .build();

        return anonymizeData
                ? AccountBalanceHistoryAnonymizer.anonymize(original)
                : original;
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private boolean matchesFilters(AccountBalanceHistory balance, AccountBalanceHistoryPageRequest request) {
        if (request.accountCode() != null && !request.accountCode().equals(balance.getAccountCode())) {
            return false;
        }

        if (request.companyCode() != null && !request.companyCode().equals(balance.getCompanyCode())) {
            return false;
        }

        if (request.bankCode() != null && !request.bankCode().equals(balance.getBankCode())) {
            return false;
        }

        if (request.isBusinessDay() != null && !request.isBusinessDay().equals(balance.getIsBusinessDay())) {
            return false;
        }

        if (request.referenceDateFrom() != null && balance.getReferenceDate() != null) {
            if (balance.getReferenceDate().isBefore(request.referenceDateFrom())) {
                return false;
            }
        }

        if (request.referenceDateTo() != null && balance.getReferenceDate() != null) {
            if (balance.getReferenceDate().isAfter(request.referenceDateTo())) {
                return false;
            }
        }

        return true;
    }

    // =========================================================================
    // PAGINAÇÃO E ORDENAÇÃO
    // =========================================================================

    private Pageable buildPageable(AccountBalanceHistoryPageRequest request) {
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

    private List<AccountBalanceHistory> applySorting(List<AccountBalanceHistory> list, Sort sort) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<AccountBalanceHistory> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<AccountBalanceHistory> fieldComparator = getComparator(order.getProperty());

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

    private Comparator<AccountBalanceHistory> getComparator(String field) {
        return switch (field) {
            case "referenceDate" -> Comparator.comparing(AccountBalanceHistory::getReferenceDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "accumulatedBalance" -> Comparator.comparing(AccountBalanceHistory::getAccumulatedBalance, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dailyCredits" -> Comparator.comparing(AccountBalanceHistory::getDailyCredits, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dailyDebits" -> Comparator.comparing(AccountBalanceHistory::getDailyDebits, Comparator.nullsLast(Comparator.naturalOrder()));
            case "netDailyMovement" -> Comparator.comparing(AccountBalanceHistory::getNetDailyMovement, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountBalanceHistory> toPage(List<AccountBalanceHistory> list, Pageable pageable) {
        List<AccountBalanceHistory> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountBalanceHistoryResponse> toPageResponse(Page<AccountBalanceHistory> page) {
        List<AccountBalanceHistoryResponse> content = page.getContent().stream()
                .map(AccountBalanceHistoryResponse::from)
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
