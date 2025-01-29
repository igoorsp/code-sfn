package com.camunda.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.camunda.lambda.domain.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;

import java.util.Map;

public class CallbackLambda implements RequestHandler<SQSEvent, Message> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Message handleRequest(SQSEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Inputs recebidos: " + input);

        input.getRecords().forEach(message -> processMessage(message, logger));
        return Message.builder()
                .messageField("success").build();
    }

    private void processMessage(SQSEvent.SQSMessage message, LambdaLogger logger) {
        try {
            var body = message.getBody();
            logger.log("Mensagem body: " + body);

            // Converte o body para um mapa
            var bodyMap = OBJECT_MAPPER.readValue(body, Map.class);

            var taskToken = (String) bodyMap.get("TaskToken");
            if (taskToken == null) {
                logger.log("Nenhum TaskToken encontrado na mensagem!");
                return;
            }

            // Executa a lógica de negócio
            boolean tarefaBemSucedida = true; // Simule uma lógica aqui
            handleTaskCallback(taskToken, tarefaBemSucedida, logger);

        } catch (Exception e) {
            logger.log("Erro ao processar a mensagem: " + e.getMessage());
        }
    }

    private void handleTaskCallback(String taskToken, boolean success, LambdaLogger logger) {
        try (var sfnClient = SfnClient.builder().region(Region.SA_EAST_1).build()) {
            if (success) {
                var request = SendTaskSuccessRequest.builder()
                        .taskToken(taskToken)
                        .output("{\"status\":\"OK\"}")
                        .build();
                sfnClient.sendTaskSuccess(request);
                logger.log("SendTaskSuccess chamado com sucesso!");
            } else {
                var request = SendTaskFailureRequest.builder()
                        .taskToken(taskToken)
                        .error("ErroExemplo")
                        .cause("Algum motivo de falha")
                        .build();
                sfnClient.sendTaskFailure(request);
                logger.log("SendTaskFailure chamado!");
            }
        } catch (Exception e) {
            logger.log("Erro ao enviar o callback para a Step Functions: " + e.getMessage());
        }
    }
}
