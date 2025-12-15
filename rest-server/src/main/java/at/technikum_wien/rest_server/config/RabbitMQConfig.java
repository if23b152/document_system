package at.technikum_wien.rest_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Configuration class for RabbitMQ.
 * Defines the Queue, Exchange, and the Binding between them.
 * This ensures the messaging infrastructure is set up when the producer service starts.
 */
@Configuration
public class RabbitMQConfig {

    // --- Existing OCR inbound queue for worker requests ---
    public static final String OCR_QUEUE = "ocr-queue";
    public static final String EXCHANGE_NAME = "dms-exchange";
    public static final String OCR_ROUTING_KEY = "ocr.process";

    // --- NEW: Queue for results returning from the worker ---
    public static final String RESULT_QUEUE = "result-queue";
    public static final String RESULT_ROUTING_KEY = "result.process";

    // ------------------------------
    //  OCR Request Queue (existing)
    // ------------------------------
    @Bean
    public Queue ocrQueue() {
        return new Queue(OCR_QUEUE, true, false, false);
    }

    // ------------------------------
    //  NEW: Worker Result Queue
    // ------------------------------
    @Bean
    public Queue resultQueue() {
        return new Queue(RESULT_QUEUE, true, false, false);
    }

    // ------------------------------
    // Topic Exchange (shared)
    // ------------------------------
    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // ------------------------------
    // Binding: OCR requests
    // ------------------------------
    @Bean
    public Binding ocrBinding(Queue ocrQueue, TopicExchange dmsExchange) {
        return BindingBuilder
                .bind(ocrQueue)
                .to(dmsExchange)
                .with(OCR_ROUTING_KEY);
    }

    // ------------------------------
    // NEW Binding: Worker results
    // ------------------------------
    @Bean
    public Binding resultBinding(Queue resultQueue, TopicExchange dmsExchange) {
        return BindingBuilder
                .bind(resultQueue)
                .to(dmsExchange)
                .with(RESULT_ROUTING_KEY);
    }

    // ------------------------------
    // JSON converter for messages
    // ------------------------------
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ------------------------------
    // RabbitTemplate with JSON support
    // ------------------------------
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}