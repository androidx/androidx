plugins {
    kotlin("multiplatform") version "1.7.10"
    application
}

group = "net.saff"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("http://localhost:1480")
        isAllowInsecureProtocol = true
    }
    mavenCentral()
    google()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val nativeMain by getting
        val commonMain by getting {
            dependencies {
                implementation("androidx.collection:collection:1.3.0-alpha03")
            }
        }
        val nativeTest by getting
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

application {
    mainClass.set("MainKt")
}
