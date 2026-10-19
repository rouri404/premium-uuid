package com.coto.premiumuuid.mojang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Mojang API HTTP client. Handles fast timeouts and rate-limiting (HTTP 429) gracefully.
 */
public final class MojangApiClient {

    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    // Mojang account names: 1-16 chars of [A-Za-z0-9_]. Anything else cannot be premium.
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    static final long RATE_LIMIT_BACKOFF_MS = 60_000L;
    static final long FAILURE_BACKOFF_MS = 30_000L;

    private final HttpClient httpClient;
    private final Logger logger;

    // Fail fast instead of re-hitting Mojang while it is rate-limiting or failing for a name
    private volatile long rateLimitedUntil;
    private final ConcurrentHashMap<String, Long> failedUntil = new ConcurrentHashMap<>();

    public MojangApiClient(Logger logger) {
        this(logger, HttpClient.newHttpClient());
    }

    MojangApiClient(Logger logger, HttpClient httpClient) {
        this.logger = logger;
        this.httpClient = httpClient;
    }

    public sealed interface LookupResult permits Success, Failure {}

    public record Success(UUID uuid, String correctName, boolean premium) implements LookupResult {}

    public record Failure(String reason) implements LookupResult {}

    public static boolean isValidUsername(String username) {
        return username != null && VALID_NAME.matcher(username).matches();
    }

    public LookupResult lookup(String username, int timeoutMs, boolean debug) {
        if (!isValidUsername(username)) { // not a valid Mojang name → can never be premium
            return new Success(computeOfflineUUID(username), username, false);
        }

        long now = System.currentTimeMillis();
        if (now < rateLimitedUntil) {
            return new Failure("Rate-limited (backing off for " + (rateLimitedUntil - now) + "ms)");
        }
        String key = username.toLowerCase();
        Long blockedUntil = failedUntil.get(key);
        if (blockedUntil != null) {
            if (now < blockedUntil) {
                return new Failure("Recent lookup failure (backing off for " + (blockedUntil - now) + "ms)");
            }
            failedUntil.remove(key, blockedUntil);
        }

        LookupResult result = doLookup(username, timeoutMs, debug);
        if (result instanceof Failure) {
            failedUntil.put(key, System.currentTimeMillis() + FAILURE_BACKOFF_MS);
        }
        return result;
    }

    private LookupResult doLookup(String username, int timeoutMs, boolean debug) {
        URI uri = URI.create(API_URL + username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .GET()
                .build();

        long start = System.currentTimeMillis();
        CompletableFuture<HttpResponse<String>> future = null;
        try {
            future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            // Bounds the whole exchange (connect + headers + body), not just the headers
            HttpResponse<String> response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
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
                rateLimitedUntil = System.currentTimeMillis() + retryAfterMs(response);
                return new Failure("Rate-limited (HTTP 429)");
            }

            return new Failure("Unexpected HTTP status " + status);

        } catch (TimeoutException e) {
            future.cancel(true); // only thrown by future.get, so future is set
            long elapsed = System.currentTimeMillis() - start;
            if (debug) {
                logger.info("[DEBUG] Mojang API GET " + uri + " → TIMEOUT after " + elapsed + "ms");
            }
            return new Failure("Timeout after " + elapsed + "ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof java.net.http.HttpTimeoutException) {
                return new Failure("Timeout after " + (System.currentTimeMillis() - start) + "ms");
            }
            return new Failure("Network error: " + cause.getMessage());
        } catch (InterruptedException e) {
            future.cancel(true); // only thrown by future.get, so future is set
            Thread.currentThread().interrupt();
            return new Failure("Interrupted");
        } catch (RuntimeException e) { // never let an unexpected error escape into the login thread
            return new Failure("Unexpected error: " + e);
        }
    }

    private static long retryAfterMs(HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After")
                .map(v -> {
                    try {
                        return Math.max(1L, Long.parseLong(v.trim())) * 1000L;
                    } catch (NumberFormatException e) {
                        return RATE_LIMIT_BACKOFF_MS;
                    }
                })
                .orElse(RATE_LIMIT_BACKOFF_MS);
    }

    static LookupResult parsePremiumResponse(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) {
                return new Failure("Malformed response: not a JSON object");
            }
            JsonObject json = root.getAsJsonObject();
            JsonElement id = json.get("id");
            JsonElement name = json.get("name");
            if (id == null || !id.isJsonPrimitive() || name == null || !name.isJsonPrimitive()) {
                return new Failure("Malformed response: missing 'id' or 'name'");
            }
            return new Success(fromUndashed(id.getAsString()), name.getAsString(), true);
        } catch (RuntimeException e) { // JsonSyntaxException, bad UUID, ...
            return new Failure("Malformed response: " + e.getMessage());
        }
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
