package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.DataLakeClient;
import com.prestobr.financeiro.domain.entity.AccountPayable;
import com.prestobr.financeiro.domain.util.AccountPayableAnonymizer;
import com.prestobr.financeiro.domain.util.QueryFilter;
import com.prestobr.financeiro.domain.util.QueryParser;
import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
import com.prestobr.financeiro.dto.response.AccountPayableResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import com.prestobr.financeiro.dto.response.QueryResponse;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.prestobr.financeiro.util.ParquetUtils.*;

@Slf4j
@Service
public class AccountPayableService {

    private final DataLakeClient dataLakeClient;
    private final ApplicationContext applicationContext;

    @Value("${financeiro.account-payable.anonymize-data:false}")
    private boolean anonymizeData;

    @Value("${datalake.gold-account-payable-base-prefix}")
    private String goldAccountPayableBasePrefix;

    public AccountPayableService(DataLakeClient dataLakeClient, ApplicationContext applicationContext) {
        this.dataLakeClient = dataLakeClient;
        this.applicationContext = applicationContext;
    }

    private AccountPayableService self() {
        return applicationContext.getBean(AccountPayableService.class);
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountPayableResponse> search(AccountPayablePageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountPayable> filtered = self().loadAll().stream()
                .filter(ap -> matchesFilters(ap, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    public AccountPayableResponse getByCodigoTitulo(String codigoTitulo) {
        return self().loadAll().stream()
                .filter(ap -> codigoTitulo.equals(ap.getTitleCode()))
                .findFirst()
                .map(AccountPayableResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Título não encontrado: " + codigoTitulo
                ));
    }

    @CacheEvict(value = "accounts-payable", allEntries = true)
    public void clearCache() {
        log.info("Cache de contas a pagar limpo");
    }

    // =========================================================================
    // CARREGAMENTO DE DADOS
    // =========================================================================

    @Cacheable("accounts-payable")
    public List<AccountPayable> loadAll() {
        List<String> latestRunKeys = dataLakeClient.findLatestRunParquetKeysFromPrefix(goldAccountPayableBasePrefix);

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Gold");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente (Gold)", latestRunKeys.size());

        List<AccountPayable> all = new ArrayList<>();
        for (String key : latestRunKeys) {
            all.addAll(readParquetFile(key));
        }

        return all;
    }

    private List<AccountPayable> readParquetFile(String s3Key) {
        List<AccountPayable> accounts = new ArrayList<>();
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
                    accounts.add(mapToEntity(record));
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

    // =========================================================================
    // MAPEAMENTO PARQUET -> ENTITY
    // =========================================================================

    private AccountPayable mapToEntity(GenericRecord record) {
        Object raw = record.get("valor_titulo");

        System.out.println("VALOR_TITULO RAW: " + raw);
        System.out.println("TIPO: " + (raw != null ? raw.getClass() : "null"));

        AccountPayable original = AccountPayable.builder()
                // Identificação
                .titleCode(getString(record, "codigo_titulo"))
                .purchaseCode(getString(record, "codigo_compra"))

                // Empresa
                .companyCode(getString(record, "cod_empresa"))
                .companyName(getString(record, "nome_empresa"))

                // Fornecedor
                .vendorCode(getString(record, "cod_fornecedor"))
                .vendorName(getString(record, "nome_fornecedor"))
                .vendorTradeName(getString(record, "fantasia_fornecedor"))
                .vendorCnpj(getString(record, "cnpj_fornecedor"))
                .vendorCpf(getString(record, "cpf_fornecedor"))

                // Transportador
                .carrierCode(getString(record, "carrierCode"))
                .carrierName(getString(record, "nome_transportador"))
                .carrierTradeName(getString(record, "fantasia_transportador"))
                .carrierCnpj(getString(record, "cnpj_transportador"))
                .carrierCpf(getString(record, "cpf_transportador"))

                // Prestador
                .providerCode(getString(record, "providerCode"))
                .providerName(getString(record, "nome_prestador"))
                .providerTradeName(getString(record, "fantasia_prestador"))
                .providerCnpj(getString(record, "cnpj_prestador"))
                .providerCpf(getString(record, "cpf_prestador"))

                // Status
                .paymentStatus(getString(record, "status_pagamento"))
                .statusName(getString(record, "nome_status"))

                // Tipo Documento
                .documentType(getString(record, "tipo_documento"))
                .documentTypeName(getString(record, "nome_tipo_documento"))

                // Centro de Custo
                .costCenterCode(getString(record, "cod_centro_custo"))
                .costCenterName(getString(record, "nome_centro_custo"))

                // Subcentro de Custo
                .subCostCenterCode(getString(record, "cod_subcentro_custo"))
                .subCostCenterName(getString(record, "nome_subcentro_custo"))

                // Plano de Conta
                .accountPlan(getString(record, "plano_conta"))
                .accountPlanName(getString(record, "nome_plano_conta"))

                // Setor e Contrato
                .departmentCode(getString(record, "cod_setor"))
                .contract(getString(record, "contract"))

                // Datas
                .emissionDate(getLocalDateTime(record, "data_emissao"))
                .dueDate(getLocalDateTime(record, "data_vencimento"))
                .entryDate(getLocalDateTime(record, "data_entrada"))
                .createdAt(getLocalDateTime(record, "data_cadastro"))
                .updatedAt(getLocalDateTime(record, "data_alteracao"))

                // Valores
                .titleValue(getBigDecimal(record, "valor_titulo"))
                .paidValue(getBigDecimal(record, "valor_pago"))
                .balanceValue(getBigDecimal(record, "valor_saldo"))
                .grossValue(getBigDecimal(record, "valor_bruto"))
                .discountValue(getBigDecimal(record, "valor_desconto"))
                .surchargeValue(getBigDecimal(record, "valor_acrescimo"))
                .movementValue(getBigDecimal(record, "valor_movimento"))
                .otherValues(getBigDecimal(record, "valor_outras"))
                .monetaryCorrection(getBigDecimal(record, "atualizacao_monetaria"))

                // Flags
                .isFullyPaid(getBoolean(record, "is_pago_total"))
                .isProvision(getBoolean(record, "is_provisao"))

                // Classificação
                .titleStatus(getString(record, "situacao_titulo"))
                .titleType(getString(record, "tipo_titulo"))
                .operation(getString(record, "operation"))
                .paymentMethod(getString(record, "forma_pagamento"))
                .paymentOption(getString(record, "opcao_pagamento"))

                // Parcela / Competência
                .installmentNumber(getString(record, "numero_parcela"))
                .referenceMonth(getString(record, "mes_competencia"))
                .period(getString(record, "period"))
                .assessmentPeriod(getString(record, "periodo_apuracao"))
                .referencePeriod(getString(record, "periodo_referencia"))
                .calculationYear(getInteger(record, "ano_calculo"))
                .daysOverdue(getInteger(record, "dias_atraso"))

                // Texto / Histórico
                .description(getString(record, "description"))
                .notes(getString(record, "notes"))

                // Fiscal
                .taxpayerDocument(getString(record, "documento_contribuinte"))
                .stateRegistration(getString(record, "inscricao_estadual"))
                .cityCode(getString(record, "cod_municipio"))
                .state(getString(record, "state"))

                // Auditoria
                .paymentCounter(getInteger(record, "contador_pagamento"))
                .createdBy(getString(record, "operador_cadastro"))
                .updatedBy(getString(record, "operador_alteracao"))

                // Metadados
                .snapshotDatetime(getLocalDateTime(record, "snapshot_datetime"))
                .build();

        return anonymizeData
                ? AccountPayableAnonymizer.anonymize(original)
                : original;
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private boolean matchesFilters(AccountPayable ap, AccountPayablePageRequest request) {
        if ("PENDING".equalsIgnoreCase(request.paymentStatus()) && Boolean.TRUE.equals(ap.getIsFullyPaid())) {
            return false;
        }
        if ("PAID".equalsIgnoreCase(request.paymentStatus()) && !Boolean.TRUE.equals(ap.getIsFullyPaid())) {
            return false;
        }

        if (request.titleCode() != null && !request.titleCode().equals(ap.getTitleCode())) {
            return false;
        }

        if (request.purchaseCode() != null && !request.purchaseCode().equals(ap.getPurchaseCode())){
            return false;
        }

        if (request.vendorCode() != null && !request.vendorCode().equals(ap.getVendorCode())) {
            return false;
        }

        if (request.providerCode() != null && !request.providerCode().equals(ap.getProviderCode())) {
            return false;
        }

        if (request.installmentNumber() != null && !request.installmentNumber().equals(ap.getInstallmentNumber())) {
            return false;
        }

        if (request.costCenterCode() != null && !request.costCenterCode().equals(ap.getCostCenterCode())) {
            return false;
        }

        if (request.subCostCenterCode() != null && !request.subCostCenterCode().equals(ap.getSubCostCenterCode())) {
            return false;
        }

        if (request.departmentCode() != null && !request.departmentCode().equals(ap.getDepartmentCode())) {
            return false;
        }

        if (request.accountPlan() != null && !request.accountPlan().equals(ap.getAccountPlan())) {
            return false;
        }

        if (request.description() != null && ap.getDescription() != null
                && !ap.getDescription().toLowerCase().contains(request.description().toLowerCase())) {
            return false;
        }

        if (request.documentType() != null && !request.documentType().equals(ap.getDocumentType())) {
            return false;
        }

        if (request.titleType() != null && !request.titleType().equals(ap.getTitleType())) {
            return false;
        }

        if (request.operation() != null && !request.operation().equals(ap.getOperation())) {
            return false;
        }

        if (request.paymentMethod() != null && !request.paymentMethod().equals(ap.getPaymentMethod())) {
            return false;
        }

        if (request.paymentOption() != null && !request.paymentOption().equals(ap.getPaymentOption())) {
            return false;
        }

        if (request.emissionDateFrom() != null && ap.getEmissionDate() != null) {
            if (ap.getEmissionDate().toLocalDate().isBefore(request.emissionDateFrom())) {
                return false;
            }
        }

        if (request.emissionDateTo() != null && ap.getEmissionDate() != null) {
            if (ap.getEmissionDate().toLocalDate().isAfter(request.emissionDateTo())) {
                return false;
            }
        }

        if (request.dueDateFrom() != null && ap.getDueDate() != null) {
            if (ap.getDueDate().toLocalDate().isBefore(request.dueDateFrom())) {
                return false;
            }
        }

        if (request.dueDateTo() != null && ap.getDueDate() != null) {
            if (ap.getDueDate().toLocalDate().isAfter(request.dueDateTo())) {
                return false;
            }
        }

        return true;
    }

    // =========================================================================
    // PAGINAÇÃO E ORDENAÇÃO
    // =========================================================================

    private Pageable buildPageable(AccountPayablePageRequest request) {
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
            case "emissionDate" -> Comparator.comparing(AccountPayable::getEmissionDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dueDate" -> Comparator.comparing(AccountPayable::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "entryDate" -> Comparator.comparing(AccountPayable::getEntryDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdAt" -> Comparator.comparing(AccountPayable::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(AccountPayable::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "titleValue" -> Comparator.comparing(AccountPayable::getTitleValue, Comparator.nullsLast(Comparator.naturalOrder()));
            case "installmentNumber" -> Comparator.comparing(AccountPayable::getInstallmentNumber, Comparator.nullsLast(Comparator.naturalOrder()));
            case "daysOverdue" -> Comparator.comparing(AccountPayable::getDaysOverdue, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountPayable> toPage(List<AccountPayable> list, Pageable pageable) {
        List<AccountPayable> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountPayableResponse> toPageResponse(Page<AccountPayable> page) {
        List<AccountPayableResponse> content = page.getContent().stream()
                .map(AccountPayableResponse::from)
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

    public QueryResponse executeQuery(String query) {
        long startTime = System.currentTimeMillis();

        List<AccountPayable> allData = self().loadAll();

        // Parser simples da query
        QueryParser parsed = parseQuery(query);

        // Aplica filtros
        List<AccountPayable> filtered = allData.stream()
                .filter(ap -> matchesQueryFilters(ap, parsed))
                .toList();

        // Aplica agregação ou seleção
        List<Map<String, Object>> result;
        List<String> columns;

        if (parsed.hasAggregation()) {
            result = executeAggregation(filtered, parsed);
            columns = parsed.getSelectColumns();
        } else {
            result = selectColumns(filtered, parsed);
            columns = parsed.getSelectColumns();
        }

        // Aplica limit
        if (parsed.getLimit() > 0 && result.size() > parsed.getLimit()) {
            result = result.subList(0, parsed.getLimit());
        }

        long executionTime = System.currentTimeMillis() - startTime;

        return QueryResponse.builder()
                .columns(columns)
                .data(result)
                .totalRecords(result.size())
                .executedQuery(query)
                .executionTimeMs(executionTime)
                .build();
    }

    private QueryParser parseQuery(String query) {
        return new QueryParser(query);
    }

    private boolean matchesQueryFilters(AccountPayable ap, QueryParser parsed) {
        for (QueryFilter filter : parsed.getFilters()) {
            Object value = getFieldValue(ap, filter.getField());
            if (!filter.matches(value)) {
                return false;
            }
        }
        return true;
    }

    private List<Map<String, Object>> executeAggregation(List<AccountPayable> data, QueryParser parsed) {
        String groupByField = parsed.getGroupByColumn();

        // Se tem GROUP BY, agrupa os dados
        if (groupByField != null && !groupByField.isEmpty()) {
            return executeGroupedAggregation(data, parsed, groupByField);
        }

        // Sem GROUP BY, retorna agregação única (comportamento atual)
        Map<String, Object> result = new HashMap<>();

        for (String col : parsed.getSelectColumns()) {
            if (col.toUpperCase().startsWith("SUM(")) {
                String field = col.substring(4, col.length() - 1);
                BigDecimal sum = data.stream()
                        .map(ap -> getBigDecimalField(ap, field))
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                result.put(col, sum);
            } else if (col.toUpperCase().startsWith("COUNT(")) {
                result.put(col, data.size());
            } else if (col.toUpperCase().startsWith("AVG(")) {
                String field = col.substring(4, col.length() - 1);
                BigDecimal sum = data.stream()
                        .map(ap -> getBigDecimalField(ap, field))
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = data.isEmpty() ? BigDecimal.ZERO :
                        sum.divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP);
                result.put(col, avg);
            } else if (col.toUpperCase().startsWith("MIN(")) {
                String field = col.substring(4, col.length() - 1);
                BigDecimal min = data.stream()
                        .map(ap -> getBigDecimalField(ap, field))
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(null);
                result.put(col, min);
            } else if (col.toUpperCase().startsWith("MAX(")) {
                String field = col.substring(4, col.length() - 1);
                BigDecimal max = data.stream()
                        .map(ap -> getBigDecimalField(ap, field))
                        .filter(Objects::nonNull)
                        .max(BigDecimal::compareTo)
                        .orElse(null);
                result.put(col, max);
            }
        }

        return List.of(result);
    }

    private List<Map<String, Object>> executeGroupedAggregation(List<AccountPayable> data, QueryParser parsed, String groupByField) {
        // Agrupa os dados pelo campo
        Map<Object, List<AccountPayable>> grouped = data.stream()
                .collect(Collectors.groupingBy(ap -> {
                    Object value = getFieldValue(ap, groupByField.toLowerCase());
                    return value != null ? value : "NULL";
                }));

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map.Entry<Object, List<AccountPayable>> entry : grouped.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            List<AccountPayable> groupData = entry.getValue();

            // Adiciona o campo do GROUP BY
            row.put(groupByField, entry.getKey());

            // Calcula agregações para cada grupo
            for (String col : parsed.getSelectColumns()) {
                String colUpper = col.toUpperCase();

                if (colUpper.startsWith("SUM(")) {
                    String field = col.substring(4, col.length() - 1);
                    BigDecimal sum = groupData.stream()
                            .map(ap -> getBigDecimalField(ap, field))
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    row.put(col, sum);
                } else if (colUpper.startsWith("COUNT(")) {
                    row.put(col, groupData.size());
                } else if (colUpper.startsWith("AVG(")) {
                    String field = col.substring(4, col.length() - 1);
                    BigDecimal sum = groupData.stream()
                            .map(ap -> getBigDecimalField(ap, field))
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = groupData.isEmpty() ? BigDecimal.ZERO :
                            sum.divide(BigDecimal.valueOf(groupData.size()), 2, RoundingMode.HALF_UP);
                    row.put(col, avg);
                } else if (colUpper.startsWith("MIN(")) {
                    String field = col.substring(4, col.length() - 1);
                    BigDecimal min = groupData.stream()
                            .map(ap -> getBigDecimalField(ap, field))
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(null);
                    row.put(col, min);
                } else if (colUpper.startsWith("MAX(")) {
                    String field = col.substring(4, col.length() - 1);
                    BigDecimal max = groupData.stream()
                            .map(ap -> getBigDecimalField(ap, field))
                            .filter(Objects::nonNull)
                            .max(BigDecimal::compareTo)
                            .orElse(null);
                    row.put(col, max);
                }
            }

            results.add(row);
        }

        // Ordena pelo campo do GROUP BY
        results.sort((a, b) -> {
            Object valA = a.get(groupByField);
            Object valB = b.get(groupByField);
            if (valA instanceof Comparable && valB instanceof Comparable) {
                return ((Comparable) valA).compareTo(valB);
            }
            return String.valueOf(valA).compareTo(String.valueOf(valB));
        });

        return results;
    }

    private List<Map<String, Object>> selectColumns(List<AccountPayable> data, QueryParser parsed) {
        List<String> columns = parsed.getSelectColumns();
        boolean selectAll = columns.contains("*");

        return data.stream()
                .map(ap -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    if (selectAll) {
                        row.put("titleCode", ap.getTitleCode());
                        row.put("vendorName", ap.getVendorName());
                        row.put("titleValue", ap.getTitleValue());
                        row.put("balanceValue", ap.getBalanceValue());
                        row.put("dueDate", ap.getDueDate());
                        row.put("isFullyPaid", ap.getIsFullyPaid());
                    } else {
                        for (String col : columns) {
                            row.put(col, getFieldValue(ap, col));
                        }
                    }
                    return row;
                })
                .toList();
    }

    private Object getFieldValue(AccountPayable ap, String field) {
        return switch (field.toLowerCase()) {
            case "codigotitulo" -> ap.getTitleCode();
            case "codigocompra" -> ap.getPurchaseCode();
            case "codempresa" -> ap.getCompanyCode();
            case "nomeempresa" -> ap.getCompanyName();
            case "codfornecedor" -> ap.getVendorCode();
            case "nomefornecedor" -> ap.getVendorName();
            case "cnpjfornecedor" -> ap.getVendorCnpj();
            case "cpffornecedor" -> ap.getVendorCpf();
            case "codcentrocusto" -> ap.getCostCenterCode();
            case "nomecentrocusto" -> ap.getCostCenterName();
            case "planoconta" -> ap.getAccountPlan();
            case "nomeplanoconta" -> ap.getAccountPlanName();
            case "statuspagamento" -> ap.getPaymentStatus();
            case "dataemissao" -> ap.getEmissionDate();
            case "datavencimento" -> ap.getDueDate();
            case "valortitulo" -> ap.getTitleValue();
            case "valorpago" -> ap.getPaidValue();
            case "valorsaldo" -> ap.getBalanceValue();
            case "valorbruto" -> ap.getGrossValue();
            case "valordesconto" -> ap.getDiscountValue();
            case "valoracrescimo" -> ap.getSurchargeValue();
            case "ispagototal" -> ap.getIsFullyPaid();
            case "isprovisao" -> ap.getIsProvision();
            case "tipotitulo" -> ap.getTitleType();
            case "operation" -> ap.getOperation();
            case "formapagamento" -> ap.getPaymentMethod();
            case "numeroparcela" -> ap.getInstallmentNumber();
            case "diasatraso" -> ap.getDaysOverdue();
            case "description" -> ap.getDescription();
            case "state" -> ap.getState();
            default -> null;
        };
    }

    private BigDecimal getBigDecimalField(AccountPayable ap, String field) {
        return switch (field.toLowerCase()) {
            case "valortitulo" -> ap.getTitleValue();
            case "valorpago" -> ap.getPaidValue();
            case "valorsaldo" -> ap.getBalanceValue();
            case "valorbruto" -> ap.getGrossValue();
            case "valordesconto" -> ap.getDiscountValue();
            case "valoracrescimo" -> ap.getSurchargeValue();
            case "valormovimento" -> ap.getMovementValue();
            case "valoroutras" -> ap.getOtherValues();
            default -> null;
        };
    }
}