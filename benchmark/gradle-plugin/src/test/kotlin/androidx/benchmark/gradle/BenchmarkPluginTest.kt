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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class BenchmarkPluginTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var buildToolsVersion: String
    private lateinit var compileSdkVersion: String
    private lateinit var prebuiltsRoot: String
    private lateinit var minSdkVersion: String

    private lateinit var buildFile: File
    private lateinit var propertiesFile: File
    private lateinit var versionPropertiesFile: File
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        val stream = BenchmarkPluginTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        prebuiltsRoot = properties.getProperty("prebuiltsRoot")
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

        versionPropertiesFile = File(testProjectDir.root, "version.properties")
        versionPropertiesFile.createNewFile()

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
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
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
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
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
    }

    @Test
    fun applyPluginNonAndroidProject() {
        buildFile.writeText(
            """
            plugins {
                id('androidx.benchmark')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
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
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
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

        val output = gradleRunner.withArguments("tasks").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
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
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
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
    }

    @Test
    fun applyPluginOnAgp36() {
        buildFile.writeText(
            """
            plugins {
                id('androidx.benchmark')
                id('com.android.library')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                    testInstrumentationRunnerArguments additionalTestOutputDir: "/fake_path/files"
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }

            tasks.register("printTestBuildType") {
                println android.testBuildType
            }
        """.trimIndent()
        )

        propertiesFile.appendText("android.enableAdditionalTestOutput=true")
        versionPropertiesFile.writeText("buildVersion=3.6.0-alpha05")

        val output = gradleRunner.withArguments("tasks").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }

        // Should depend on AGP to pull benchmark reports via additionalTestOutputDir.
        assertFalse { output.output.contains("benchmarkReport - ") }

        val testBuildTypeOutput = gradleRunner.withArguments("printTestBuildType").build()
        assertTrue { testBuildTypeOutput.output.contains("release") }
    }

    @Test
    fun applyPluginOnAgp35() {
        buildFile.writeText(
            """
            plugins {
                id('androidx.benchmark')
                id('com.android.library')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                    testInstrumentationRunnerArguments.remove("additionalTestOutputDir")
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"

            }

            tasks.register("printInstrumentationArgs") {
                println android.defaultConfig.testInstrumentationRunnerArguments
            }

            tasks.register("printTestBuildType") {
                println android.testBuildType
            }
        """.trimIndent()
        )

        versionPropertiesFile.writeText("buildVersion=3.5.0-rc03")

        val output = gradleRunner.withArguments("tasks").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }

        // Should try to pull benchmark reports via legacy BenchmarkPlugin code path.
        assertTrue { output.output.contains("benchmarkReport - ") }

        val argsOutput = gradleRunner.withArguments("printInstrumentationArgs").build()
        assertTrue { argsOutput.output.contains("no-isolated-storage:1") }

        val testBuildTypeOutput = gradleRunner.withArguments("printTestBuildType").build()
        assertTrue { testBuildTypeOutput.output.contains("release") }
    }

    @Test
    fun applyPluginDefaultAgpProperties() {
        buildFile.writeText(
            """
            import com.android.build.gradle.TestedExtension

            plugins {
                id('com.android.library')
                id('androidx.benchmark')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
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

            tasks.register("printTestInstrumentationRunner") {
                println android.defaultConfig.testInstrumentationRunner
            }

            tasks.register("printTestCoverageEnabled") {
                def extension = project.extensions.getByType(TestedExtension)
                println extension.buildTypes.getByName("debug").testCoverageEnabled
            }
        """.trimIndent()
        )

        val runnerOutput = gradleRunner.withArguments("printTestInstrumentationRunner").build()
        assertTrue {
            runnerOutput.output.contains("androidx.benchmark.junit4.AndroidBenchmarkRunner")
        }

        val codeCoverageOutput = gradleRunner.withArguments("printTestCoverageEnabled").build()
        assertTrue { codeCoverageOutput.output.contains("false") }
    }

    @Test
    fun applyPluginOverrideAgpProperties() {
        buildFile.writeText(
            """
            import com.android.build.gradle.TestedExtension

            plugins {
                id('com.android.library')
                id('androidx.benchmark')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                buildTypes {
                    debug {
                        testCoverageEnabled = true
                    }
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"

            }

            tasks.register("printTestInstrumentationRunner") {
                println android.defaultConfig.testInstrumentationRunner
            }

            tasks.register("printTestCoverageEnabled") {
                def extension = project.extensions.getByType(TestedExtension)
                println extension.buildTypes.getByName("debug").testCoverageEnabled
            }
        """.trimIndent()
        )

        val runnerOutput = gradleRunner.withArguments("printTestInstrumentationRunner").build()
        assertTrue {
            runnerOutput.output.contains("androidx.test.runner.AndroidJUnitRunner")
        }

        val codeCoverageOutput = gradleRunner.withArguments("printTestCoverageEnabled").build()
        assertTrue { codeCoverageOutput.output.contains("true") }
    }

    @Test
    fun applyPluginAndroidOldRunner36() {
        buildFile.writeText(
            """
            plugins {
                id('com.android.library')
                id('androidx.benchmark')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                    testInstrumentationRunner "androidx.benchmark.AndroidBenchmarkRunner"
                    testInstrumentationRunnerArguments additionalTestOutputDir: "/fake_path/files"
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha04"
            }
        """.trimIndent()
        )

        propertiesFile.appendText("android.enableAdditionalTestOutput=true")

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("assemble").build()
        }
    }

    @Test
    fun applyPluginAndroidOldRunner35() {
        buildFile.writeText(
            """
            plugins {
                id('com.android.library')
                id('androidx.benchmark')
            }

            repositories {
                maven { url "$prebuiltsRoot/androidx/external" }
                maven { url "$prebuiltsRoot/androidx/internal" }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                    testInstrumentationRunner "androidx.benchmark.AndroidBenchmarkRunner"
                    testInstrumentationRunnerArguments.remove("additionalTestOutputDir")
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha04"
            }
        """.trimIndent()
        )

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("assemble").build()
        }
    }
}
