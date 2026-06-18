package com.scjc.ndt.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 弹性日期反序列化器 — 容错用户输入的多种日期格式。
 * 支持: yyyy-MM-dd / yyyy.MM.dd / yyyy/MM/dd / yyyyMMdd
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ISO_LOCAL_DATE
    );

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim();
        if (value.isEmpty()) return null;

        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new IllegalArgumentException("无法解析日期: " + value + "，请使用 yyyy-MM-dd 格式");
    }
}
