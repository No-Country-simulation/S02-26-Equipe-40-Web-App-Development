package com.nocountry.conversionflow.conversionflow_api.config.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitRetryDlqConfig {

    @Bean
    DirectExchange authEventsExchange(@Value("${async.auth.exchange:auth.events}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue userRegisteredRetryQueue(
            @Value("${async.auth.user-registered-retry-queue:user.registered.v1.retry}") String retryQueue,
            @Value("${async.auth.exchange:auth.events}") String exchange,
            @Value("${async.auth.user-registered-routing-key:user.registered.v1}") String mainRoutingKey,
            @Value("${async.auth.retry-delay-ms:30000}") int retryDelayMs
    ) {
        return new Queue(retryQueue, true, false, false, Map.of(
                "x-dead-letter-exchange", exchange,
                "x-dead-letter-routing-key", mainRoutingKey,
                "x-message-ttl", retryDelayMs
        ));
    }

    @Bean
    Queue userRegisteredDlq(
            @Value("${async.auth.user-registered-dlq:auth.user.registered.v1.dlq}") String dlqQueue
    ) {
        return new Queue(dlqQueue, true);
    }

    @Bean
    Binding bindMainQueue(
            Queue userRegisteredQueue,
            DirectExchange authEventsExchange,
            @Value("${async.auth.user-registered-routing-key:user.registered.v1}") String mainRoutingKey
    ) {
        return BindingBuilder.bind(userRegisteredQueue).to(authEventsExchange).with(mainRoutingKey);
    }

    @Bean
    Binding bindRetryQueue(
            Queue userRegisteredRetryQueue,
            DirectExchange authEventsExchange,
            @Value("${async.auth.user-registered-retry-queue:user.registered.v1.retry}") String retryRoutingKey
    ) {
        return BindingBuilder.bind(userRegisteredRetryQueue).to(authEventsExchange).with(retryRoutingKey);
    }

    @Bean
    Binding bindDlqQueue(
            Queue userRegisteredDlq,
            DirectExchange authEventsExchange,
            @Value("${async.auth.user-registered-dlq:auth.user.registered.v1.dlq}") String dlqRoutingKey
    ) {
        return BindingBuilder.bind(userRegisteredDlq).to(authEventsExchange).with(dlqRoutingKey);
    }
}
