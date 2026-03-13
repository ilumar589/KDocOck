plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        withJava()
    }
    
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("io.arrow-kt:arrow-core:1.2.4")
                implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-cio:2.3.12")
                implementation("org.jetbrains.exposed:exposed-core:0.55.0")
                implementation("org.jetbrains.exposedx:exposed-r2dbc:0.3.1")
                implementation("io.r2dbc:r2dbc-postgresql:1.0.0.RELEASE")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.9.0")
                implementation("org.apache.poi:poi:5.3.0")
                implementation("org.apache.poi:poi-ooxml:5.3.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:5.9.1")
            }
        }

        val wasmJsMain by getting {
            dependencies {
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}