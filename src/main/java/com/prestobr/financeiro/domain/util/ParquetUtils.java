package com.prestobr.financeiro.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.parquet.io.api.Binary;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
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

            // valor numérico simples (ex: campo Parquet do tipo double/float/int/long, sem logical type decimal)
            if (value instanceof Number number && !(value instanceof GenericData.Fixed)) {
                return new BigDecimal(number.toString());
            }

            // decimal com logical type (FIXED ou BYTES) — precisa da escala declarada no schema
            if (value instanceof GenericData.Fixed || value instanceof ByteBuffer) {
                int scale = getDecimalScale(record, field);

                byte[] bytes = value instanceof GenericData.Fixed fixed
                        ? fixed.bytes()
                        : toByteArray((ByteBuffer) value);

                return bytes.length == 0
                        ? BigDecimal.ZERO.setScale(scale)
                        : new BigDecimal(new BigInteger(bytes), scale);
            }

            return new BigDecimal(value.toString());

        } catch (Exception e) {
            return null;
        }
    }

    private static int getDecimalScale(GenericRecord record, String field) {
        Schema.Field schemaField = record.getSchema().getField(field);
        Schema fieldSchema = schemaField.schema();

        // trata union (nullable)
        if (fieldSchema.getType() == Schema.Type.UNION) {
            for (Schema s : fieldSchema.getTypes()) {
                if (s.getType() != Schema.Type.NULL) {
                    fieldSchema = s;
                    break;
                }
            }
        }

        LogicalTypes.Decimal decimalType = (LogicalTypes.Decimal) fieldSchema.getLogicalType();
        return decimalType.getScale();
    }

    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
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

    public static LocalDate getLocalDate(GenericRecord record, String field) {
        try {
            Object value = record.get(field);
            if (value == null) return null;

            if (value instanceof Integer) {
                return LocalDate.ofEpochDay((Integer) value);
            }

            if (value instanceof Long) {
                return LocalDate.ofEpochDay((Long) value);
            }

            if (value instanceof CharSequence) {
                return LocalDate.parse(value.toString());
            }

            return null;
        } catch (Exception e) {
            log.trace("Erro ao converter campo {} para LocalDate: {}", field, e.getMessage());
            return null;
        }
    }
}