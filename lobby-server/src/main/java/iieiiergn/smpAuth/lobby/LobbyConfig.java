package iieiiergn.smpAuth.lobby;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Lobby config from {@code ./config.properties} (written with defaults if absent). */
public final class LobbyConfig {

    public final String host;
    public final int port;
    public final String velocitySecret;
    public final String authServerBaseUrl;
    public final String authLoginUrl;
    public final String sharedSecret;

    private LobbyConfig(Properties p) {
        this.host = p.getProperty("host", "0.0.0.0");
        this.port = Integer.parseInt(p.getProperty("port", "25566"));
        this.velocitySecret = p.getProperty("velocitySecret", "");
        this.authServerBaseUrl = p.getProperty("authServerBaseUrl", "http://localhost:8080");
        this.authLoginUrl = p.getProperty("authLoginUrl", authServerBaseUrl + "/login");
        this.sharedSecret = p.getProperty("sharedSecret", "");
    }

    public static LobbyConfig load() throws IOException {
        Path file = Path.of("config.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            }
        } else {
            p.setProperty("host", "0.0.0.0");
            p.setProperty("port", "25566");
            p.setProperty("velocitySecret", "CHANGE_ME_VELOCITY_FORWARDING_SECRET");
            p.setProperty("authServerBaseUrl", "http://localhost:8080");
            p.setProperty("authLoginUrl", "http://localhost:8080/login");
            p.setProperty("sharedSecret", "CHANGE_ME_LONG_RANDOM_SECRET");
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "SMP Lobby config");
            }
        }
        return new LobbyConfig(p);
    }
}
