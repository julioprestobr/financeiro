package com.prestobr.financeiro.service;

import com.prestobr.financeiro.domain.entity.ProductStock;
import com.prestobr.financeiro.dto.request.ProductStockPageRequest;
import com.prestobr.financeiro.dto.response.PageResponse;
import com.prestobr.financeiro.dto.response.Pagination;
import com.prestobr.financeiro.dto.response.ProductStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private static final String TABLE = "estoque_produto";

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "stockQuantity", "CAST(qtd_estoque AS NUMERIC)",
            "availableQuantity", "CAST(qtd_estoque_disponivel AS NUMERIC)",
            "stockValue", "valor_estoque",
            "cost", "CAST(custo AS NUMERIC)",
            "salePrice", "CAST(preco_venda AS NUMERIC)",
            "createdAt", "data_cadastro",
            "updatedAt", "data_alteracao",
            "inventoryDate", "data_inventario"
    );

    private static final RowMapper<ProductStock> ROW_MAPPER = (rs, rowNum) -> ProductStock.builder()
            .companyCode(rs.getString("cod_empresa"))
            .companyName(rs.getString("nome_empresa"))
            .productCode(rs.getString("cod_produto"))
            .productName(rs.getString("nome_produto"))
            .shortName(rs.getString("nome_resumido"))
            .reference(rs.getString("referencia"))
            .barcode(rs.getString("cod_barras"))
            .groupCode(rs.getString("cod_grupo"))
            .groupName(rs.getString("nome_grupo"))
            .subgroupCode(rs.getString("cod_subgrupo"))
            .subgroupName(rs.getString("nome_subgrupo"))
            .brand(rs.getString("marca"))
            .unit(rs.getString("unidade_produto"))
            .fiscalClassification(rs.getString("classificacao_fiscal"))
            .productType(rs.getString("tipo_produto"))
            .status(rs.getString("situacao"))
            .location(rs.getString("localizacao_produto"))
            .shelf(rs.getString("prateleira"))
            .vendorCode(rs.getString("cod_fornecedor"))
            .vendorName(rs.getString("nome_fornecedor"))
            .vendorTradeName(rs.getString("fantasia_fornecedor"))
            .stockQuantity(getBigDecimalFromText(rs, "qtd_estoque"))
            .blockedQuantity(getBigDecimalFromText(rs, "qtd_estoque_bloqueado"))
            .incomingQuantity(getBigDecimalFromText(rs, "qtd_estoque_entrada"))
            .outgoingQuantity(getBigDecimalFromText(rs, "qtd_estoque_saida"))
            .pendingQuantity(getBigDecimalFromText(rs, "qtd_estoque_pendente"))
            .reservedQuantity(getBigDecimalFromText(rs, "qtd_estoque_reservado"))
            .availableQuantity(getBigDecimalFromText(rs, "qtd_estoque_disponivel"))
            .maxStock(getBigDecimalFromText(rs, "produto_estoque_maximo"))
            .minStock(getBigDecimalFromText(rs, "produto_estoque_minimo"))
            .safetyStock(getBigDecimalFromText(rs, "produto_estoque_seguranca"))
            .belowMinimum(getBoolean(rs, "is_abaixo_minimo"))
            .aboveMaximum(getBoolean(rs, "is_acima_maximo"))
            .cost(getBigDecimalFromText(rs, "custo"))
            .averageCost(getBigDecimalFromText(rs, "custo_medio"))
            .purchaseCost(getBigDecimalFromText(rs, "custo_compra"))
            .averagePurchaseCost(getBigDecimalFromText(rs, "custo_compra_medio"))
            .salePrice(getBigDecimalFromText(rs, "preco_venda"))
            .minSalePrice(getBigDecimalFromText(rs, "preco_venda_minimo"))
            .markup(getBigDecimalFromText(rs, "markup"))
            .stockValue(rs.getBigDecimal("valor_estoque"))
            .inventoryDate(getLocalDateTime(rs, "data_inventario"))
            .createdAt(getLocalDateTime(rs, "data_cadastro"))
            .updatedAt(getLocalDateTime(rs, "data_alteracao"))
            .createdBy(rs.getString("operador_cadastro"))
            .updatedBy(rs.getString("operador_alteracao"))
            .snapshotDatetime(getLocalDateTime(rs, "snapshot_datetime"))
            .build();

    private final JdbcTemplate dataLakeJdbcTemplate;

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    public PageResponse<ProductStockResponse> search(ProductStockPageRequest request) {
        WhereClause where = buildWhereClause(request);
        long total = countTotal(where);

        String sql = "SELECT * FROM " + TABLE
                + where.sql()
                + buildOrderBy(request.sort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>(where.params());
        params.add(request.size());
        params.add(request.page() * request.size());

        List<ProductStockResponse> content = dataLakeJdbcTemplate.query(sql, ROW_MAPPER, params.toArray())
                .stream()
                .map(ProductStockResponse::from)
                .toList();

        int totalPages = (int) Math.ceil((double) total / request.size());
        return new PageResponse<>(new Pagination(request.page(), request.size(), total, totalPages), content);
    }

    public List<ProductStockResponse> getByCodigoProduto(String codigoProduto) {
        String sql = "SELECT * FROM " + TABLE + " WHERE cod_produto = ?";

        return dataLakeJdbcTemplate.query(sql, ROW_MAPPER, codigoProduto)
                .stream()
                .map(ProductStockResponse::from)
                .toList();
    }

    // =========================================================================
    // FILTROS (WHERE dinâmico)
    // =========================================================================

    private WhereClause buildWhereClause(ProductStockPageRequest request) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (request.companyCode() != null) {
            conditions.add("cod_empresa = ?");
            params.add(request.companyCode());
        }
        if (request.productCode() != null) {
            conditions.add("cod_produto = ?");
            params.add(request.productCode());
        }
        if (request.barcode() != null) {
            conditions.add("cod_barras = ?");
            params.add(request.barcode());
        }
        if (request.vendorCode() != null) {
            conditions.add("cod_fornecedor = ?");
            params.add(request.vendorCode());
        }
        if (request.groupCode() != null) {
            conditions.add("CAST(cod_grupo AS TEXT) = ?");
            params.add(request.groupCode());
        }
        if (request.subgroupCode() != null) {
            conditions.add("CAST(cod_subgrupo AS TEXT) = ?");
            params.add(request.subgroupCode());
        }
        if (request.brand() != null) {
            conditions.add("marca = ?");
            params.add(request.brand());
        }
        if (request.status() != null) {
            conditions.add("situacao = ?");
            params.add(request.status());
        }
        if (request.productName() != null) {
            conditions.add("nome_produto ILIKE ?");
            params.add("%" + request.productName() + "%");
        }
        if (request.belowMinimum() != null) {
            conditions.add("is_abaixo_minimo = ?");
            params.add(request.belowMinimum());
        }
        if (request.aboveMaximum() != null) {
            conditions.add("is_acima_maximo = ?");
            params.add(request.aboveMaximum());
        }

        String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new WhereClause(sql, params);
    }

    private long countTotal(WhereClause where) {
        String sql = "SELECT count(*) FROM " + TABLE + where.sql();
        Long total = dataLakeJdbcTemplate.queryForObject(sql, Long.class, where.params().toArray());
        return total == null ? 0 : total;
    }

    // =========================================================================
    // ORDENAÇÃO
    // =========================================================================

    private String buildOrderBy(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return "";
        }

        List<String> orders = new ArrayList<>();
        for (String s : sort) {
            String[] parts = s.split(",");
            String column = SORTABLE_COLUMNS.get(parts[0]);
            if (column == null) {
                continue;
            }
            String direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]) ? "DESC" : "ASC";
            orders.add(column + " " + direction);
        }

        return orders.isEmpty() ? "" : " ORDER BY " + String.join(", ", orders);
    }

    // =========================================================================
    // MAPEAMENTO RESULTSET -> ENTITY
    // =========================================================================

    private static BigDecimal getBigDecimalFromText(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record WhereClause(String sql, List<Object> params) {}
}
