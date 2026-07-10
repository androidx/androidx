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

package androidx.room3.gradle

import androidx.kruth.assertThat
import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test

class RoomKmpGradlePluginTest {
    @get:Rule val projectSetup = ProjectSetupRule()

    private val roomVersion by lazy {
        projectSetup.getLibraryLatestVersionInLocalRepo("androidx/room3/room3-compiler")
    }

    private fun setup(projectRoot: File = projectSetup.rootDir) {
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
            |    id('androidx.room3')
            |}
            |
            |$repositoriesBlock
            |
            |${projectSetup.androidProject}
            |
            |kotlin {
            |  androidTarget {
            |    compilerOptions {
            |      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            |    }
            |  }
            |  linuxX64()
            |  jvm()
            |  sourceSets {
            |    commonMain {
            |      dependencies {
            |        implementation "androidx.room3:room3-runtime:$roomVersion"
            |      }
            |    }
            |  }
            |
            |  compilerOptions {
            |    languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
            |  }
            |}
            |
            |dependencies {
            |    add("kspCommonMainMetadata", "androidx.room3:room3-compiler:$roomVersion")
            |    add("kspAndroid", "androidx.room3:room3-compiler:$roomVersion")
            |    add("kspLinuxX64", "androidx.room3:room3-compiler:$roomVersion")
            |    add("kspJvm", "androidx.room3:room3-compiler:$roomVersion")
            |}
            |
            |android {
            |    namespace "room.testapp"
            |    compileOptions {
            |      sourceCompatibility = JavaVersion.VERSION_17
            |      targetCompatibility = JavaVersion.VERSION_17
            |    }
            |}
            |
            |room3 {
            |  schemaDirectory("metadata", "${'$'}projectDir/schemas/common")
            |  schemaDirectory("android", "${'$'}projectDir/schemas/android")
            |  schemaDirectory("linuxX64", "${'$'}projectDir/schemas/native")
            |  schemaDirectory("jvm", "${'$'}projectDir/schemas/jvm")
            |}
            |
            |ksp {
            |  useKsp2 = true
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
                projectSetup = projectSetup,
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
    fun `Blocking query DAO function in non-Android source set`() {
        setup()

        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/nativeMain/kotlin/room/testapp/MyDatabase.kt"),
            search = "// Insert-change",
            replace =
                """
                @Query("SELECT * FROM NativeEntity")
                fun blockingQuery(): NativeEntity
                """
                    .trimIndent(),
        )

        runGradle(NATIVE_COMPILE_TASK, projectSetup = projectSetup, expectFailure = true).let {
            result ->
            result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
            result.output.contains(
                "Only suspend functions are allowed in DAOs" + " declared in non-Android platforms."
            )
        }
    }

    @Test
    fun `Blocking shortcut DAO function in non-Android source set`() {
        setup()

        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/nativeMain/kotlin/room/testapp/MyDatabase.kt"),
            search = "// Insert-change",
            replace =
                """
                @Insert
                fun blockingInsert(entity: NativeEntity)
                """
                    .trimIndent(),
        )

        runGradle(NATIVE_COMPILE_TASK, projectSetup = projectSetup, expectFailure = true).let {
            result ->
            result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
            result.output.contains(
                "Only suspend functions are allowed in DAOs" + " declared in non-Android platforms."
            )
        }
    }

    @Test
    fun `Blocking transaction wrapper DAO function in non-Android source set`() {
        setup()

        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/nativeMain/kotlin/room/testapp/MyDatabase.kt"),
            search = "// Insert-change",
            replace =
                """
                @Transaction
                fun blockingTransaction() { }
                """
                    .trimIndent(),
        )

        runGradle(NATIVE_COMPILE_TASK, projectSetup = projectSetup, expectFailure = true).let {
            result ->
            result.assertTaskOutcome(NATIVE_KSP_TASK, TaskOutcome.FAILED)
            result.output.contains(
                "Only suspend functions are allowed in DAOs" + " declared in non-Android platforms."
            )
        }
    }

    companion object {
        private const val CLEAN_TASK = ":clean"
        private const val COMMON_KSP_TASK = ":kspCommonMainKotlinMetadata"
        private const val ANDROID_COMPILE_TASK = ":compileDebugKotlinAndroid"
        private const val ANDROID_KSP_TASK = ":kspDebugKotlinAndroid"
        private const val NATIVE_COMPILE_TASK = ":compileKotlinLinuxX64"
        private const val NATIVE_KSP_TASK = ":kspKotlinLinuxX64"
        private const val JVM_COMPILE_TASK = ":compileKotlinJvm"
        private const val JVM_KSP_TASK = ":kspKotlinJvm"
        private const val ANDROID_COPY_TASK = ":copyRoomSchemasAndroid"
        private const val NATIVE_COPY_TASK = ":copyRoomSchemasLinuxX64"
    }
}
