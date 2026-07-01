package iieiiergn.smpAuth.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Velocity plugin config, loaded from {@code <dataDir>/config.properties} (written with defaults if absent). */
public final class VelocityConfig {

    public final String authServerBaseUrl;
    public final String sharedSecret;
    public final String lobbyServerName;
    /** Explicit gated servers; empty means "every server except the lobby is gated". */
    private final Set<String> gatedServers;

    private VelocityConfig(String authServerBaseUrl, String sharedSecret, String lobbyServerName, Set<String> gated) {
        this.authServerBaseUrl = authServerBaseUrl;
        this.sharedSecret = sharedSecret;
        this.lobbyServerName = lobbyServerName;
        this.gatedServers = gated;
    }

    /** A server requires a link unless it is the lobby. */
    public boolean isGated(String serverName) {
        if (serverName.equals(lobbyServerName)) return false;
        return gatedServers.isEmpty() || gatedServers.contains(serverName);
    }

    public static VelocityConfig load(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        Path file = dataDir.resolve("config.properties");
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }
        } else {
            props.setProperty("authServerBaseUrl", "http://localhost:8080");
            props.setProperty("sharedSecret", "CHANGE_ME_LONG_RANDOM_SECRET");
            props.setProperty("lobbyServerName", "lobby");
            props.setProperty("gatedServers", "");
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "SMP Auth — Velocity config. gatedServers: comma-separated; empty = all non-lobby servers gated.");
            }
        }

        Set<String> gated = new HashSet<>();
        for (String s : props.getProperty("gatedServers", "").split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) gated.add(t);
        }
        return new VelocityConfig(
                props.getProperty("authServerBaseUrl", "http://localhost:8080"),
                props.getProperty("sharedSecret", ""),
                props.getProperty("lobbyServerName", "lobby"),
                gated
        );
    }

    @Override
    public String toString() {
        return "VelocityConfig{authServerBaseUrl=" + authServerBaseUrl
                + ", lobbyServerName=" + lobbyServerName
                + ", gatedServers=" + (gatedServers.isEmpty() ? "<all non-lobby>" : gatedServers) + "}";
    }
}
