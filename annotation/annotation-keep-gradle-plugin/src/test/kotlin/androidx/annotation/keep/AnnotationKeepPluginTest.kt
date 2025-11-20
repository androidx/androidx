/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.annotation.keep

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test

class AnnotationKeepPluginTest {

    @get:Rule val projectSetup = ProjectSetupRule()

    @Test
    fun basicUsageTest() {
        applyPlugin()
    }

    private fun applyPlugin() {
        // Runs ./gradlew tasks after the Plugin has been applied.
        setup()
        // There is some ambiguity when using the `withPluginClassPath` API.
        // Implementation details of the Plugin might end up overriding things that are added to
        // The plugins {} block. But, this is acceptable for the test given our implementation
        // does not do those atypical things.
        @Suppress("WithPluginClasspathUsage")
        GradleRunner.create()
            .withProjectDir(/* projectDir= */ projectSetup.rootDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()
    }

    private fun setup() {
        val projectRoot = projectSetup.rootDir
        // Copy Fixture
        val projectDirectory = File("$TEST_DATA/basic-keep-plugin-example")
        projectDirectory.copyRecursively(target = projectRoot)
        // Repositories Block
        val resolvers =
            projectSetup.allRepositoryPaths.joinToString(separator = System.lineSeparator()) {
                """
                    |maven {
                    | url "$it"
                    |}
                """
                    .trimMargin()
            }
        val repositories =
            """
            |repositories {
            |  $resolvers
            |}
            """
                .trimMargin()

        val buildScript =
            """
                |plugins {
                |  id("com.android.library")
                |  id("androidx.annotation.keep")
                |}
                |
                |$repositories
                |${projectSetup.androidProject}
                |
                |dependencies {
                |  // TODO: We need to be able to provision a dependency here.
                |  // This has not been published yet, so we need to do that first.
                |  // implementation(project(":annotation:annotation-keep"))
                |}
                |
                |android {
                |  namespace = "androidx.keep.annotation.plugin.example"
                |}
                """
                .trimMargin()

        // Write build.gradle
        File(projectRoot, "build.gradle").writeText(buildScript)
    }

    companion object {
        private const val TEST_DATA = "src/test/testData"
    }
}
