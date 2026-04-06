package com.sporty.bettask.integration.support;

import tools.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class IntegrationTestSupport {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int port;

    public IntegrationTestSupport(ObjectMapper objectMapper, int port) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.port = port;
    }

    public String url(String path) {
        return "http://localhost:" + port + path;
    }

    public HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public HttpEntity<String> jsonEntity(Object body) {
        try {
            return new HttpEntity<>(objectMapper.writeValueAsString(body), jsonHeaders());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize request body for test", exception);
        }
    }

    public HttpEntity<String> rawJsonEntity(String body) {
        return new HttpEntity<>(body, jsonHeaders());
    }

    public ResponseEntity<String> postJson(String path, Object body) {
        return exchange("POST", path, jsonEntity(body));
    }

    public ResponseEntity<String> postRawJson(String path, String body) {
        return exchange("POST", path, rawJsonEntity(body));
    }

    public ResponseEntity<String> get(String path) {
        return exchange("GET", path, null);
    }

    public void awaitAsserted(ThrowingAssertion assertion) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(assertion::run);
    }

    @FunctionalInterface
    public interface ThrowingAssertion {
        void run() throws Exception;
    }

    private ResponseEntity<String> exchange(String method, String path, HttpEntity<String> entity) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url(path)));

            if (entity != null && entity.getHeaders().getContentType() != null) {
                requestBuilder.header(HttpHeaders.CONTENT_TYPE, entity.getHeaders().getContentType().toString());
            }

            switch (method) {
                case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(entity != null ? entity.getBody() : ""));
                case "GET" -> requestBuilder.GET();
                default -> throw new IllegalArgumentException("Unsupported method: " + method);
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            HttpHeaders headers = new HttpHeaders();
            response.headers().map().forEach((name, values) -> headers.put(name, java.util.List.copyOf(values)));
            return ResponseEntity.status(response.statusCode()).headers(headers).body(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing integration test HTTP request", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to execute integration test HTTP request", exception);
        }
    }
}
