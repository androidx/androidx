/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.gradle

import androidx.kruth.assertThat
import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test

class RoomKmpGradlePluginTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()

    private val roomVersion by lazy {
        projectSetup.getLibraryLatestVersionInLocalRepo("androidx/room/room-compiler")
    }

    private fun setup(
        projectRoot: File = projectSetup.rootDir,
    ) {
        // copy test project
        File("src/test/test-data/multiplatform-project").copyRecursively(projectRoot)

        val repositoriesBlock = buildString {
            appendLine("repositories {")
            projectSetup.allRepositoryPaths.forEach {
                appendLine("""maven { url "$it" }""")
            }
            appendLine("}")
        }

        // set up build file
        File(projectRoot, "build.gradle").writeText(
            """
            |plugins {
            |    id('com.android.application')
            |    id('kotlin-multiplatform')
            |    id('com.google.devtools.ksp')
            |    id('androidx.room')
            |}
            |
            |$repositoriesBlock
            |
            |${projectSetup.androidProject}
            |
            |// Disabled due to https://youtrack.jetbrains.com/issue/KT-65761
            |ext["kotlin.native.disableCompilerDaemon"] = 'true'
            |
            |kotlin {
            |  androidTarget()
            |  linuxX64("native")
            |  sourceSets {
            |    androidMain {
            |      dependencies {
            |        implementation "androidx.room:room-runtime:$roomVersion"
            |      }
            |    }
            |    nativeMain {
            |      dependencies {
            |        implementation "androidx.room:room-runtime:$roomVersion"
            |      }
            |    }
            |  }
            |}
            |
            |dependencies {
            |    add("kspAndroid", "androidx.room:room-compiler:$roomVersion")
            |    add("kspNative", "androidx.room:room-compiler:$roomVersion")
            |}
            |
            |android {
            |    namespace "room.testapp"
            |    compileOptions {
            |      sourceCompatibility = JavaVersion.VERSION_17
            |      targetCompatibility = JavaVersion.VERSION_17
            |    }
            |    kotlin {
            |      jvmToolchain(17)
            |    }
            |}
            |
            |room {
            |  schemaDirectory("android", "${'$'}projectDir/schemas/android")
            |  schemaDirectory("native", "${'$'}projectDir/schemas/native")
            |  generateKotlin = true
            |}
            |
            """.trimMargin()
        )
    }

    @Test
    fun testWorkflow() {
        setup()

        // First build, all tasks run
        runGradle(
            CLEAN_TASK, ANDROID_COMPILE_TASK, NATIVE_COMPILE_TASK,
            projectDir = projectSetup.rootDir
        ).let { result ->
            result.assertTaskOutcome(ANDROID_COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(NATIVE_COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(ANDROID_COPY_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(NATIVE_COPY_TASK, TaskOutcome.SUCCESS)
        }

        // Check created schema files
        val androidSchema =
            projectSetup.rootDir.resolve("schemas/android/room.testapp.MyDatabase/1.json")
        val nativeSchema =
            projectSetup.rootDir.resolve("schemas/native/room.testapp.MyDatabase/1.json")
        assertThat(androidSchema.exists())
        assertThat(nativeSchema.exists())
        // The schemas are different between targets
        assertThat(androidSchema.readText()).isNotEqualTo(nativeSchema.readText())
    }

    companion object {
        private const val CLEAN_TASK = ":clean"
        private const val ANDROID_COMPILE_TASK = ":compileDebugKotlinAndroid"
        private const val NATIVE_COMPILE_TASK = ":compileKotlinNative"
        private const val ANDROID_COPY_TASK = ":copyRoomSchemasAndroid"
        private const val NATIVE_COPY_TASK = ":copyRoomSchemasNative"
    }
}
