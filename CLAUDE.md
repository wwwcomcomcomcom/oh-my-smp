# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Paper (Bukkit) Minecraft server plugin written in Kotlin, implementing SMP gameplay rules. Targets **Paper API 26.2**, built with the Kotlin JVM plugin on **Java/Kotlin jvmToolchain 25**.

## Build & run

- `./gradlew build` — compiles and produces the plugin jar via shadowJar.
- **Deployable artifact is `build/libs/oh-my-smp-<version>-all.jar`** (the `-all` shadow jar, which bundles the Kotlin stdlib). The plain `oh-my-smp-<version>.jar` lacks the Kotlin runtime and throws `NoClassDefFoundError` if dropped into a server — never deploy it.
- `./gradlew runServer` — launches a real Paper test server under `run/` with this plugin loaded. First run stops on the Mojang EULA; set `eula=true` in `run/eula.txt` to continue. There are no unit tests; verify behavior on this server.

## Architecture

Each feature lives in its own package under `src/main/kotlin/iieiiergn/ohMySmp/` and is wired together in `OhMySmp.onEnable()`:

- `config/PluginConfig` — reads `config.yml` once into typed fields; pass this object to features rather than re-reading config.
- `border/`, `combat/`, `dragon/`, `spawn/`, `listener/` — one concern each (world border, combat tagging + action bar, ender dragon buffs, safe-respawn location finding, death/respawn handlers).
- Managers holding scheduler tasks (e.g. `DragonManager`, `CombatDisplay`) expose `start()`/`stop()` and must be cancelled in `onDisable()`.

When adding a feature, follow this pattern: new package + register its listener/manager in `onEnable()`, and add any settings to both `config/PluginConfig` and `src/main/resources/config.yml`.

## Config behavior

`config.yml` in `src/main/resources/` is the bundled default template. At runtime `saveDefaultConfig()` extracts it to `plugins/oh-my-smp/config.yml`, which is what server admins edit. There is no in-game reload command — **config changes require a server restart** to take effect.
