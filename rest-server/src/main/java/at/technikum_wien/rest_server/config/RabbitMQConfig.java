package at.technikum_wien.rest_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Configuration class for RabbitMQ.
 * Defines the Queues, Exchange, and the Bindings between them.
 * This ensures the messaging infrastructure is created automatically at startup.
 */
@Configuration // Marks this as a Spring configuration class
public class RabbitMQConfig {

    // --- Queue for sending OCR processing requests to the worker ---
    public static final String OCR_QUEUE = "ocr-queue";

    // --- Shared exchange used by all services ---
    public static final String EXCHANGE_NAME = "dms-exchange";

    // --- Routing key for OCR processing requests ---
    public static final String OCR_ROUTING_KEY = "ocr.process";

    // --- Queue for receiving processing results from workers ---
    public static final String RESULT_QUEUE = "result-queue";

    // --- Routing key for worker result messages ---
    public static final String RESULT_ROUTING_KEY = "result.process";

    // ------------------------------
    // OCR request queue (REST → Worker)
    // ------------------------------
    @Bean
    public Queue ocrQueue() {
        // Durable queue that survives broker restarts
        return new Queue(OCR_QUEUE, true, false, false);
    }

    // ------------------------------
    // Worker result queue (Worker → REST)
    // ------------------------------
    @Bean
    public Queue resultQueue() {
        // Durable queue for results coming back from workers
        return new Queue(RESULT_QUEUE, true, false, false);
    }

    // ------------------------------
    // Topic exchange (shared by all message types)
    // ------------------------------
    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // ------------------------------
    // Binding: OCR request messages
    // ------------------------------
    @Bean
    public Binding ocrBinding(Queue ocrQueue, TopicExchange dmsExchange) {
        return BindingBuilder
                .bind(ocrQueue)              // Bind OCR queue
                .to(dmsExchange)             // To shared exchange
                .with(OCR_ROUTING_KEY);      // Using OCR routing key
    }

    // ------------------------------
    // Binding: Worker result messages
    // ------------------------------
    @Bean
    public Binding resultBinding(Queue resultQueue, TopicExchange dmsExchange) {
        return BindingBuilder
                .bind(resultQueue)           // Bind result queue
                .to(dmsExchange)             // To shared exchange
                .with(RESULT_ROUTING_KEY);   // Using result routing key
    }

    // ------------------------------
    // JSON converter for message serialization
    // ------------------------------
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        // Automatically converts Java objects ↔ JSON in RabbitMQ messages
        return new Jackson2JsonMessageConverter();
    }

    // ------------------------------
    // RabbitTemplate configured with JSON support
    // ------------------------------
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        // Main Spring helper class for sending messages to RabbitMQ
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // Use JSON serialization instead of raw byte arrays
        template.setMessageConverter(jsonMessageConverter());

        return template;
    }
}