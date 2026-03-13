plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("io.ktor:ktor-server-netty")
    id("io.ktor:ktor-server-core")
    id("io.ktor:ktor-websockets")
    id("io.ktor:ktor-content-negotiation")
    id("io.ktor:ktor-status-pages")
}

dependencies {
    implementation(project(":shared"))
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-websockets-jvm:2.3.12")
    implementation("io.ktor:ktor-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-status-pages-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.github.microut:microut-core:3.2.0")
    implementation("io.github.microut:microut-logging:3.2.0")
}