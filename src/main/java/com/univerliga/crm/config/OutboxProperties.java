package com.univerliga.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        String exchange,
        int maxAttempts,
        int batchSize,
        long schedulerDelayMs,
        int baseBackoffSeconds
) {
}
