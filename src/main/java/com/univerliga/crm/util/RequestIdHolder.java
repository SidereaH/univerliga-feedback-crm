package com.univerliga.crm.util;

import java.util.UUID;
import org.slf4j.MDC;

public final class RequestIdHolder {

    private static final String KEY = "requestId";

    private RequestIdHolder() {
    }

    public static String getOrGenerate() {
        String existing = MDC.get(KEY);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        MDC.put(KEY, generated);
        return generated;
    }

    public static void set(String requestId) {
        MDC.put(KEY, requestId);
    }

    public static void clear() {
        MDC.remove(KEY);
    }
}
