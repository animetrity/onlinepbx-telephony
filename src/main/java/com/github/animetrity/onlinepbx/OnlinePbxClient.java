package com.github.animetrity.onlinepbx;

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

    public OnlinePbxClient(String domain, String apiKey) {
        this.domain = domain;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    private <T> T sendPostRequest(String endpoint, String body, Class<T> responseType) throws Exception {
        // Формуємо правильний URL згідно з документацією: https://api.onlinepbx.ru/{domain}/...
        String url = "https://api.onlinepbx.ru/" + this.domain + "/" + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("x-pbx-authentication", this.apiKey) // Передаємо ключ у заголовку
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), responseType);
    }

    // Метод для отримання всіх користувачів
    public PbxUserResponse getUsers() throws Exception {
        return sendPostRequest("user/get.json", "", PbxUserResponse.class);
    }
}