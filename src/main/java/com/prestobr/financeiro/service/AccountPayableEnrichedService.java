package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.DataLakeClient;
import com.prestobr.financeiro.domain.entity.AccountPayableEnriched;
import com.prestobr.financeiro.domain.util.AccountPayableAnonymizer;
import com.prestobr.financeiro.domain.util.AccountPayableEnrichedAnonymizer;
import com.prestobr.financeiro.domain.util.QueryFilter;
import com.prestobr.financeiro.domain.util.QueryParser;
import com.prestobr.financeiro.dto.request.AccountPayablePageRequest;
import com.prestobr.financeiro.dto.response.AccountPayableEnrichedResponse;
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
public class AccountPayableEnrichedService {

    private final DataLakeClient dataLakeClient;
    private final ApplicationContext applicationContext;

    @Value("${financeiro.account-payable.anonymize-data:false}")
    private boolean anonymizeData;

    public AccountPayableEnrichedService(DataLakeClient dataLakeClient, ApplicationContext applicationContext) {
        this.dataLakeClient = dataLakeClient;
        this.applicationContext = applicationContext;
    }

    private AccountPayableEnrichedService self() {
        return applicationContext.getBean(AccountPayableEnrichedService.class);
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountPayableEnrichedResponse> search(AccountPayablePageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountPayableEnriched> filtered = self().loadAll().stream()
                .filter(ap -> matchesFilters(ap, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    public AccountPayableEnrichedResponse getByCodigoTitulo(String codigoTitulo) {
        return self().loadAll().stream()
                .filter(ap -> codigoTitulo.equals(ap.getCodigoTitulo()))
                .findFirst()
                .map(AccountPayableEnrichedResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Título não encontrado: " + codigoTitulo
                ));
    }

    @CacheEvict(value = "accounts-payable-enriched", allEntries = true)
    public void clearCache() {
        log.info("Cache de contas a pagar enriquecidas limpo");
    }

    // =========================================================================
    // CARREGAMENTO DE DADOS
    // =========================================================================

    @Cacheable("accounts-payable-enriched")
    public List<AccountPayableEnriched> loadAll() {
        List<String> latestRunKeys = dataLakeClient.findLatestRunEnrichedParquetKeys();

        if (latestRunKeys.isEmpty()) {
            log.warn("Nenhum arquivo Parquet encontrado no Data Lake Gold");
            return Collections.emptyList();
        }

        log.info("Encontrados {} arquivos Parquet na run mais recente (Gold)", latestRunKeys.size());

        List<AccountPayableEnriched> all = new ArrayList<>();
        for (String key : latestRunKeys) {
            all.addAll(readParquetFile(key));
        }

        return all;
    }

    private List<AccountPayableEnriched> readParquetFile(String s3Key) {
        List<AccountPayableEnriched> accounts = new ArrayList<>();
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

    private AccountPayableEnriched mapToEntity(GenericRecord record) {
        AccountPayableEnriched original = AccountPayableEnriched.builder()
                // Identificação
                .codigoTitulo(getString(record, "codigo_titulo"))
                .codigoCompra(getString(record, "codigo_compra"))

                // Empresa
                .codEmpresa(getString(record, "cod_empresa"))
                .nomeEmpresa(getString(record, "nome_empresa"))

                // Fornecedor
                .codFornecedor(getString(record, "cod_fornecedor"))
                .nomeFornecedor(getString(record, "nome_fornecedor"))
                .fantasiaFornecedor(getString(record, "fantasia_fornecedor"))
                .cnpjFornecedor(getString(record, "cnpj_fornecedor"))
                .cpfFornecedor(getString(record, "cpf_fornecedor"))

                // Transportador
                .transportador(getString(record, "transportador"))
                .nomeTransportador(getString(record, "nome_transportador"))

                // Prestador
                .prestador(getString(record, "prestador"))
                .nomePrestador(getString(record, "nome_prestador"))

                // Status
                .statusPagamento(getString(record, "status_pagamento"))
                .nomeStatus(getString(record, "nome_status"))

                // Tipo Documento
                .tipoDocumento(getString(record, "tipo_documento"))
                .nomeTipoDocumento(getString(record, "nome_tipo_documento"))

                // Centro de Custo
                .codCentroCusto(getString(record, "cod_centro_custo"))
                .nomeCentroCusto(getString(record, "nome_centro_custo"))

                // Subcentro de Custo
                .codSubcentroCusto(getString(record, "cod_subcentro_custo"))
                .nomeSubcentroCusto(getString(record, "nome_subcentro_custo"))

                // Plano de Conta
                .planoConta(getString(record, "plano_conta"))
                .nomePlanoConta(getString(record, "nome_plano_conta"))

                // Setor e Contrato
                .codSetor(getString(record, "cod_setor"))
                .contrato(getString(record, "contrato"))

                // Datas
                .dataEmissao(getLocalDateTime(record, "data_emissao"))
                .dataVencimento(getLocalDateTime(record, "data_vencimento"))
                .dataEntrada(getLocalDateTime(record, "data_entrada"))
                .dataCadastro(getLocalDateTime(record, "data_cadastro"))
                .dataAlteracao(getLocalDateTime(record, "data_alteracao"))

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

                // Flags
                .isPagoTotal(getBoolean(record, "is_pago_total"))
                .isProvisao(getBoolean(record, "is_provisao"))

                // Classificação
                .situacaoTitulo(getString(record, "situacao_titulo"))
                .tipoTitulo(getString(record, "tipo_titulo"))
                .operacao(getString(record, "operacao"))
                .formaPagamento(getString(record, "forma_pagamento"))
                .opcaoPagamento(getString(record, "opcao_pagamento"))

                // Parcela / Competência
                .numeroParcela(getString(record, "numero_parcela"))
                .mesCompetencia(getString(record, "mes_competencia"))
                .periodo(getString(record, "periodo"))
                .periodoApuracao(getString(record, "periodo_apuracao"))
                .periodoReferencia(getString(record, "periodo_referencia"))
                .anoCalculo(getInteger(record, "ano_calculo"))
                .diasAtraso(getInteger(record, "dias_atraso"))

                // Texto / Histórico
                .historico(getString(record, "historico"))
                .observacao(getString(record, "observacao"))

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
                .build();

        return anonymizeData
                ? AccountPayableEnrichedAnonymizer.anonymize(original)
                : original;
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private boolean matchesFilters(AccountPayableEnriched ap, AccountPayablePageRequest request) {
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

    private List<AccountPayableEnriched> applySorting(List<AccountPayableEnriched> list, Sort sort) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<AccountPayableEnriched> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<AccountPayableEnriched> fieldComparator = getComparator(order.getProperty());

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

    private Comparator<AccountPayableEnriched> getComparator(String field) {
        return switch (field) {
            case "dataEmissao" -> Comparator.comparing(AccountPayableEnriched::getDataEmissao, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataVencimento" -> Comparator.comparing(AccountPayableEnriched::getDataVencimento, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataEntrada" -> Comparator.comparing(AccountPayableEnriched::getDataEntrada, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataCadastro" -> Comparator.comparing(AccountPayableEnriched::getDataCadastro, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dataAlteracao" -> Comparator.comparing(AccountPayableEnriched::getDataAlteracao, Comparator.nullsLast(Comparator.naturalOrder()));
            case "valorTitulo" -> Comparator.comparing(AccountPayableEnriched::getValorTitulo, Comparator.nullsLast(Comparator.naturalOrder()));
            case "numeroParcela" -> Comparator.comparing(AccountPayableEnriched::getNumeroParcela, Comparator.nullsLast(Comparator.naturalOrder()));
            case "diasAtraso" -> Comparator.comparing(AccountPayableEnriched::getDiasAtraso, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountPayableEnriched> toPage(List<AccountPayableEnriched> list, Pageable pageable) {
        List<AccountPayableEnriched> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountPayableEnrichedResponse> toPageResponse(Page<AccountPayableEnriched> page) {
        List<AccountPayableEnrichedResponse> content = page.getContent().stream()
                .map(AccountPayableEnrichedResponse::from)
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

        List<AccountPayableEnriched> allData = self().loadAll();

        // Parser simples da query
        QueryParser parsed = parseQuery(query);

        // Aplica filtros
        List<AccountPayableEnriched> filtered = allData.stream()
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

    private boolean matchesQueryFilters(AccountPayableEnriched ap, QueryParser parsed) {
        for (QueryFilter filter : parsed.getFilters()) {
            Object value = getFieldValue(ap, filter.getField());
            if (!filter.matches(value)) {
                return false;
            }
        }
        return true;
    }

    private List<Map<String, Object>> executeAggregation(List<AccountPayableEnriched> data, QueryParser parsed) {
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

    private List<Map<String, Object>> selectColumns(List<AccountPayableEnriched> data, QueryParser parsed) {
        List<String> columns = parsed.getSelectColumns();
        boolean selectAll = columns.contains("*");

        return data.stream()
                .map(ap -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    if (selectAll) {
                        row.put("codigoTitulo", ap.getCodigoTitulo());
                        row.put("nomeFornecedor", ap.getNomeFornecedor());
                        row.put("valorTitulo", ap.getValorTitulo());
                        row.put("valorSaldo", ap.getValorSaldo());
                        row.put("dataVencimento", ap.getDataVencimento());
                        row.put("isPagoTotal", ap.getIsPagoTotal());
                    } else {
                        for (String col : columns) {
                            row.put(col, getFieldValue(ap, col));
                        }
                    }
                    return row;
                })
                .toList();
    }

    private Object getFieldValue(AccountPayableEnriched ap, String field) {
        return switch (field.toLowerCase()) {
            case "codigotitulo" -> ap.getCodigoTitulo();
            case "codigocompra" -> ap.getCodigoCompra();
            case "codempresa" -> ap.getCodEmpresa();
            case "nomeempresa" -> ap.getNomeEmpresa();
            case "codfornecedor" -> ap.getCodFornecedor();
            case "nomefornecedor" -> ap.getNomeFornecedor();
            case "cnpjfornecedor" -> ap.getCnpjFornecedor();
            case "cpffornecedor" -> ap.getCpfFornecedor();
            case "codcentrocusto" -> ap.getCodCentroCusto();
            case "nomecentrocusto" -> ap.getNomeCentroCusto();
            case "planoconta" -> ap.getPlanoConta();
            case "nomeplanoconta" -> ap.getNomePlanoConta();
            case "statuspagamento" -> ap.getStatusPagamento();
            case "dataemissao" -> ap.getDataEmissao();
            case "datavencimento" -> ap.getDataVencimento();
            case "valortitulo" -> ap.getValorTitulo();
            case "valorpago" -> ap.getValorPago();
            case "valorsaldo" -> ap.getValorSaldo();
            case "valorbruto" -> ap.getValorBruto();
            case "valordesconto" -> ap.getValorDesconto();
            case "valoracrescimo" -> ap.getValorAcrescimo();
            case "ispagototal" -> ap.getIsPagoTotal();
            case "isprovisao" -> ap.getIsProvisao();
            case "tipotitulo" -> ap.getTipoTitulo();
            case "operacao" -> ap.getOperacao();
            case "formapagamento" -> ap.getFormaPagamento();
            case "numeroparcela" -> ap.getNumeroParcela();
            case "diasatraso" -> ap.getDiasAtraso();
            case "historico" -> ap.getHistorico();
            case "uf" -> ap.getUf();
            default -> null;
        };
    }

    private BigDecimal getBigDecimalField(AccountPayableEnriched ap, String field) {
        return switch (field.toLowerCase()) {
            case "valortitulo" -> ap.getValorTitulo();
            case "valorpago" -> ap.getValorPago();
            case "valorsaldo" -> ap.getValorSaldo();
            case "valorbruto" -> ap.getValorBruto();
            case "valordesconto" -> ap.getValorDesconto();
            case "valoracrescimo" -> ap.getValorAcrescimo();
            case "valormovimento" -> ap.getValorMovimento();
            case "valoroutras" -> ap.getValorOutras();
            default -> null;
        };
    }
}