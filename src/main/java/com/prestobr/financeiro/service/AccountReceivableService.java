package com.prestobr.financeiro.service;

import com.prestobr.financeiro.client.AthenaQueryClient;
import com.prestobr.financeiro.domain.entity.AccountReceivable;
import com.prestobr.financeiro.domain.util.AccountReceivableAnonymizer;
import com.prestobr.financeiro.dto.request.AccountReceivablePageRequest;
import com.prestobr.financeiro.dto.response.AccountReceivableResponse;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import lombok.extern.slf4j.Slf4j;
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

import java.util.*;
import java.util.stream.Collectors;

import static com.prestobr.financeiro.util.AthenaResultUtils.*;

@Slf4j
@Service
public class AccountReceivableService {

    private final AthenaQueryClient athenaQueryClient;
    private final ApplicationContext applicationContext;

    @Value("${financeiro.account-receivable.anonymize-data:false}")
    private boolean anonymizeData;

    @Value("${athena.table.account-receivable}")
    private String accountReceivableTable;

    public AccountReceivableService(AthenaQueryClient athenaQueryClient, ApplicationContext applicationContext) {
        this.athenaQueryClient = athenaQueryClient;
        this.applicationContext = applicationContext;
    }

    private AccountReceivableService self() {
        return applicationContext.getBean(AccountReceivableService.class);
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<AccountReceivableResponse> search(AccountReceivablePageRequest request) {
        Pageable pageable = buildPageable(request);
        List<AccountReceivable> filtered = self().loadAll().stream()
                .filter(ar -> matchesFilters(ar, request))
                .collect(Collectors.toList());

        return toPageResponse(toPage(filtered, pageable));
    }

    public AccountReceivableResponse getByCodigoTitulo(String codigoTitulo) {
        return self().loadAll().stream()
                .filter(ar -> codigoTitulo.equals(ar.getTitleCode()))
                .findFirst()
                .map(AccountReceivableResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Título não encontrado: " + codigoTitulo
                ));
    }

    @CacheEvict(value = "accounts-receivable", allEntries = true)
    public void clearCache() {
        log.info("Cache de contas a receber limpo");
    }

    // =========================================================================
    // CARREGAMENTO DE DADOS
    // =========================================================================

    @Cacheable("accounts-receivable")
    public List<AccountReceivable> loadAll() {
        List<Map<String, String>> rows = athenaQueryClient.runQuery("SELECT * FROM " + accountReceivableTable);

        if (rows.isEmpty()) {
            log.warn("Nenhum registro encontrado na tabela Athena: {}", accountReceivableTable);
            return Collections.emptyList();
        }

        log.info("Encontrados {} registros via Athena ({})", rows.size(), accountReceivableTable);

        return rows.stream().map(this::mapToEntity).collect(Collectors.toList());
    }

    // =========================================================================
    // MAPEAMENTO ATHENA -> ENTITY
    // =========================================================================

    private AccountReceivable mapToEntity(Map<String, String> record) {
        AccountReceivable original = AccountReceivable.builder()
                // Identificação
                .titleCode(getString(record, "codigo_titulo"))
                .linkCode(getString(record, "codigo_vinculo"))
                .generatedTitleCode(getString(record, "titulo_gerado"))

                // Empresa
                .companyCode(getString(record, "cod_empresa"))
                .companyName(getString(record, "nome_empresa"))

                // Cliente
                .clientCode(getString(record, "cod_cliente"))
                .creditClientCode(getString(record, "cod_cliente_credito"))
                .clientName(getString(record, "nome_cliente"))
                .clientTradeName(getString(record, "fantasia_cliente"))
                .clientCnpj(getString(record, "cnpj_cliente"))
                .clientCpf(getString(record, "cpf_cliente"))

                // Vendedor
                .sellerCode(getString(record, "cod_vendedor"))
                .sellerName(getString(record, "nome_vendedor"))

                // Status
                .receiptStatus(getInteger(record, "status_recebimento"))
                .receiptStatusName(getString(record, "nome_status_recebimento"))

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

                // Contrato
                .contract(getString(record, "contrato"))

                // Datas
                .emissionDate(getLocalDateTime(record, "data_emissao"))
                .dueDate(getLocalDateTime(record, "data_vencimento"))
                .originalDueDate(getLocalDateTime(record, "data_vencimento_original"))
                .confirmationDate(getLocalDateTime(record, "data_confirmacao"))
                .createdAt(getLocalDateTime(record, "data_cadastro"))
                .updatedAt(getLocalDateTime(record, "data_alteracao"))

                // Valores
                .titleValue(getBigDecimal(record, "valor_titulo"))
                .receivedValue(getBigDecimal(record, "valor_recebido"))
                .grossValue(getBigDecimal(record, "valor_bruto"))
                .discountValue(getBigDecimal(record, "valor_desconto"))
                .surchargeValue(getBigDecimal(record, "valor_acrescimo"))
                .movementValue(getBigDecimal(record, "valor_movimento"))
                .benefitDiscountValue(getBigDecimal(record, "valor_desconto_benef"))

                // Flags
                .isProvision(getBoolean(record, "is_provisao"))
                .isProvision2(getBoolean(record, "is_provisao2"))

                // Classificação
                .titleStatus(getString(record, "situacao_titulo"))
                .titleStatusName(getString(record, "nome_situacao_titulo"))
                .titleType(getInteger(record, "tipo_titulo"))

                // Competência
                .referenceMonth(getString(record, "mes_competencia"))
                .daysOverdue(getInteger(record, "dias_atraso"))

                // Texto / Histórico
                .description(getString(record, "historico"))
                .notes(getString(record, "observacao"))

                // Emitente / dados bancários
                .issuerName(getString(record, "nome_emitente"))
                .issuerTradeName(getString(record, "nome_fantasia"))
                .bankAgencyNumber(getString(record, "numero_agencia"))
                .bankAccountNumber(getString(record, "numero_conta"))
                .checkNumber(getString(record, "numero_cheque"))
                .processNumber(getString(record, "numero_processo"))
                .creditCode(getString(record, "codigo_credito"))
                .authorizationCode(getString(record, "codigo_autorizacao"))
                .telemetry(getString(record, "telemetria"))
                .fiscalDocumentNumber(getString(record, "numero_documento_fiscal"))
                .serasa(getString(record, "serasa"))
                .batchNumber(getInteger(record, "lote"))

                // Auditoria
                .createdBy(getString(record, "operador_cadastro"))
                .updatedBy(getString(record, "operador_alteracao"))
                .confirmedBy(getString(record, "operador_confirmacao"))

                // Metadados
                .snapshotDatetime(getLocalDateTime(record, "snapshot_datetime"))
                .build();

        return anonymizeData
                ? AccountReceivableAnonymizer.anonymize(original)
                : original;
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private boolean matchesFilters(AccountReceivable ar, AccountReceivablePageRequest request) {
        if (request.titleCode() != null && !request.titleCode().equals(ar.getTitleCode())) {
            return false;
        }

        if (request.companyCode() != null && !request.companyCode().equals(ar.getCompanyCode())) {
            return false;
        }

        if (request.clientCode() != null && !request.clientCode().equals(ar.getClientCode())) {
            return false;
        }

        if (request.costCenterCode() != null && !request.costCenterCode().equals(ar.getCostCenterCode())) {
            return false;
        }

        if (request.subCostCenterCode() != null && !request.subCostCenterCode().equals(ar.getSubCostCenterCode())) {
            return false;
        }

        if (request.accountPlan() != null && !request.accountPlan().equals(ar.getAccountPlan())) {
            return false;
        }

        if (request.description() != null && ar.getDescription() != null
                && !ar.getDescription().toLowerCase().contains(request.description().toLowerCase())) {
            return false;
        }

        if (request.documentType() != null && !request.documentType().equals(ar.getDocumentType())) {
            return false;
        }

        if (request.emissionDateFrom() != null && ar.getEmissionDate() != null) {
            if (ar.getEmissionDate().toLocalDate().isBefore(request.emissionDateFrom())) {
                return false;
            }
        }

        if (request.emissionDateTo() != null && ar.getEmissionDate() != null) {
            if (ar.getEmissionDate().toLocalDate().isAfter(request.emissionDateTo())) {
                return false;
            }
        }

        if (request.dueDateFrom() != null && ar.getDueDate() != null) {
            if (ar.getDueDate().toLocalDate().isBefore(request.dueDateFrom())) {
                return false;
            }
        }

        if (request.dueDateTo() != null && ar.getDueDate() != null) {
            if (ar.getDueDate().toLocalDate().isAfter(request.dueDateTo())) {
                return false;
            }
        }

        return true;
    }

    // =========================================================================
    // PAGINAÇÃO E ORDENAÇÃO
    // =========================================================================

    private Pageable buildPageable(AccountReceivablePageRequest request) {
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

    private List<AccountReceivable> applySorting(List<AccountReceivable> list, Sort sort) {
        if (sort.isUnsorted()) {
            return list;
        }

        Comparator<AccountReceivable> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<AccountReceivable> fieldComparator = getComparator(order.getProperty());

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

    private Comparator<AccountReceivable> getComparator(String field) {
        return switch (field) {
            case "emissionDate" -> Comparator.comparing(AccountReceivable::getEmissionDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dueDate" -> Comparator.comparing(AccountReceivable::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "originalDueDate" -> Comparator.comparing(AccountReceivable::getOriginalDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdAt" -> Comparator.comparing(AccountReceivable::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(AccountReceivable::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "titleValue" -> Comparator.comparing(AccountReceivable::getTitleValue, Comparator.nullsLast(Comparator.naturalOrder()));
            case "daysOverdue" -> Comparator.comparing(AccountReceivable::getDaysOverdue, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private Page<AccountReceivable> toPage(List<AccountReceivable> list, Pageable pageable) {
        List<AccountReceivable> sorted = applySorting(list, pageable.getSort());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start > sorted.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sorted.size());
        }
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }

    private PageResponse<AccountReceivableResponse> toPageResponse(Page<AccountReceivable> page) {
        List<AccountReceivableResponse> content = page.getContent().stream()
                .map(AccountReceivableResponse::from)
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
