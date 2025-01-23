package com.camunda.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;

import java.util.Map;

public class CallbackLambda implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        // Exemplo: extrair o TaskToken do input
        String taskToken = (String) input.get("TaskToken");

        if (taskToken == null) {
            context.getLogger().log("Nenhum TaskToken encontrado no input!");
            return "Nenhum token encontrado!";
        }

        // Aqui você pode fazer alguma lógica de negócio.
        // Exemplo simples: decidir se queremos mandar sucesso ou falha.
        boolean tarefaBemSucedida = true; // Troque de acordo com sua lógica

        try (SfnClient sfnClient = SfnClient.builder()
                .region(Region.SA_EAST_1)
                .build()) {

            if (tarefaBemSucedida) {
                // Envia mensagem de sucesso para a Step Function
                sfnClient.sendTaskSuccess(SendTaskSuccessRequest.builder()
                        .taskToken(taskToken)
                        .output("{\"status\":\"OK\"}") // JSON com o resultado que irá para a Step Function
                        .build());
                context.getLogger().log("SendTaskSuccess chamado com sucesso!");
            } else {
                // Envia mensagem de falha para a Step Function
                sfnClient.sendTaskFailure(SendTaskFailureRequest.builder()
                        .taskToken(taskToken)
                        .error("ErroExemplo")
                        .cause("Algum motivo de falha")
                        .build());
                context.getLogger().log("SendTaskFailure chamado!");
            }
        } catch (Exception e) {
            context.getLogger().log("Erro no callback: " + e.getMessage());
        }

        return "Processamento finalizado!";
    }
}