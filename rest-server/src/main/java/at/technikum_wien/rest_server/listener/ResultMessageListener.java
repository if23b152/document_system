package at.technikum_wien.rest_server.listener;

import at.technikum_wien.rest_server.handler.ResultMessageHandler;
import at.technikum_wien.rest_server.model.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ResultMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ResultMessageListener.class);

    private final ResultMessageHandler resultMessageHandler;

    public ResultMessageListener(ResultMessageHandler resultMessageHandler) {
        this.resultMessageHandler = resultMessageHandler;
    }

    @RabbitListener(queues = "result-queue")
    public void handleWorkerResult(ResultMessage message) {
        log.info("Received worker result for document {}. Success: {}", message.getDocumentId(), message.isSuccess());
        resultMessageHandler.process(message);
    }
}
