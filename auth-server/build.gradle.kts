plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

application {
    mainClass = "iieiiergn.smpAuth.auth.ApplicationKt"
}

dependencies {
    implementation(project(":common"))

    implementation(libs.datagsm.sdk)
    implementation(libs.sqlite.jdbc)
    implementation(libs.gson)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)

    runtimeOnly(libs.logback.classic)
}
