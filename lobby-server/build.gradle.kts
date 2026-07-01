plugins {
    application
    alias(libs.plugins.shadow)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

application {
    mainClass = "iieiiergn.smpAuth.lobby.Main"
}

dependencies {
    implementation(project(":common"))
    implementation(libs.minestom)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

// Runnable fat jar is lobby-server-all.jar (Shadow's default classifier),
// kept distinct from the application plugin's own jar/dist tasks.
tasks.shadowJar {
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
