package iieiiergn.smpAuth.common;

/** Request/response bodies for the auth-server REST API (server-to-server, JSON). */
public final class RestDtos {

    /** {@code POST /api/bind} body sent by the lobby after a player pastes their key. */
    public record BindRequest(String key, String uuid, String username) {
    }

    /** {@code POST /api/bind} success body. */
    public record BindResponse(StudentData student) {
    }

    /** {@code GET /api/links/{uuid}} body. {@code student} is null when {@code linked} is false. */
    public record LinkResponse(boolean linked, StudentData student) {
    }

    private RestDtos() {
    }
}
