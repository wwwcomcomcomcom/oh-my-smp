// Root project — no code. Shared conventions live in each module's build script.
// All modules target the Java 25 toolchain (see gradle/libs.versions.toml).
//
// Modules:
//   common / auth-server / velocity-plugin / lobby-server / content-lib / sample-content-plugin
//     — the SmpAuth (smp-robby) auth stack.
//   smp-server — the oh-my-smp Paper content plugin; depends on :content-lib.

tasks.register("printModules") {
    group = "help"
    description = "Lists the modules in this multi-module build."
    doLast {
        subprojects.forEach { println("• ${it.name}") }
    }
}
