package com.github.animetrity.onlinepbx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.animetrity.onlinepbx.models.PbxUserResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OnlinePbxClient {
    private final String domain;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Зберігаємо сесійні ключі
    private String sessionKeyId = null;
    private String sessionKey = null;

    public OnlinePbxClient(String domain, String apiKey) {
        this.domain = domain;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // Метод для отримання або оновлення сесійних ключів
    private void authenticate() throws Exception {
        String url = "https://api.onlinepbx.ru/" + this.domain + "/auth.json";
        String body = "auth_key=" + this.apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode rootNode = objectMapper.readTree(response.body());

        if ("1".equals(rootNode.path("status").asText())) {
            this.sessionKeyId = rootNode.path("data").path("key_id").asText();
            this.sessionKey = rootNode.path("data").path("key").asText();
        } else {
            throw new RuntimeException("Помилка авторизації в OnlinePBX: " + response.body());
        }
    }

    // Внутрішній метод для виконання самого HTTP-запиту
    private String executeRequest(String endpoint, String body) throws Exception {
        String url = "https://api.onlinepbx.ru/" + this.domain + "/" + endpoint;
        String authHeader = this.sessionKeyId + ":" + this.sessionKey; // Формат key_id:key

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("x-pbx-authentication", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Універсальний метод, який викликає запит і сам обробляє помилки авторизації
    private <T> T sendPostRequest(String endpoint, String body, Class<T> responseType) throws Exception {
        // Якщо це перший запит і ключів ще немає - отримуємо їх
        if (this.sessionKeyId == null || this.sessionKey == null) {
            authenticate();
        }

        String responseBody = executeRequest(endpoint, body);
        JsonNode rootNode = objectMapper.readTree(responseBody);

        // Якщо ключі протермінувались - оновлюємо і повторюємо запит
        if (rootNode.path("isNotAuth").asBoolean()) {
            authenticate();
            responseBody = executeRequest(endpoint, body);
        }

        return objectMapper.readValue(responseBody, responseType);
    }

    // Метод для отримання всіх користувачів
    public PbxUserResponse getUsers() throws Exception {
        return sendPostRequest("user/get.json", "", PbxUserResponse.class);
    }
}