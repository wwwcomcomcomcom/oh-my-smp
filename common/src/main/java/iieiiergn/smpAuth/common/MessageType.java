package iieiiergn.smpAuth.common;

/** Message kinds carried over the {@link Channels#AUTH} plugin-messaging channel. */
public enum MessageType {

    /** Backend → Velocity: "send me the auth data for this connection's player." */
    AUTH_REQUEST,

    /** Velocity → backend: the player's auth state (see {@link AuthMessage#linked()}). */
    AUTH_RESPONSE,

    /** Lobby → Velocity: "this player just linked, reload their state from the auth-server." */
    LINK_UPDATED,
}
