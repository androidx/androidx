/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.safeargs.gradle

import androidx.testutils.gradle.ProjectSetupRule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import java.io.File

internal const val MAIN_DIR = "androidx/navigation/testapp"

internal const val NEXT_DIRECTIONS = "$MAIN_DIR/NextFragmentDirections"
internal const val FOO_NEXT_DIRECTIONS = "$MAIN_DIR/foo/NextFragmentDirections"
internal const val NEXT_ARGUMENTS = "$MAIN_DIR/NextFragmentArgs"
internal const val FOO_NEXT_ARGUMENTS = "$MAIN_DIR/foo/NextFragmentArgs"
internal const val MAIN_DIRECTIONS = "$MAIN_DIR/MainFragmentDirections"
internal const val FOO_MAIN_DIRECTIONS = "$MAIN_DIR/foo/MainFragmentDirections"
internal const val MODIFIED_NEXT_DIRECTIONS = "$MAIN_DIR/ModifiedNextFragmentDirections"
internal const val ADDITIONAL_DIRECTIONS = "$MAIN_DIR/AdditionalFragmentDirections"
internal const val FOO_DIRECTIONS = "$MAIN_DIR/foo/FooFragmentDirections"
internal const val FEATURE_DIRECTIONS = "$MAIN_DIR/FeatureFragmentDirections"
internal const val LIBRARY_DIRECTIONS = "$MAIN_DIR/LibraryFragmentDirections"
internal const val FOO_DYNAMIC_DIRECTIONS =
    "safe/gradle/test/app/safe/DynFooFeatureFragmentDirections"
internal const val NOTFOO_DYNAMIC_DIRECTIONS = "$MAIN_DIR/DynFeatureFragmentDirections"

internal const val NAV_RESOURCES = "src/main/res/navigation"
internal const val SEC = 1000L

abstract class BasePluginTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()

    internal fun projectRoot(): File = projectSetup.rootDir

    internal fun assertGenerated(name: String, prefix: String? = null): File {
        return prefix?.let { assertExists(name, true, it) } ?: assertExists(name, true)
    }

    internal fun assertNotGenerated(name: String, prefix: String? = null): File {
        return prefix?.let { assertExists(name, false, it) } ?: assertExists(name, false)
    }

    internal fun assertExists(name: String, ex: Boolean, prefix: String = ""): File {
        val generatedFile = File(
            projectRoot(),
            "${prefix}build/$GENERATED_PATH/$name"
        )
        assertThat(
            generatedFile.exists(),
            CoreMatchers.`is`(ex)
        )
        return generatedFile
    }

    internal fun navResource(name: String) =
        File(projectRoot(), "$NAV_RESOURCES/$name")

    internal fun gradleBuilder(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectRoot()).withPluginClasspath()
        // b/175897186 set explicit metaspace size in hopes of fewer crashes
        .withArguments("-Dorg.gradle.jvmargs=-XX:MaxMetaspaceSize=512m", *args)

    internal fun runGradle(vararg args: String) = gradleBuilder(*args).build()
    internal fun runAndFailGradle(vararg args: String) = gradleBuilder(*args).buildAndFail()

    internal fun setupSimpleBuildGradle() {
        testData("app-project").copyRecursively(projectRoot())
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id('com.android.application')
                    id('androidx.navigation.safeargs')
                }
            """.trimIndent(),
            suffix = """
                android {
                    namespace 'androidx.navigation.testapp'
                }
                dependencies {
                    implementation "${projectSetup.props.navigationRuntime}"
                }
            """.trimIndent()
        )
    }

    internal fun setupMultiModuleBuildGradle() {
        testData("multimodule-project").copyRecursively(projectRoot())
        val repositoriesBlock = buildString {
            appendLine("repositories {")
            projectSetup.allRepositoryPaths.forEach {
                appendLine("""maven { url "$it" }""")
            }
            appendLine("}")
        }
        val props = projectSetup.props
        projectSetup.buildFile.writeText(
            """
            buildscript {
                ext.compileSdk = ${props.compileSdkVersion}
                ext.buildTools = "${props.buildToolsVersion}"
                ext.minSdk = ${props.minSdkVersion}
                ext.debugKeystoreFile = "${props.debugKeystore}"
                ext.navigationCommonDep = "${props.navigationRuntime}"
            }

            allprojects {
                $repositoriesBlock
            }
            """.trimIndent()
        )
    }

    internal fun setupSimpleKotlinBuildGradle() {
        testData("app-project-kotlin").copyRecursively(projectRoot())
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id('com.android.application')
                    id('kotlin-android')
                    id('androidx.navigation.safeargs.kotlin')
                }
            """.trimIndent(),
            suffix = """
                android {
                    namespace 'androidx.navigation.testapp'
                }
                dependencies {
                    implementation "${projectSetup.props.kotlinStblib}"
                    implementation "${projectSetup.props.navigationRuntime}"
                }
            """.trimIndent()
        )
    }
}

internal fun testData(name: String) = File("src/test/test-data", name)

internal fun BuildResult.assertSuccessfulTask(name: String): BuildResult {
    assertThat(task(":$name")!!.outcome, CoreMatchers.`is`(TaskOutcome.SUCCESS))
    return this
}

internal fun BuildResult.assertFailingTask(name: String): BuildResult {
    assertThat(task(":$name")!!.outcome, CoreMatchers.`is`(TaskOutcome.FAILED))
    return this
}
