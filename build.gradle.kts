plugins {
    kotlin("multiplatform") version "2.3.10" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}

allprojects {
    group = "com.kdockerck"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "kotlin-toolchain")
    apply(plugin = "code-quality")
}