package com.nocountry.conversionflow.conversionflow_api.config.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitConsumerConfig {

    @Bean
    Queue userRegisteredQueue(@Value("${async.auth.user-registered-queue:auth.user.registered.v1}") String queueName) {
        return new Queue(queueName, true);
    }
}
