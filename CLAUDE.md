# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A **multi-module Gradle build** for an SMP (Survival Multiplayer) test network on Minecraft **26.2**. It bundles two things that used to live in separate repos:

1. **The SmpAuth auth stack** (formerly `smp-robby`) — DataGSM OAuth login gated through a Velocity proxy and a Minestom lobby, plus a Paper-side content library.
2. **The oh-my-smp Paper plugin** (`smp-server` module) — the actual SMP gameplay rules (world border, combat tagging, ender dragon buffs, student nametags), which consumes the auth data via `content-lib`.

Modules (`settings.gradle.kts`):

| Module | Lang | What it is |
|--------|------|------------|
| `common` | Java | Wire contracts (`StudentData`, `AuthMessage`, JSON DTOs) shared across the stack. |
| `auth-server` | Kotlin/Ktor | DataGSM OAuth web server; issues login keys, stores links in SQLite. |
| `velocity-plugin` | Java | Velocity proxy plugin — gates servers behind auth, forwards auth data. |
| `lobby-server` | Java/Minestom | The login lobby players land in first. |
| `content-lib` | Java | Paper plugin **SmpAuth**; exposes `iieiiergn.smpAuth.paperlib.SmpAuth` + `AuthDataLoadedEvent` to content plugins. Bundles `common`. |
| `sample-content-plugin` | Java | Reference content plugin (not deployed by `setup.sh`). |
| `smp-server` | Kotlin | **oh-my-smp** — the Paper SMP gameplay plugin. Compiles against `:content-lib`. |

Everything targets the **Java 25 toolchain**; versions are centralized in `gradle/libs.versions.toml`. The JDK 25 location is pinned in `gradle.properties` (brew `openjdk@25`) — adjust if yours lives elsewhere.

## Build & run

- `./gradlew build` — builds every module.
- `./gradlew :smp-server:build` — just the oh-my-smp plugin. **Deployable artifact is `smp-server/build/libs/oh-my-smp-<version>-all.jar`** (the `-all` shadow jar, which bundles the Kotlin stdlib). The plain `oh-my-smp-<version>.jar` lacks the Kotlin runtime and throws `NoClassDefFoundError` if dropped into a server — never deploy it. The artifact keeps the `oh-my-smp` base name (via `base.archivesName`) even though the module dir is `smp-server`.
- `./gradlew :smp-server:runServer` — launches a standalone Paper test server under `smp-server/run/` with just this plugin. Without the SmpAuth plugin present the nametag/`/student-info` features self-disable (logged warning); other gameplay works. First run stops on the Mojang EULA; set `eula=true` in `smp-server/run/eula.txt` to continue. There are no unit tests; verify behavior on a server.

### Full test network — `setup.sh`

`./setup.sh` provisions the **whole 4-process stack** (auth + Minestom lobby + Velocity + a Paper "content" server running SmpAuth + oh-my-smp) into whatever scratch directory you run it from:

```
mkdir ~/smp-test && cd ~/smp-test
/path/to/oh-my-smp/setup.sh   # builds jars, downloads Velocity + Paper, writes configs, patches Paper forwarding
./start-all.sh                # launch all four (each in its own tmux session); connect a 26.2 client to 127.0.0.1:25565
./console.sh {auth|lobby|velocity|paper}   # attach to a server's console to run admin commands (detach: Ctrl-B, D)
./stop-all.sh
```

It refuses to run inside the repo root (to avoid littering the source tree). Ports: Velocity 25565, lobby 25566, Paper 25567, auth 8080. Override secrets by dropping a `secrets.env` next to where you run it. Requires `tmux` (`brew install tmux`) for console access.

After code changes, run `/path/to/oh-my-smp/update.sh` from the same scratch directory to rebuild and swap in **only the jars** (configs, the auth SQLite db, and Paper worlds are untouched) — no need to rerun `setup.sh`. Pass `--restart` to also bounce the whole stack; otherwise restart manually (`./stop-all.sh && ./start-all.sh`) since running servers keep their loaded classes.

## Architecture (smp-server / oh-my-smp)

Each feature lives in its own package under `smp-server/src/main/kotlin/iieiiergn/ohMySmp/` and is wired together in `OhMySmp.onEnable()`:

- `config/PluginConfig` — reads `config.yml` once into typed fields; pass this object to features rather than re-reading config.
- `border/`, `combat/`, `dragon/`, `spawn/`, `listener/` — one concern each (world border, combat tagging + action bar, ender dragon buffs, safe-respawn location finding, death/respawn handlers).
- `nametag/` — student nametags + `/student-info`, driven by SmpAuth data. Gated on the `SmpAuth` plugin being present (`softdepend`); skipped with a warning otherwise.
- Managers holding scheduler tasks (e.g. `DragonManager`, `CombatDisplay`) expose `start()`/`stop()` and must be cancelled in `onDisable()`.

The auth API (`StudentData`, `SmpAuth.get(player)`, `AuthDataLoadedEvent`) comes from `compileOnly(project(":content-lib"))` — **not** a vendored jar. `content-lib` re-exposes `common` via `api(...)`, so `StudentData` resolves transitively. It is `compileOnly` because the running SmpAuth plugin provides these classes; never shade them.

When adding a gameplay feature: new package + register its listener/manager in `onEnable()`, and add any settings to both `config/PluginConfig` and `smp-server/src/main/resources/config.yml`.

## Config behavior

`config.yml` in `smp-server/src/main/resources/` is the bundled default template. At runtime `saveDefaultConfig()` extracts it to `plugins/oh-my-smp/config.yml`, which is what server admins edit. There is no in-game reload command — **config changes require a server restart** to take effect.
