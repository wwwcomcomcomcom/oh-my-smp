plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    // SmpAuth(content-lib) 컨텐츠 API. content-lib 가 api(project(":common")) 로 StudentData 를
    // 재노출하므로 이 한 줄로 paperlib.SmpAuth / AuthDataLoadedEvent / common.StudentData 컴파일
    // 의존이 모두 해결된다. 런타임엔 SmpAuth 플러그인이 제공하므로 compileOnly(=shade 금지).
    compileOnly(project(":content-lib"))
    implementation(libs.kotlin.stdlib)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

// 배포 산출물명을 oh-my-smp-<version>-all.jar 로 유지(모듈명 smp-server 와 무관하게).
base {
    archivesName = "oh-my-smp"
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
