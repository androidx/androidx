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

package androidx.benchmark.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class BenchmarkPluginTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var buildToolsVersion: String
    private lateinit var compileSdkVersion: String
    private lateinit var prebuiltsRepo: String
    private lateinit var minSdkVersion: String

    private lateinit var buildFile: File
    private lateinit var propertiesFile: File
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        val stream = BenchmarkPluginTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        prebuiltsRepo = properties.getProperty("prebuiltsRepo")
        compileSdkVersion = properties.getProperty("compileSdkVersion")
        buildToolsVersion = properties.getProperty("buildToolsVersion")
        minSdkVersion = properties.getProperty("minSdkVersion")

        testProjectDir.root.mkdirs()

        val localPropFile = File("../../local.properties")
        localPropFile.copyTo(File(testProjectDir.root, "local.properties"), overwrite = true)

        buildFile = File(testProjectDir.root, "build.gradle")
        buildFile.createNewFile()

        propertiesFile = File(testProjectDir.root, "gradle.properties")
        propertiesFile.writer().use {
            val props = Properties()
            props.setProperty("android.useAndroidX", "true")
            props.setProperty("android.enableJetpack", "true")
            props.store(it, null)
        }

        File("src/test/test-data", "app-project").copyRecursively(testProjectDir.root)

        gradleRunner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
    }

    @Test
    fun applyPluginAppProject() {
        buildFile.writeText(
            """
            plugins {
                id('com.android.application')
                id('androidx.benchmark')
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
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
        """.trimIndent()
        )

        val output = gradleRunner.withArguments("tasks").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
        assertTrue { output.output.contains("benchmarkReport - ") }
    }

    @Test
    fun applyPluginAndroidLibProject() {
        buildFile.writeText(
            """
            plugins {
                id('com.android.library')
                id('androidx.benchmark')
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
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
        """.trimIndent()
        )

        val output = gradleRunner.withArguments("tasks").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
        assertTrue { output.output.contains("benchmarkReport - ") }
    }

    @Test
    fun applyPluginNonAndroidProject() {
        buildFile.writeText(
            """
            plugins {
                id('androidx.benchmark')
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
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
        """.trimIndent()
        )

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("assemble").build()
        }
    }

    @Test
    fun applyPluginNonBenchmarkProject() {
        buildFile.writeText(
            """
            plugins {
                id('com.android.library')
                id('androidx.benchmark')
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
            }
        """.trimIndent()
        )

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("-m", "connectedAndroidTest").build()
        }
    }

    @Test
    fun applyPluginBeforeAndroid() {
        buildFile.writeText(
            """
            plugins {
                id('androidx.benchmark')
                id('com.android.library')
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
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
        """.trimIndent()
        )

        val output = gradleRunner.withArguments("tasks").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
        assertTrue { output.output.contains("benchmarkReport - ") }
    }
}
