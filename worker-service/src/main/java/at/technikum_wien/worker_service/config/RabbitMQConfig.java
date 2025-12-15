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

@Configuration // Marks this class as a Spring configuration class
public class RabbitMQConfig {

    // Name of the RabbitMQ queue that will hold OCR requests
    public static final String QUEUE_NAME = "ocr-queue";

    // Name of the exchange used for routing messages
    public static final String EXCHANGE_NAME = "dms-exchange";

    // Routing key used to match messages to the OCR queue
    public static final String ROUTING_KEY = "ocr.process";

    // --- Result outbound queue ---
    public static final String RESULT_QUEUE = "result-queue";
    public static final String RESULT_ROUTING_KEY = "result.process";

    /**
     * Creates a durable queue for OCR messages.
     * Durable = true means the queue survives RabbitMQ restarts.
     * Non-exclusive and non-auto-delete: can be shared and won’t be deleted automatically.
     */
    @Bean
    public Queue ocrQueue() {
        return new Queue(QUEUE_NAME, true, false, false);
    }

    /**
     * NEW: Result queue for sending summary + OCR text back to REST server.
     */
    @Bean
    public Queue resultQueue() {
        return new Queue(RESULT_QUEUE, true, false, false);
    }

    /**
     * Creates a TopicExchange.
     * Exchanges route messages to queues based on routing keys.
     * A topic exchange allows pattern matching with routing keys.
     */
    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Binds the OCR queue to the exchange using the routing key.
     * This ensures messages sent with "ocr.process" reach the OCR queue.
     */
    @Bean
    public Binding ocrBinding(Queue ocrQueue, TopicExchange dmsExchange) {
        return BindingBuilder.bind(ocrQueue).to(dmsExchange).with(ROUTING_KEY);
    }

    /**
     * NEW: Binding for result queue.
     */
    @Bean
    public Binding resultBinding(Queue resultQueue, TopicExchange dmsExchange) {
        return BindingBuilder.bind(resultQueue)
                .to(dmsExchange)
                .with(RESULT_ROUTING_KEY);
    }

    // --- JSON converter for DTO messages ---
    /**
     * Converts messages to/from JSON automatically.
     * This allows sending/receiving Java objects instead of raw byte arrays.
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures the RabbitTemplate used to send messages.
     * Uses the JSON message converter so objects are serialized automatically.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter()); // set JSON converter
        return template;
    }
}
