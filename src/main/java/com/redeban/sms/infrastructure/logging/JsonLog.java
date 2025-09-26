package com.redeban.sms.infrastructure.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JsonLog {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonLog() {}

    public static String toJson(Map<String, ?> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"json-serialization-failed\"}";
        }
    }

    public static String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
