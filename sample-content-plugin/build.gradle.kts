plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencies {
    compileOnly(libs.paper.api)
    // Compiled against the SmpAuth plugin's API; provided at runtime by the SmpAuth plugin (plugin.yml depend).
    compileOnly(project(":content-lib"))
}
