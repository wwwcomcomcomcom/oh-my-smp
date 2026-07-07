pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Velocity + Paper API
        maven("https://repo.papermc.io/repository/maven-public/")
        // Minestom snapshots (kept for snapshot builds; the pinned 26.1.2 release lives on Central)
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content { includeGroup("net.minestom") }
            mavenContent { snapshotsOnly() }
        }
        // DataGSM OAuth SDK
        maven("https://jitpack.io")
    }
}

rootProject.name = "oh-my-smp"

include(
    "common",
    "auth-server",
    "velocity-plugin",
    "lobby-server",
    "content-lib",
    "sample-content-plugin",
    "smp-server",
)
