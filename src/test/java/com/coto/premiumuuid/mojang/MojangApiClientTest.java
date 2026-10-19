package com.coto.premiumuuid.mojang;

import com.coto.premiumuuid.mojang.MojangApiClient.Failure;
import com.coto.premiumuuid.mojang.MojangApiClient.LookupResult;
import com.coto.premiumuuid.mojang.MojangApiClient.Success;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MojangApiClientTest {

    private final Logger logger = Logger.getLogger("MojangApiClientTest");
    private final HttpClient http = mock(HttpClient.class);
    private final MojangApiClient client = new MojangApiClient(logger, http);

    @SuppressWarnings("unchecked")
    private void respond(int status, String body, Map<String, List<String>> headers) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        lenient().when(response.body()).thenReturn(body);
        lenient().when(response.headers()).thenReturn(HttpHeaders.of(headers, (k, v) -> true));
        when(http.sendAsync(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    @Test
    void invalidUsername_NotPremium_WithoutHttpCall() {
        for (String name : new String[]{"bad name", "../evil", "a?b", "x#y", "waytoolongusername1", ""}) {
            LookupResult result = client.lookup(name, 1000, false);
            Success success = assertInstanceOf(Success.class, result, name);
            assertFalse(success.premium());
            assertEquals(MojangApiClient.computeOfflineUUID(name), success.uuid());
        }
        verifyNoInteractions(http);
    }

    @Test
    void premiumResponse_Parsed() {
        respond(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}", Map.of());

        Success success = assertInstanceOf(Success.class, client.lookup("notch", 1000, false));

        assertTrue(success.premium());
        assertEquals("Notch", success.correctName());
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), success.uuid());
    }

    @Test
    void notFound_NotPremium() {
        respond(404, "", Map.of());

        Success success = assertInstanceOf(Success.class, client.lookup("Nobody_123", 1000, false));

        assertFalse(success.premium());
    }

    @Test
    void malformedBodies_ReturnFailure() {
        for (String body : new String[]{"{not json", "[]", "{\"name\":\"Notch\"}", "{\"id\":\"zz\",\"name\":\"Notch\"}",
                "{\"id\":\"069a79f444e94726a5befca90e38zzzz\",\"name\":\"Notch\"}", "\"str\""}) {
            assertInstanceOf(Failure.class, MojangApiClient.parsePremiumResponse(body), body);
        }
    }

    @Test
    void rateLimited_BacksOffWithoutNewRequests() {
        respond(429, "", Map.of("Retry-After", List.of("30")));

        assertInstanceOf(Failure.class, client.lookup("Alice", 1000, false));
        // A different name is still blocked by the global rate-limit backoff
        assertInstanceOf(Failure.class, client.lookup("Bob", 1000, false));

        verify(http, times(1)).sendAsync(any(), any());
    }

    @Test
    void failure_BacksOffSameName() {
        respond(500, "", Map.of());

        assertInstanceOf(Failure.class, client.lookup("Alice", 1000, false));
        assertInstanceOf(Failure.class, client.lookup("alice", 1000, false));

        verify(http, times(1)).sendAsync(any(), any());
    }

    @Test
    void slowResponse_BoundedByTimeout() {
        when(http.sendAsync(any(), any())).thenReturn(new CompletableFuture<>()); // never completes

        long start = System.currentTimeMillis();
        Failure failure = assertInstanceOf(Failure.class, client.lookup("Alice", 100, false));

        assertTrue(failure.reason().startsWith("Timeout"), failure.reason());
        assertTrue(System.currentTimeMillis() - start < 2000);
    }

    @Test
    void unexpectedRuntimeError_ReturnsFailure() {
        when(http.sendAsync(any(), any())).thenThrow(new IllegalStateException("boom"));

        assertInstanceOf(Failure.class, client.lookup("Alice", 1000, false));
    }
}
