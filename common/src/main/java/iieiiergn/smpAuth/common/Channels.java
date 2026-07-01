package iieiiergn.smpAuth.common;

/** Plugin-messaging channel identifiers shared by Velocity, the lobby, and content servers. */
public final class Channels {

    /** Namespaced channel ({@code namespace:key}, lowercase) carrying {@link AuthMessage} envelopes. */
    public static final String AUTH = "smpauth:data";

    private Channels() {
    }
}
