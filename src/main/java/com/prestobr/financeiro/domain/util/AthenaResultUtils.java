package com.prestobr.financeiro.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class AthenaResultUtils {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");

    private AthenaResultUtils() {}

    public static String getString(Map<String, String> row, String field) {
        return row.get(field);
    }

    public static Integer getInteger(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long getLong(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal getBigDecimal(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean getBoolean(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) return null;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    public static LocalDateTime getLocalDateTime(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, TIMESTAMP_FORMAT);
        } catch (Exception e) {
            log.trace("Erro ao converter campo {} para LocalDateTime: {}", field, e.getMessage());
            return null;
        }
    }

    public static LocalDate getLocalDate(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.trace("Erro ao converter campo {} para LocalDate: {}", field, e.getMessage());
            return null;
        }
    }
}
