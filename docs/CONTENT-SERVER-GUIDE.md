# Content Server Integration Guide

How to read a player's **DataGSM auth data** (student name, grade, class, major, GitHub, …)
on a Paper/Spigot content server in the smp-robby network.

You do **not** talk to the auth-server or the OAuth flow yourself. By the time a player
reaches your content server, Velocity has already verified they are linked (unlinked players
are blocked from transferring) and holds their data globally. Your server just asks for it
over plugin messaging — and the `SmpAuth` plugin does that for you.

---

## 1. Install the SmpAuth plugin

Build it and drop it into your content server's `plugins/` folder:

```bash
./gradlew :content-lib:shadowJar
# -> content-lib/build/libs/content-lib.jar   (rename to SmpAuth.jar if you like)
```

`SmpAuth` registers the `smpauth:data` plugin channel, requests each joining player's data
from Velocity, caches it, and exposes it. No configuration needed.

> Requirement: content servers run on **Java 25+** (the whole network targets Java 25) and
> behind the same Velocity proxy with modern forwarding enabled.

## 2. Depend on it

In your plugin's `plugin.yml`:

```yaml
depend: [SmpAuth]
```

In Gradle, compile against the API (provided at runtime by the SmpAuth plugin — do **not** shade it):

```kotlin
dependencies {
    compileOnly(project(":content-lib"))   // or the published SmpAuth artifact
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
```

## 3. Read the data

Two ways — poll, or react.

### Poll with `SmpAuth.get(player)`

```java
import iieiiergn.smpAuth.paperlib.SmpAuth;
import iieiiergn.smpAuth.common.StudentData;
import java.util.Optional;

Optional<StudentData> data = SmpAuth.get(player);
data.ifPresent(s ->
    player.sendMessage("환영합니다, " + s.name() + " (" + s.grade() + "학년)"));
```

> Right after join the data can be absent for a tick or two while the request round-trips.
> If you need the exact moment it arrives, use the event below.

### React with `AuthDataLoadedEvent`

```java
import iieiiergn.smpAuth.paperlib.AuthDataLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    @EventHandler
    public void onAuth(AuthDataLoadedEvent event) {
        if (!event.isLinked()) return;                 // not linked (rare on a gated server)
        var s = event.getData();
        event.getPlayer().sendMessage("학번: " + s.studentNumber());
    }
}
```

## 4. Worked example — gate by grade

```java
Optional<StudentData> data = SmpAuth.get(player);
if (data.isPresent() && Integer.valueOf(3).equals(data.get().grade())) {
    // grade-3 only feature
} else {
    player.sendMessage("3학년만 입장할 수 있습니다.");
}
```

See `sample-content-plugin/` for a complete, working example (`/whoami`, `/seniors`, join greeting).

---

## `StudentData` fields

| Field | Type | Notes |
|-------|------|-------|
| `datagsmId()` | `Long` | DataGSM account id (stable external identity) |
| `email()` | `String` | |
| `role()` | `String` | account role (`USER`, `ADMIN`, …) |
| `isStudent()` | `boolean` | false for non-student accounts; student fields below are null |
| `name()` | `String` | |
| `studentNumber()` | `Integer` | |
| `grade()` / `classNum()` / `number()` | `Integer` | 학년 / 반 / 번호 |
| `sex()` | `String` | `MAN` / `WOMAN` |
| `major()` | `String` | `SW_DEVELOPMENT` / `SMART_IOT` / `AI` |
| `studentRole()` | `String` | `STUDENT_COUNCIL` / `DORMITORY_MANAGER` / `GENERAL_STUDENT` |
| `dormitoryFloor()` / `dormitoryRoom()` | `Integer` | |
| `githubId()` / `githubUrl()` | `String` | nullable |
| `majorClub()` / `autonomousClub()` | `ClubInfoDto` | nullable; `id/name/type/status/foundedYear/abolishedYear` |

## Caveats

- **Snapshot, never refreshed.** The data is captured once when the player first links
  (`/login` → `/verify`) and never auto-updates. A grade rollover won't reflect until they re-link.
- **Unlinked players never reach you.** Velocity blocks transfer to any non-lobby server until
  the player links, so on a content server `SmpAuth.get` is effectively always present —
  but still guard for `Optional.empty()` defensively.
- **Don't bundle `common` yourself.** Let the SmpAuth plugin own `StudentData`/`AuthMessage`;
  shading your own copy will cause `ClassCastException` across plugin classloaders.
