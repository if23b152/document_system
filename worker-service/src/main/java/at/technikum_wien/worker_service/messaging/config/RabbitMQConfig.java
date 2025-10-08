package at.technikum_wien.worker_service.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ in the worker service.
 * Defines the necessary Queue, Exchange, and Binding.
 * NOTE: This configuration must match the producer's setup exactly.
 */
@Configuration
public class RabbitMQConfig {

    // Naming constants for easy reference and management
    public static final String QUEUE_NAME = "ocr-queue";
    public static final String EXCHANGE_NAME = "dms-exchange";
    public static final String ROUTING_KEY = "ocr.process"; // Key the queue is bound to

    /**
     * Defines the Queue bean.
     * The worker registers this queue to listen for incoming messages.
     */
    @Bean
    public Queue ocrQueue() {
        return new Queue(QUEUE_NAME, true, false, false);
    }

    /**
     * Defines the Topic Exchange bean.
     */
    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Binds the Queue to the Exchange using the Routing Key.
     */
    @Bean
    public Binding binding(Queue ocrQueue, TopicExchange dmsExchange) {
        return BindingBuilder
                .bind(ocrQueue)
                .to(dmsExchange)
                .with(ROUTING_KEY);
    }
}
