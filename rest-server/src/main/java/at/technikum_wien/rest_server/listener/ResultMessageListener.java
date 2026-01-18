package at.technikum_wien.rest_server.listener;

import at.technikum_wien.rest_server.handler.ResultMessageHandler;
import at.technikum_wien.rest_server.model.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component // Spring-managed component for listening to RabbitMQ messages
public class ResultMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ResultMessageListener.class);

    private final ResultMessageHandler resultMessageHandler; // Handles the actual processing

    // Constructor injection
    public ResultMessageListener(ResultMessageHandler resultMessageHandler) {
        this.resultMessageHandler = resultMessageHandler;
    }

    // Listen to messages coming into the result-queue
    @RabbitListener(queues = "result-queue")
    public void handleWorkerResult(ResultMessage message) {
        // Log receipt of worker result
        log.info("Received worker result for document {}. Success: {}",
                message.getDocumentId(), message.isSuccess());

        // Delegate to handler for business logic
        resultMessageHandler.process(message);
    }
}