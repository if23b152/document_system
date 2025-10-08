package at.technikum_wien.rest_server.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ.
 * Defines the Queue, Exchange, and the Binding between them.
 * This ensures the messaging infrastructure is set up when the producer service starts.
 */
@Configuration
public class RabbitMQConfig {

    // Naming constants for easy reference and management
    public static final String QUEUE_NAME = "ocr-queue";
    public static final String EXCHANGE_NAME = "dms-exchange";
    public static final String ROUTING_KEY = "ocr.process"; // Key used by producer to route message

    /**
     * Defines the Queue bean.
     * The Queue is durable (persists across broker restarts) and non-exclusive.
     */
    @Bean
    public Queue ocrQueue() {
        // Queue(name, durable, exclusive, autoDelete)
        return new Queue(QUEUE_NAME, true, false, false);
    }

    /**
     * Defines the Topic Exchange bean.
     * Topic exchanges allow for flexible routing based on the ROUTING_KEY pattern.
     */
    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Binds the Queue to the Exchange using the Routing Key.
     * This tells the RabbitMQ broker: "Route any message sent to 'dms-exchange'
     * with a routing key of 'ocr.process' into the 'ocr-queue'."
     */
    @Bean
    public Binding binding(Queue ocrQueue, TopicExchange dmsExchange) {
        return BindingBuilder
                .bind(ocrQueue)
                .to(dmsExchange)
                .with(ROUTING_KEY);
    }
}
