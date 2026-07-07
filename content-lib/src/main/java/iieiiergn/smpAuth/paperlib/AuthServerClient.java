package iieiiergn.smpAuth.paperlib;

import iieiiergn.smpAuth.common.Json;
import iieiiergn.smpAuth.common.RestDtos.LinkResponse;
import iieiiergn.smpAuth.common.StudentData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Thin async client over the auth-server REST API (shared-secret authenticated).
 *
 * <p>The content server reads each player's DataGSM link straight from the auth-server —
 * the persistent source of truth — instead of a proxy-side in-memory cache, so a player's
 * nametag/{@code /student-info} data can't be lost to a proxy reconnect race.
 */
public final class AuthServerClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String baseUrl;
    private final String sharedSecret;

    public AuthServerClient(String baseUrl, String sharedSecret) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.sharedSecret = sharedSecret;
    }

    /** GET /api/links/{uuid} → the student snapshot, or null if not linked / on error. */
    public CompletableFuture<StudentData> fetchLink(UUID uuid) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/links/" + uuid))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + sharedSecret)
                .GET()
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) return null;
                    LinkResponse parsed = Json.GSON.fromJson(resp.body(), LinkResponse.class);
                    return (parsed != null && parsed.linked()) ? parsed.student() : null;
                })
                .exceptionally(ex -> null);
    }
}
