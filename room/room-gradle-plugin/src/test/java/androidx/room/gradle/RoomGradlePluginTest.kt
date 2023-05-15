/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class RoomGradlePluginTest(
    @TestParameter val backend: ProcessingBackend
) {
    @get:Rule
    val projectSetup = ProjectSetupRule()

    private val roomVersion by lazy {
        projectSetup.getLibraryLatestVersionInLocalRepo("androidx/room/room-compiler")
    }

    private fun setup(projectName: String, projectRoot: File = projectSetup.rootDir) {
        // copy test project
        File("src/test/test-data/$projectName").copyRecursively(projectRoot)

        if (backend.isForKotlin) {
            // copy Kotlin database file
            File("src/test/test-data/kotlin/MyDatabase.kt").let {
                it.copyTo(projectRoot.resolve("src/main/java/room/testapp/${it.name}"))
            }
        } else {
            // copy Java database file
            File("src/test/test-data/java/MyDatabase.java").let {
                it.copyTo(projectRoot.resolve("src/main/java/room/testapp/${it.name}"))
            }
        }

        val additionalPluginsBlock = when (backend) {
            ProcessingBackend.JAVAC ->
                ""
            ProcessingBackend.KAPT ->
                """
                    id('kotlin-android')
                    id('kotlin-kapt')
                """
            ProcessingBackend.KSP ->
                """
                    id('kotlin-android')
                    id('com.google.devtools.ksp')
                """
        }

        val repositoriesBlock = buildString {
            appendLine("repositories {")
            projectSetup.allRepositoryPaths.forEach {
                appendLine("""maven { url "$it" }""")
            }
            appendLine("}")
        }

        val processorConfig = when (backend) {
            ProcessingBackend.JAVAC -> "annotationProcessor"
            ProcessingBackend.KAPT -> "kapt"
            ProcessingBackend.KSP -> "ksp"
        }

        val kotlinJvmTargetBlock = if (backend.isForKotlin) {
            """
            tasks.withType(
                org.jetbrains.kotlin.gradle.tasks.KotlinCompile
            ).configureEach {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
            """.trimIndent()
        } else {
            ""
        }

        // set up build file
        File(projectRoot, "build.gradle").writeText(
            """
            plugins {
                id('com.android.application')
                id('androidx.room')
                $additionalPluginsBlock
            }

            $repositoriesBlock

            %s

            dependencies {
                // Uses latest Room built from tip of tree
                implementation "androidx.room:room-runtime:$roomVersion"
                $processorConfig "androidx.room:room-compiler:$roomVersion"
            }

            android {
                namespace "room.testapp"
                compileOptions {
                  sourceCompatibility = JavaVersion.VERSION_1_8
                  targetCompatibility = JavaVersion.VERSION_1_8
                }
            }

            $kotlinJvmTargetBlock

            room {
                schemaDirectory("${'$'}projectDir/schemas")
            }

            """
                .trimMargin()
                // doing format instead of "$projectSetup.androidProject" on purpose,
                // because otherwise trimIndent will mess with formatting
                .format(projectSetup.androidProject)

        )
    }

    @Test
    fun testWorkflow() {
        setup("simple-project")

        // First clean build, all tasks need to run
        runGradleTasks(CLEAN_TASK, COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.SUCCESS)
        }

        // Schema file at version 1 is created
        var schemaOneTimestamp: Long
        projectSetup.rootDir.resolve("schemas/debug/room.testapp.MyDatabase/1.json").let {
            assertThat(it.exists()).isTrue()
            schemaOneTimestamp = it.lastModified()
        }

        // Incremental build, compile task re-runs because schema 1 is used as input, but no copy
        // is done since schema has not changed.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.NO_SOURCE)
        }

        // Incremental build, everything is up to date.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.UP_TO_DATE)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.NO_SOURCE)
        }

        // Make a change that changes the schema at version 1
        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/main/java/room/testapp/MyEntity.java"),
            search = "// Insert-change",
            replace = "public String text;"
        )

        // Incremental build, new schema for version 1 is generated and copied.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.SUCCESS)
        }

        // Check schema file at version 1 is updated
        projectSetup.rootDir.resolve("schemas/debug/room.testapp.MyDatabase/1.json").let {
            assertThat(it.exists()).isTrue()
            assertThat(schemaOneTimestamp).isNotEqualTo(it.lastModified())
            schemaOneTimestamp = it.lastModified()
        }

        // Incremental build, compile task re-runs because schema 1 is used as input (it changed),
        // but no copy is done since schema has not changed.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.NO_SOURCE)
        }

        // Incremental build, everything is up to date.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.UP_TO_DATE)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.NO_SOURCE)
        }

        // Add a new file, it does not change the schema
        projectSetup.rootDir.resolve("src/main/java/room/testapp/NewUtil.java")
            .writeText("""
            package room.testapp;
            public class NewUtil {
            }
            """.trimIndent())

        // Incremental build, compile task re-runs because of new source, but no schema is copied
        // since Room processor didn't even run.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.NO_SOURCE)
        }

        // Incremental build, everything is up to date.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.UP_TO_DATE)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.NO_SOURCE)
        }

        // Change the database version to 2
        val dbFile = if (backend.isForKotlin) "MyDatabase.kt" else "MyDatabase.java"
        searchAndReplace(
            file = projectSetup.rootDir.resolve("src/main/java/room/testapp/$dbFile"),
            search = "version = 1",
            replace = "version = 2"
        )

        // Incremental build, due to the version change a new schema file is generated.
        runGradleTasks(COMPILE_TASK).let { result ->
            result.assertTaskOutcome(COMPILE_TASK, TaskOutcome.SUCCESS)
            result.assertTaskOutcome(COPY_TASK, TaskOutcome.SUCCESS)
        }

        // Check schema file at version 1 is still present and unchanged.
        projectSetup.rootDir.resolve("schemas/debug/room.testapp.MyDatabase/1.json").let {
            assertThat(it.exists()).isTrue()
            assertThat(schemaOneTimestamp).isEqualTo(it.lastModified())
        }

        // Check schema file at version 2 is created and copied.
        projectSetup.rootDir.resolve("schemas/debug/room.testapp.MyDatabase/2.json").let {
            assertThat(it.exists()).isTrue()
        }
    }

    @Test
    fun testFlavoredProject() {
        setup("flavored-project")

        File(projectSetup.rootDir, "build.gradle").appendText(
            """
            android {
                flavorDimensions "mode"
                productFlavors {
                    flavorOne {
                        dimension "mode"
                    }
                    flavorTwo {
                        dimension "mode"
                    }
                }
            }
            """.trimIndent()
        )

        runGradleTasks(
            CLEAN_TASK,
            "compileFlavorOneDebugJavaWithJavac",
            "compileFlavorTwoDebugJavaWithJavac"
        ).let { result ->
            result.assertTaskOutcome(":compileFlavorOneDebugJavaWithJavac", TaskOutcome.SUCCESS)
            result.assertTaskOutcome(":compileFlavorTwoDebugJavaWithJavac", TaskOutcome.SUCCESS)
            result.assertTaskOutcome(":copyRoomSchemasFlavorOneDebug", TaskOutcome.SUCCESS)
            result.assertTaskOutcome(":copyRoomSchemasFlavorTwoDebug", TaskOutcome.SUCCESS)
        }
        // Check schema files are generated for both flavor, each in its own folder.
        val flavorOneSchema = projectSetup.rootDir.resolve(
            "schemas/flavorOneDebug/room.testapp.MyDatabase/1.json"
        )
        val flavorTwoSchema = projectSetup.rootDir.resolve(
            "schemas/flavorTwoDebug/room.testapp.MyDatabase/1.json"
        )
        assertThat(flavorOneSchema.exists()).isTrue()
        assertThat(flavorTwoSchema.exists()).isTrue()
        // Check the schemas in both flavors are different
        assertThat(flavorOneSchema.readText()).isNotEqualTo(flavorTwoSchema.readText())
    }

    @Test
    fun testMoreBuildTypesProject() {
        setup("simple-project")

        File(projectSetup.rootDir, "build.gradle").appendText(
            """
            android {
                buildTypes {
                    staging {
                        initWith debug
                        applicationIdSuffix ".debugStaging"
                    }
                }
            }
            """.trimIndent()
        )

        runGradleTasks(CLEAN_TASK, "compileStagingJavaWithJavac",).let { result ->
            result.assertTaskOutcome(":compileStagingJavaWithJavac", TaskOutcome.SUCCESS)
            result.assertTaskOutcome(":copyRoomSchemasStaging", TaskOutcome.SUCCESS)
        }
        val schemeFile = projectSetup.rootDir.resolve(
            "schemas/staging/room.testapp.MyDatabase/1.json"
        )
        assertThat(schemeFile.exists()).isTrue()
    }

    private fun runGradleTasks(
        vararg args: String,
        projectDir: File = projectSetup.rootDir
    ): BuildResult {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            // workaround for b/231154556
            .withArguments("-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m", *args)
            .build()
    }

    private fun BuildResult.assertTaskOutcome(taskPath: String, outcome: TaskOutcome) {
        assertThat(this.task(taskPath)!!.outcome).isEqualTo(outcome)
    }

    private fun searchAndReplace(file: File, search: String, replace: String) {
        file.writeText(file.readText().replace(search, replace))
    }

    enum class ProcessingBackend(
        val isForKotlin: Boolean
    ) {
        JAVAC(false),
        KAPT(true),
        KSP(true)
    }

    companion object {
        private const val CLEAN_TASK = ":clean"
        private const val COMPILE_TASK = ":compileDebugJavaWithJavac"
        private const val COPY_TASK = ":copyRoomSchemasDebug"
    }
}