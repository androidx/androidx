import androidx.build.AndroidXComposePlugin
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import androidx.build.JetbrainsAndroidXPlugin

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("JetbrainsAndroidXPlugin")
}

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)
JetbrainsAndroidXPlugin.applyAndConfigure(project)

repositories {
    mavenLocal()
}

fun KotlinNativeBinaryContainer.configureFramework() {
    framework {
        baseName = "shared"
        freeCompilerArgs += listOf(
            "-linker-option", "-framework", "-linker-option", "Metal",
            "-linker-option", "-framework", "-linker-option", "CoreText",
            "-linker-option", "-framework", "-linker-option", "CoreGraphics"
        )
    }
}

kotlin {
    val isArm64Host = System.getProperty("os.arch") == "aarch64"
    iosArm64 {
        binaries {
            configureFramework()
        }
    }
    if (isArm64Host) {
        iosSimulatorArm64 {
            binaries {
                configureFramework()
            }
        }
    } else {
        iosX64 {
            binaries {
                configureFramework()
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp"))
                implementation(project(":compose:mpp:demo"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(libs.kotlinCoroutinesCore)
            }
        }
        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.skikoCommon)
            }
        }
        val nativeMain by creating { dependsOn(skikoMain) }
        val darwinMain by creating { dependsOn(nativeMain) }
        val uikitMain by creating { dependsOn(darwinMain) }
        val iosArm64Main by getting { dependsOn(uikitMain) }
        if (isArm64Host) {
            val iosSimulatorArm64Main by getting { dependsOn(uikitMain) }
        } else {
            val iosX64Main by getting { dependsOn(uikitMain) }
        }
    }
}
