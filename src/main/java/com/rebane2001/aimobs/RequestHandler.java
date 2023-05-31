package com.rebane2001.aimobs;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class RequestHandler {
    private static class OpenAIRequest {
        String model;
        String stop = "\"";
        String prompt;
        float temperature;
        Integer max_tokens;

        OpenAIRequest(String prompt, String model, float temperature, Integer max_tokens) {
            this.prompt = prompt;
            this.model = model;
            this.temperature = temperature;
            this.max_tokens = max_tokens;
        }
    }

    private static class OpenAIResponse {
        static class Choice {
            String text;
        }
        Choice[] choices;
    }

    public static String getAIResponse(String prompt, Integer maxTokens, boolean isSummary) throws IOException {
        if (prompt.length() > 4096) 
            prompt = ConversationHistoryManager.truncateToTokenLimit(prompt,4096);

        AIMobsMod.LOGGER.info("Prompt: " + prompt);

        String endpoint = "https://api.openai.com/v1/completions";
        if (isSummary) 
            endpoint = "https://api.openai.com/v1/completions";

        if (maxTokens == null) {
            maxTokens = 4096; // Set to the maximum allowed by your model
        }

        OpenAIRequest openAIRequest = new OpenAIRequest(prompt, AIMobsConfig.config.model, AIMobsConfig.config.temperature, maxTokens);
        String data = new Gson().toJson(openAIRequest);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(endpoint);
            StringEntity params = new StringEntity(data, "UTF-8");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", "Bearer " + AIMobsConfig.config.apiKey);
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            OpenAIResponse res = new Gson().fromJson(responseString, OpenAIResponse.class);

            // Combine all choices
            StringBuilder combinedChoices = new StringBuilder();
            for (OpenAIResponse.Choice choice : res.choices) {
                combinedChoices.append(choice.text.replace("\n", " "));
            }
            return combinedChoices.toString();
        }
    }
}
