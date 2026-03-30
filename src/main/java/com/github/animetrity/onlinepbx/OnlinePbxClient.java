package com.github.animetrity.onlinepbx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.animetrity.onlinepbx.models.PbxUserResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.github.animetrity.onlinepbx.models.PbxCallHistoryResponse;

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


    // Отримання дзвінків за датою (максимальний інтервал в OnlinePBX - 1 тиждень)
    public PbxCallHistoryResponse getCallsByDate(LocalDate fromDate, LocalDate toDate) throws Exception {
        long fromStamp = fromDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        // Беремо початок наступного дня і віднімаємо 1 секунду, щоб отримати 23:59:59 поточного
        long toStamp = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;

        String body = "start_stamp_from=" + fromStamp + "&start_stamp_to=" + toStamp;
        return sendPostRequest("mongo_history/search.json", body, PbxCallHistoryResponse.class);
    }

    // Пошук дзвінків за конкретним номером (вхідні та вихідні)
    public PbxCallHistoryResponse getCallsByNumber(String phoneNumber, LocalDate fromDate, LocalDate toDate) throws Exception {
        long fromStamp = fromDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        // Беремо початок наступного дня і віднімаємо 1 секунду, щоб отримати 23:59:59 поточного
        long toStamp = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;

        String encodedNumber = URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8).replace("+", "%2B");
        String body = "sub_phone_numbers=" + encodedNumber + "&start_stamp_from=" + fromStamp + "&start_stamp_to=" + toStamp;
        return sendPostRequest("mongo_history/search.json", body, PbxCallHistoryResponse.class);
    }


    // Внутрішній метод для виконання самого HTTP-запиту
    private String executeRequest(String endpoint, String body) throws Exception {
        String url = "https://api.onlinepbx.ru/" + this.domain + "/" + endpoint;
        String authHeader = this.sessionKeyId + ":" + this.sessionKey; // Формат key_id:key
        System.out.println(authHeader);
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