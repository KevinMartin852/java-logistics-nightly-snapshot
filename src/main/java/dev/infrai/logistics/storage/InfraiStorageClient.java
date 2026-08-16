package dev.infrai.logistics.storage;

import dev.infrai.logistics.config.SnapshotProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiStorageClient {
    private static final int MAX_ATTEMPTS = 4;
    private final SnapshotProperties properties;
    private final HttpClient http;

    public InfraiStorageClient(SnapshotProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(properties.requestTimeout()).build();
    }

    // Capability idiom: infrai.storage.bucket.create
    public void createBucket(String name) throws IOException, InterruptedException {
        call("POST", "/v1/storage/bucket/create", Map.of("name", name));
    }

    // Capability idiom: infrai.storage.object.put
    public void putSnapshot(String bucket, String key, byte[] content, String idempotencyKey)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data_base64", Base64.getEncoder().encodeToString(content));
        body.put("content_type", "application/json");
        body.put("idempotency_key", idempotencyKey);
        call("PUT", "/v1/storage/object/put/" + segment(bucket) + "/" + pathKey(key), body);
    }

    private Map<String, Object> call(String method, String path, Map<String, Object> body)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(properties.baseUri().resolve(path))
                    .timeout(properties.requestTimeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JsonCodec.write(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = JsonCodec.readObject(response.body());

            if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                throw InfraiException.from(response.statusCode(), envelope.get("error"));
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Storage transport response " + response.statusCode());
            }
            Object data = envelope.get("data");
            if (data instanceof Map<?, ?> raw) {
                Map<String, Object> result = new LinkedHashMap<>();
                raw.forEach((key, value) -> result.put(key.toString(), value));
                return result;
            }
            return Map.of();
        }
        throw new IOException("Retry attempts exhausted");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            return Duration.ofMillis(250L * (1L << attempt));
        }
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String pathKey(String key) {
        return key.lines().findFirst().orElseThrow().replace("\\", "/").chars()
                .mapToObj(c -> c == '/' ? "/" : segment(Character.toString((char) c)))
                .reduce("", String::concat);
    }
}
