package iieiiergn.smpAuth.velocity;

import iieiiergn.smpAuth.common.StudentData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory, proxy-global auth state: the single runtime source of truth for linked players. */
public final class AuthState {

    private final ConcurrentHashMap<UUID, StudentData> byUuid = new ConcurrentHashMap<>();

    public void put(UUID uuid, StudentData data) {
        byUuid.put(uuid, data);
    }

    public StudentData get(UUID uuid) {
        return byUuid.get(uuid);
    }

    public boolean isLinked(UUID uuid) {
        return byUuid.containsKey(uuid);
    }

    public void remove(UUID uuid) {
        byUuid.remove(uuid);
    }
}
