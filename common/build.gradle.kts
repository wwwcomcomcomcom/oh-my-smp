plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
    withSourcesJar()
}

dependencies {
    // Gson is part of the wire contract (JSON over REST + plugin messaging),
    // so it is exposed as `api` to consumers of this module.
    api(libs.gson)
}
