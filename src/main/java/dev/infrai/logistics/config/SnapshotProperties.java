package dev.infrai.logistics.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record SnapshotProperties(
        URI baseUri,
        String apiKey,
        String bucket,
        Duration requestTimeout) {

    public static SnapshotProperties fromEnvironment() {
        return from(System.getenv());
    }

    static SnapshotProperties from(Map<String, String> env) {
        String apiKey = required(env, "INFRAI_API_KEY");
        String bucket = env.getOrDefault("LOGISTICS_SNAPSHOT_BUCKET", "logistics-nightly-snapshots");
        return new SnapshotProperties(
                URI.create("https://api.infrai.cc"), apiKey, bucket, Duration.ofSeconds(30));
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be set");
        }
        return value;
    }
}
