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
    @get:Rule val projectSetup = ProjectSetupRule()

    private val roomVersion by lazy {
        projectSetup.getLibraryLatestVersionInLocalRepo("androidx/room/room-compiler")
    }

    private fun setup(projectRoot: File = projectSetup.rootDir, generateKotlin: String = "true") {
        // copy test project
        File("src/test/test-data/multiplatform-project").copyRecursively(projectRoot)

        val repositoriesBlock = buildString {
            appendLine("repositories {")
            projectSetup.allRepositoryPaths.forEach { appendLine("""maven { url "$it" }""") }
            appendLine("}")
        }

        // set up build file
        File(projectRoot, "build.gradle")
            .writeText(
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
            |kotlin {
            |  androidTarget()
            |  linuxX64("native")
            |  jvm()
            |  sourceSets {
            |    commonMain {
            |      dependencies {
            |        implementation "androidx.room:room-runtime:$roomVersion"
            |      }
            |    }
            |  }
            |
            |  compilerOptions {
            |    languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
            |  }
            |}
            |
            |dependencies {
            |    add("kspCommonMainMetadata", "androidx.room:room-compiler:$roomVersion")
            |    add("kspAndroid", "androidx.room:room-compiler:$roomVersion")
            |    add("kspNative", "androidx.room:room-compiler:$roomVersion")
            |    add("kspJvm", "androidx.room:room-compiler:$roomVersion")
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
            |  schemaDirectory("metadata", "${'$'}projectDir/schemas/common")
            |  schemaDirectory("android", "${'$'}projectDir/schemas/android")
            |  schemaDirectory("native", "${'$'}projectDir/schemas/native")
            |  schemaDirectory("jvm", "${'$'}projectDir/schemas/jvm")
            |  generateKotlin = $generateKotlin
            |}
            |
            """
                    .trimMargin()
            )
    }

    @Test
    fun `Test Workflow`() {
        setup()

        // First build, all tasks run
        runGradle(
                CLEAN_TASK,
                ANDROID_COMPILE_TASK,
                NATIVE_COMPILE_TASK,
                projectDir = projectSetup.rootDir
            )
            .let { result ->
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

    @Test
    fun `Generate Java with Non-Android targets error`() {
        setup(generateKotlin = "false")

        // Common should fail with Kotlin codegen off as there are JVM and Native targets from it
        runGradle(COMMON_KSP_TASK, projectDir = projectSetup.rootDir, expectFailure = true).let {
            result ->
            assertThat(result.output)
                .contains("Cannot generate Java targeting a non-Android platform")
            result.assertTaskOutcome(COMMON_KSP_TASK, TaskOutcome.FAILED)
        }

        // Native should fail with Kotlin codegen off
        runGradle(NATIVE_COMPILE_TASK, projectDir = projectSetup.rootDir, expectFailure = true)
            .let { result ->
                assertThat(result.output)
                    .contains("Cannot generate Java targeting a non-Android platform")
                result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
            }

        // JVM should fail with Kotlin codegen off
        runGradle(JVM_COMPILE_TASK, projectDir = projectSetup.rootDir, expectFailure = true).let {
            result ->
            assertThat(result.output)
                .contains("Cannot generate Java targeting a non-Android platform")
            result.assertTaskOutcome(JVM_KSP_TASK, TaskOutcome.FAILED)
        }

        // Android is OK when Kotlin codegen is off
        runGradle(ANDROID_COMPILE_TASK, projectDir = projectSetup.rootDir, expectFailure = false)
            .assertTaskOutcome(ANDROID_KSP_TASK, TaskOutcome.SUCCESS)
    }

    @Test
    fun `Blocking query DAO function in non-Android source set`() {
        setup(generateKotlin = "true")

        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/nativeMain/kotlin/room/testapp/MyDatabase.kt"),
            search = "// Insert-change",
            replace =
                """
                @Query("SELECT * FROM NativeEntity")
                fun blockingQuery(): NativeEntity
            """
                    .trimIndent()
        )

        runGradle(NATIVE_COMPILE_TASK, projectDir = projectSetup.rootDir, expectFailure = true)
            .let { result ->
                result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
                result.output.contains(
                    "Only suspend functions are allowed in DAOs" +
                        " declared in non-Android platforms."
                )
            }
    }

    @Test
    fun `Blocking shortcut DAO function in non-Android source set`() {
        setup(generateKotlin = "true")

        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/nativeMain/kotlin/room/testapp/MyDatabase.kt"),
            search = "// Insert-change",
            replace =
                """
                @Insert
                fun blockingInsert(entity: NativeEntity)
            """
                    .trimIndent()
        )

        runGradle(NATIVE_COMPILE_TASK, projectDir = projectSetup.rootDir, expectFailure = true)
            .let { result ->
                result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
                result.output.contains(
                    "Only suspend functions are allowed in DAOs" +
                        " declared in non-Android platforms."
                )
            }
    }

    @Test
    fun `Blocking transaction wrapper DAO function in non-Android source set`() {
        setup(generateKotlin = "true")

        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/nativeMain/kotlin/room/testapp/MyDatabase.kt"),
            search = "// Insert-change",
            replace =
                """
                @Transaction
                fun blockingTransaction() { }
            """
                    .trimIndent()
        )

        runGradle(NATIVE_COMPILE_TASK, projectDir = projectSetup.rootDir, expectFailure = true)
            .let { result ->
                result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
                result.output.contains(
                    "Only suspend functions are allowed in DAOs" +
                        " declared in non-Android platforms."
                )
            }
    }

    companion object {
        private const val CLEAN_TASK = ":clean"
        private const val COMMON_KSP_TASK = ":kspCommonMainKotlinMetadata"
        private const val ANDROID_COMPILE_TASK = ":compileDebugKotlinAndroid"
        private const val ANDROID_KSP_TASK = ":kspDebugKotlinAndroid"
        private const val NATIVE_COMPILE_TASK = ":compileKotlinNative"
        private const val NATIVE_KSP_TASK = ":kspKotlinNative"
        private const val JVM_COMPILE_TASK = ":compileKotlinJvm"
        private const val JVM_KSP_TASK = ":kspKotlinJvm"
        private const val ANDROID_COPY_TASK = ":copyRoomSchemasAndroid"
        private const val NATIVE_COPY_TASK = ":copyRoomSchemasNative"
    }
}
