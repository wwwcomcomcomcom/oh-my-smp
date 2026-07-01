package iieiiergn.smpAuth.lobby;

import iieiiergn.smpAuth.common.Json;
import iieiiergn.smpAuth.common.RestDtos.BindRequest;
import iieiiergn.smpAuth.common.RestDtos.BindResponse;
import iieiiergn.smpAuth.common.StudentData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Lobby-side client for the auth-server bind endpoint. */
public final class AuthClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String baseUrl;
    private final String sharedSecret;

    public AuthClient(String baseUrl, String sharedSecret) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.sharedSecret = sharedSecret;
    }

    /**
     * POST /api/bind — exchange a pasted key for the student snapshot, binding it to the player's UUID.
     * Completes with the {@link StudentData} on success, or null if the key was invalid/expired or a network error occurred.
     */
    public CompletableFuture<StudentData> bind(UUID uuid, String username, String key) {
        String body = Json.GSON.toJson(new BindRequest(key, uuid.toString(), username));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/bind"))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + sharedSecret)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) return null;
                    BindResponse parsed = Json.GSON.fromJson(resp.body(), BindResponse.class);
                    return parsed != null ? parsed.student() : null;
                })
                .exceptionally(ex -> null);
    }
}
