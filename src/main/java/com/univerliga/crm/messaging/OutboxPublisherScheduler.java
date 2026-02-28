package com.univerliga.crm.messaging;

import com.univerliga.crm.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherScheduler {

    private final OutboxService outboxService;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler-delay-ms:3000}")
    public void publishPending() {
        outboxService.processPending();
    }
}
