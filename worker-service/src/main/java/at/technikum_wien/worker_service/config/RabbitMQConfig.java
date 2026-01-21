package at.technikum_wien.worker_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the worker service.
 * This class defines:
 * - The queue where OCR jobs are received from the REST server
 * - The queue where results are sent back
 * - The exchange that connects everything
 * - How messages are converted to/from JSON
 */
@Configuration // Marks this class as a Spring configuration class
public class RabbitMQConfig {

    // Queue where the worker receives OCR jobs from the REST server
    public static final String QUEUE_NAME = "ocr-queue";

    // Central exchange used to send messages between services
    public static final String EXCHANGE_NAME = "dms-exchange";

    // Routing key used when sending OCR jobs to the worker
    public static final String ROUTING_KEY = "ocr.process";

    // Queue where the worker sends results back to the REST server
    public static final String RESULT_QUEUE = "result-queue";

    // Routing key used when sending results back
    public static final String RESULT_ROUTING_KEY = "result.process";

    /**
     * Creates the queue where OCR requests are received.
     * "Durable" means the queue is not lost when RabbitMQ restarts.
     */
    @Bean
    public Queue ocrQueue() {
        return new Queue(QUEUE_NAME, true, false, false);
    }

    /**
     * Creates the queue where the worker sends results back.
     */
    @Bean
    public Queue resultQueue() {
        return new Queue(RESULT_QUEUE, true, false, false);
    }

    /**
     * Creates the central exchange.
     * All messages are sent to this exchange first, and RabbitMQ
     * decides which queue they go to.
     */
    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Connects the OCR queue to the exchange.
     * This makes sure messages sent with "ocr.process" go into the OCR queue.
     */
    @Bean
    public Binding ocrBinding(Queue ocrQueue, TopicExchange dmsExchange) {
        return BindingBuilder.bind(ocrQueue)
                .to(dmsExchange)
                .with(ROUTING_KEY);
    }

    /**
     * Connects the result queue to the exchange.
     * This makes sure messages sent with "result.process" go into the result queue.
     */
    @Bean
    public Binding resultBinding(Queue resultQueue, TopicExchange dmsExchange) {
        return BindingBuilder.bind(resultQueue)
                .to(dmsExchange)
                .with(RESULT_ROUTING_KEY);
    }

    /**
     * Automatically converts Java objects to JSON and back.
     * This allows us to send DTO objects directly via RabbitMQ.
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates the RabbitTemplate used to send messages.
     * We attach the JSON converter so objects are sent automatically as JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter()); // Use JSON for messages
        return template;
    }
}