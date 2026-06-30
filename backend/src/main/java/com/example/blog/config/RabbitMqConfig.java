package com.example.blog.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String REVIEW_QUEUE = "review.notification.queue";
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_ROUTING_KEY = "review.notification";

    @Bean
    public Queue reviewNotificationQueue() {
        return QueueBuilder.durable(REVIEW_QUEUE)
                .deadLetterExchange("review.dlx.exchange")
                .deadLetterRoutingKey("review.dlx.routing")
                .build();
    }

    @Bean
    public DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE);
    }

    @Bean
    public Binding reviewBinding(Queue reviewNotificationQueue, DirectExchange reviewExchange) {
        return BindingBuilder.bind(reviewNotificationQueue).to(reviewExchange).with(REVIEW_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && cause != null) {
                System.err.println("RabbitMQ message not confirmed: " + cause);
            }
        });
        template.setReturnsCallback(returned -> {
            System.err.println("RabbitMQ message returned: " + returned.getMessage()
                    + ", replyCode: " + returned.getReplyCode()
                    + ", replyText: " + returned.getReplyText());
        });
        template.setMandatory(true);
        return template;
    }
}
