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
    // `common` is bundled into this plugin jar; it becomes the single runtime owner
    // of StudentData/AuthMessage, which dependent content plugins access via the API.
    api(project(":common"))
    compileOnly(libs.paper.api)
}

// The deployable plugin jar is content-lib.jar (the shaded one, named "SmpAuth" when you rename).
tasks.jar {
    archiveClassifier = "thin"
}
tasks.shadowJar {
    archiveClassifier = ""
    // Paper provides Gson at runtime.
    dependencies {
        exclude(dependency("com.google.code.gson:gson"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
