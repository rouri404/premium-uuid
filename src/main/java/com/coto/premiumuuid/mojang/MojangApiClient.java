package com.coto.premiumuuid.mojang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Mojang API HTTP client. Handles fast timeouts and rate-limiting (HTTP 429) gracefully.
 */
public final class MojangApiClient {

    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    private final HttpClient httpClient;
    private final Logger logger;

    public MojangApiClient(Logger logger) {
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public sealed interface LookupResult permits Success, Failure {}

    public record Success(UUID uuid, String correctName, boolean premium) implements LookupResult {}

    public record Failure(String reason) implements LookupResult {}

    public LookupResult lookup(String username, int timeoutMs, boolean debug) {
        URI uri = URI.create(API_URL + username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .GET()
                .build();

        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - start;
            int status = response.statusCode();

            if (debug) {
                logger.info("[DEBUG] Mojang API GET " + uri + " → HTTP " + status + " (" + elapsed + "ms)");
            }

            if (status == 200) {
                String body = response.body();
                if (body == null || body.isBlank()) {
                    return new Failure("HTTP 200 but empty body");
                }
                return parsePremiumResponse(body);
            }

            if (status == 204 || status == 404) { // username not found → not premium
                UUID offlineUuid = computeOfflineUUID(username);
                return new Success(offlineUuid, username, false);
            }

            if (status == 429) {
                return new Failure("Rate-limited (HTTP 429)");
            }

            return new Failure("Unexpected HTTP status " + status);

        } catch (java.net.http.HttpTimeoutException e) {
            long elapsed = System.currentTimeMillis() - start;
            if (debug) {
                logger.info("[DEBUG] Mojang API GET " + uri + " → TIMEOUT after " + elapsed + "ms");
            }
            return new Failure("Timeout after " + elapsed + "ms");
        } catch (java.io.IOException e) {
            return new Failure("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Failure("Interrupted");
        }
    }

    private Success parsePremiumResponse(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String rawId = json.get("id").getAsString();
        String correctName = json.get("name").getAsString();
        UUID uuid = fromUndashed(rawId);
        return new Success(uuid, correctName, true);
    }

    static UUID fromUndashed(String id) {
        if (id.length() != 32) {
            throw new IllegalArgumentException("Invalid undashed UUID length: " + id);
        }
        String dashed = id.substring(0, 8) + "-"
                + id.substring(8, 12) + "-"
                + id.substring(12, 16) + "-"
                + id.substring(16, 20) + "-"
                + id.substring(20);
        return UUID.fromString(dashed);
    }

    public static UUID computeOfflineUUID(String username) { // same algorithm as Minecraft server
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
