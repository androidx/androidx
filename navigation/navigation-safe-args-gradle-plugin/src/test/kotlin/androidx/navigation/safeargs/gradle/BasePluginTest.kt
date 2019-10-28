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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties

internal const val MAIN_DIR = "androidx/navigation/testapp"

internal const val NEXT_DIRECTIONS = "$MAIN_DIR/NextFragmentDirections"
internal const val NEXT_ARGUMENTS = "$MAIN_DIR/NextFragmentArgs"
internal const val MAIN_DIRECTIONS = "$MAIN_DIR/MainFragmentDirections"
internal const val MODIFIED_NEXT_DIRECTIONS = "$MAIN_DIR/ModifiedNextFragmentDirections"
internal const val ADDITIONAL_DIRECTIONS = "$MAIN_DIR/AdditionalFragmentDirections"
internal const val FOO_DIRECTIONS = "$MAIN_DIR/foo/FooFragmentDirections"
internal const val FEATURE_DIRECTIONS = "$MAIN_DIR/FeatureFragmentDirections"
internal const val LIBRARY_DIRECTIONS = "$MAIN_DIR/LibraryFragmentDirections"
internal const val FOO_DYNAMIC_DIRECTIONS =
    "safe/gradle/test/app/safe/app/foo/DynFooFeatureFragmentDirections"
internal const val NOTFOO_DYNAMIC_DIRECTIONS = "$MAIN_DIR/DynFeatureFragmentDirections"

internal const val NAV_RESOURCES = "src/main/res/navigation"
internal const val SEC = 1000L

abstract class BasePluginTest {
    @Suppress("MemberVisibilityCanBePrivate")
    @get:Rule
    val testProjectDir = TemporaryFolder()

    internal var buildFile: File = File("")
    internal var prebuiltsRepo = ""
    internal var compileSdkVersion = ""
    internal var buildToolsVersion = ""
    internal var minSdkVersion = ""
    internal var debugKeystore = ""
    internal var navigationCommon = ""
    internal var kotlinStblib = ""

    internal fun projectRoot(): File = testProjectDir.root

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
        MatcherAssert.assertThat(
            generatedFile.exists(),
            CoreMatchers.`is`(ex)
        )
        return generatedFile
    }

    internal fun navResource(name: String) =
        File(projectRoot(), "$NAV_RESOURCES/$name")

    internal fun gradleBuilder(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectRoot()).withPluginClasspath().withArguments(*args)

    internal fun runGradle(vararg args: String) = gradleBuilder(*args).build()
    internal fun runAndFailGradle(vararg args: String) = gradleBuilder(*args).buildAndFail()

    @Before
    fun setup() {
        projectRoot().mkdirs()
        buildFile = File(projectRoot(), "build.gradle")
        buildFile.createNewFile()
        // copy local.properties
        val appToolkitProperties = File("../../local.properties")
        if (appToolkitProperties.exists()) {
            appToolkitProperties.copyTo(
                File(projectRoot(), "local.properties"),
                overwrite = true
            )
        } else {
            File("../../local.properties").copyTo(
                File(projectRoot(), "local.properties"), overwrite = true
            )
        }
        val stream = BasePluginTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        prebuiltsRepo = properties.getProperty("prebuiltsRepo")
        compileSdkVersion = properties.getProperty("compileSdkVersion")
        buildToolsVersion = properties.getProperty("buildToolsVersion")
        minSdkVersion = properties.getProperty("minSdkVersion")
        debugKeystore = properties.getProperty("debugKeystore")
        navigationCommon = properties.getProperty("navigationCommon")
        kotlinStblib = properties.getProperty("kotlinStdlib")

        val propertiesFile = File(projectRoot(), "gradle.properties")
        propertiesFile.writer().use {
            val props = Properties()
            props.setProperty("android.useAndroidX", "true")
            props.store(it, null)
        }
    }

    internal fun setupSimpleBuildGradle() {
        testData("app-project").copyRecursively(projectRoot())
        buildFile.writeText("""
            plugins {
                id('com.android.application')
                id('androidx.navigation.safeargs')
            }

            repositories {
                maven { url "$prebuiltsRepo/androidx/external" }
                maven { url "$prebuiltsRepo/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                }

                signingConfigs {
                    debug {
                        storeFile file("$debugKeystore")
                    }
                }
            }

            dependencies {
                implementation "$navigationCommon"
            }
        """.trimIndent()
        )
    }

    internal fun setupMultiModuleBuildGradle() {
        testData("multimodule-project").copyRecursively(projectRoot())
        buildFile.writeText("""
            buildscript {
                ext.compileSdk = $compileSdkVersion
                ext.buildTools = "$buildToolsVersion"
                ext.minSdk = $minSdkVersion
                ext.debugKeystoreFile = "$debugKeystore"
                ext.navigationCommonDep = "$navigationCommon"
            }

            allprojects {
                repositories {
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven { url "$prebuiltsRepo/androidx/internal" }
                }
            }
        """.trimIndent()
        )
    }

    internal fun setupSimpleKotlinBuildGradle() {
        testData("app-project-kotlin").copyRecursively(projectRoot())
        buildFile.writeText("""
            plugins {
                id('com.android.application')
                id('kotlin-android')
                id('androidx.navigation.safeargs.kotlin')
            }

            repositories {
                maven { url "$prebuiltsRepo/androidx/external" }
                maven { url "$prebuiltsRepo/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                }

                signingConfigs {
                    debug {
                        storeFile file("$debugKeystore")
                    }
                }
            }

            dependencies {
                implementation "$kotlinStblib"
                implementation "$navigationCommon"
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