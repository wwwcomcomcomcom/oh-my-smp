plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}

// The deployable plugin jar is velocity-plugin.jar (the shaded one).
tasks.jar {
    archiveClassifier = "thin"
}
tasks.shadowJar {
    archiveClassifier = ""
    // Velocity provides Gson on its runtime classpath; don't bundle (and conflict with) it.
    dependencies {
        exclude(dependency("com.google.code.gson:gson"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
