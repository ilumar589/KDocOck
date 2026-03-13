plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(project(":shared"))
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(compose.html.core)
            }
        }
    }
}

compose.experimental {
    web.application {}
}