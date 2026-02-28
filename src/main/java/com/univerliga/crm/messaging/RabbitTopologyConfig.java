package com.univerliga.crm.messaging;

import com.univerliga.crm.config.OutboxProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange crmEventsExchange(OutboxProperties props) {
        return new TopicExchange(props.exchange(), true, false);
    }
}
