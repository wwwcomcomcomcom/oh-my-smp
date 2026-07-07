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

    // oh-my-smp gameplay rule values, mirrored from the profile (rendered by smp.sh)
    // so the /guide book shows the same numbers as the Paper server. The lobby only
    // *displays* these — the Paper plugin is where they take effect. Defaults MUST
    // match smp-server's PluginConfig / config.yml.
    public final double borderRadius;
    public final double borderCenterX;
    public final double borderCenterZ;
    public final double naturalDropChance;
    public final long combatDurationSeconds;
    public final boolean nametagEnabled;
    public final double dragonMaxHealth;
    public final boolean dragonImmuneExplosion;
    public final boolean dragonImmuneProjectile;
    public final double dragonRegenAmount;
    public final long dragonRegenIntervalTicks;

    private LobbyConfig(Properties p) {
        this.host = p.getProperty("host", "0.0.0.0");
        this.port = Integer.parseInt(p.getProperty("port", "25566"));
        this.velocitySecret = p.getProperty("velocitySecret", "");
        this.authServerBaseUrl = p.getProperty("authServerBaseUrl", "http://localhost:8080");
        this.authLoginUrl = p.getProperty("authLoginUrl", authServerBaseUrl + "/login");
        this.sharedSecret = p.getProperty("sharedSecret", "");

        this.borderRadius = Double.parseDouble(p.getProperty("rule.borderRadius", "5000"));
        this.borderCenterX = Double.parseDouble(p.getProperty("rule.borderCenterX", "0"));
        this.borderCenterZ = Double.parseDouble(p.getProperty("rule.borderCenterZ", "0"));
        this.naturalDropChance = Double.parseDouble(p.getProperty("rule.naturalDropChance", "0.3"));
        this.combatDurationSeconds = Long.parseLong(p.getProperty("rule.combatDurationSeconds", "15"));
        this.nametagEnabled = Boolean.parseBoolean(p.getProperty("rule.nametagEnabled", "true"));
        this.dragonMaxHealth = Double.parseDouble(p.getProperty("rule.dragonMaxHealth", "1000"));
        this.dragonImmuneExplosion = Boolean.parseBoolean(p.getProperty("rule.dragonImmuneExplosion", "true"));
        this.dragonImmuneProjectile = Boolean.parseBoolean(p.getProperty("rule.dragonImmuneProjectile", "true"));
        this.dragonRegenAmount = Double.parseDouble(p.getProperty("rule.dragonRegenAmount", "1"));
        this.dragonRegenIntervalTicks = Long.parseLong(p.getProperty("rule.dragonRegenIntervalTicks", "20"));
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
            // oh-my-smp rule values for the /guide book (see field docs above);
            // defaults match smp-server. Normally rendered by smp.sh from the profile.
            p.setProperty("rule.borderRadius", "5000");
            p.setProperty("rule.borderCenterX", "0");
            p.setProperty("rule.borderCenterZ", "0");
            p.setProperty("rule.naturalDropChance", "0.3");
            p.setProperty("rule.combatDurationSeconds", "15");
            p.setProperty("rule.nametagEnabled", "true");
            p.setProperty("rule.dragonMaxHealth", "1000");
            p.setProperty("rule.dragonImmuneExplosion", "true");
            p.setProperty("rule.dragonImmuneProjectile", "true");
            p.setProperty("rule.dragonRegenAmount", "1");
            p.setProperty("rule.dragonRegenIntervalTicks", "20");
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "SMP Lobby config");
            }
        }
        return new LobbyConfig(p);
    }
}
