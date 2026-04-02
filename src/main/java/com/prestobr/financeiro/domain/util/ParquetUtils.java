package com.prestobr.financeiro.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.parquet.io.api.Binary;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class ParquetUtils {

    private ParquetUtils() {}

    public static String getString(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getInteger(GenericRecord record, String field) {
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

    public static Long getLong(GenericRecord record, String field) {
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

    public static BigDecimal getBigDecimal(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;

            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }

            if (value instanceof ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return new BigDecimal(new java.math.BigInteger(bytes), 2); // ⚠️ scale pode variar
            }

            if (value instanceof Binary binary) {
                byte[] bytes = binary.getBytes();
                return new BigDecimal(new java.math.BigInteger(bytes), 2);
            }

            if (value instanceof Utf8 utf8) {
                return new BigDecimal(utf8.toString());
            }

            return new BigDecimal(value.toString());

        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean getBoolean(GenericRecord record, String field) {
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

    public static LocalDateTime getLocalDateTime(GenericRecord record, String field) {
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
}