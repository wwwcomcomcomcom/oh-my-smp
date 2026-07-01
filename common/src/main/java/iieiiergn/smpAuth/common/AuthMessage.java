package iieiiergn.smpAuth.common;

import java.nio.charset.StandardCharsets;

/**
 * Envelope for the {@link Channels#AUTH} plugin-messaging channel, serialized as UTF-8 JSON.
 *
 * @param type    message kind
 * @param uuid    the player's Mojang UUID (string form); may be {@code null} for requests
 *                where Velocity derives the player from the connection
 * @param linked  whether the player has a DataGSM link (meaningful for {@link MessageType#AUTH_RESPONSE})
 * @param student the enrichment payload, or {@code null} when not linked / not applicable
 */
public record AuthMessage(MessageType type, String uuid, boolean linked, StudentData student) {

    public static AuthMessage request() {
        return new AuthMessage(MessageType.AUTH_REQUEST, null, false, null);
    }

    public static AuthMessage linkUpdated(String uuid) {
        return new AuthMessage(MessageType.LINK_UPDATED, uuid, false, null);
    }

    public static AuthMessage response(String uuid, StudentData student) {
        return new AuthMessage(MessageType.AUTH_RESPONSE, uuid, student != null, student);
    }

    /** Serialize to JSON bytes for {@code sendPluginMessage}. */
    public byte[] encode() {
        return Json.GSON.toJson(this).getBytes(StandardCharsets.UTF_8);
    }

    /** Parse a plugin-message payload back into an envelope. */
    public static AuthMessage decode(byte[] data) {
        return Json.GSON.fromJson(new String(data, StandardCharsets.UTF_8), AuthMessage.class);
    }
}
