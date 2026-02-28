package com.univerliga.crm.service;

import com.univerliga.crm.config.OutboxProperties;
import com.univerliga.crm.error.ApiExceptionFactory;
import com.univerliga.crm.model.OutboxEventEntity;
import com.univerliga.crm.model.OutboxStatus;
import com.univerliga.crm.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxProperties outboxProperties;

    @Transactional
    public void enqueue(String aggregateType, String aggregateId, String type, String routingKey, Object payloadObject) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId("evt_" + UUID.randomUUID());
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setType(type);
        event.setRoutingKey(routingKey);
        event.setStatus(OutboxStatus.NEW);
        event.setAttempts(0);
        try {
            event.setPayloadJson(objectMapper.writeValueAsString(Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "type", type,
                    "occurredAt", OffsetDateTime.now(),
                    "source", "crm-service",
                    "payload", payloadObject
            )));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event payload", e);
        }
        outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<OutboxEventEntity> listByStatus(OutboxStatus status, Pageable pageable) {
        if (status == null) {
            return outboxEventRepository.findAll(pageable);
        }
        return outboxEventRepository.findAllByStatus(status, pageable);
    }

    @Transactional
    public void replay(String eventId) {
        OutboxEventEntity event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> ApiExceptionFactory.notFound("Outbox event not found: " + eventId));
        event.setStatus(OutboxStatus.NEW);
        event.setNextAttemptAt(OffsetDateTime.now());
        event.setLastError(null);
        outboxEventRepository.save(event);
    }

    @Transactional
    public void processPending() {
        List<OutboxEventEntity> batch = outboxEventRepository.findForProcessing(
                List.of(OutboxStatus.NEW, OutboxStatus.FAILED),
                OffsetDateTime.now(),
                Pageable.ofSize(outboxProperties.batchSize()));

        for (OutboxEventEntity event : batch) {
            publishSingle(event);
        }
    }

    private void publishSingle(OutboxEventEntity event) {
        try {
            rabbitTemplate.convertAndSend(outboxProperties.exchange(), event.getRoutingKey(), event.getPayloadJson());
            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(OffsetDateTime.now());
            event.setLastError(null);
            event.setNextAttemptAt(null);
            log.info("Outbox event sent id={} routingKey={}", event.getId(), event.getRoutingKey());
        } catch (Exception ex) {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setStatus(OutboxStatus.FAILED);
            event.setLastError(cut(ex.getMessage()));
            event.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSeconds(attempts)));
            if (attempts >= outboxProperties.maxAttempts()) {
                log.error("Outbox event permanently failed id={} attempts={}", event.getId(), attempts, ex);
            } else {
                log.warn("Outbox event publish failed id={} attempt={}", event.getId(), attempts, ex);
            }
        }
        outboxEventRepository.save(event);
    }

    private long backoffSeconds(int attempts) {
        long base = Math.max(1, outboxProperties.baseBackoffSeconds());
        long power = Math.min(attempts, 6);
        return base * (1L << power);
    }

    private String cut(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1900 ? message.substring(0, 1900) : message;
    }
}
