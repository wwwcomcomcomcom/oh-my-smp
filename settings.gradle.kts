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
        // Minestom snapshots (26_2-SNAPSHOT lives here, not on Central yet)
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
