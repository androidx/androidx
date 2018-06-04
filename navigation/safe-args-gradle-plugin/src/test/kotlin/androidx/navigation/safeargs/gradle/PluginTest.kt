/*
 * Copyright 2018 The Android Open Source Project
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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

private const val MAIN_DIR = "androidx/navigation/testapp"

private const val NEXT_DIRECTIONS = "$MAIN_DIR/NextFragmentDirections.java"
private const val MAIN_DIRECTIONS = "$MAIN_DIR/MainFragmentDirections.java"
private const val MODIFIED_NEXT_DIRECTIONS = "$MAIN_DIR/ModifiedNextFragmentDirections.java"
private const val ADDITIONAL_DIRECTIONS = "$MAIN_DIR/AdditionalFragmentDirections.java"
private const val FOO_DIRECTIONS = "safe/gradle/test/app/foo/FooFragmentDirections.java"

private const val NAV_RESOURCES = "src/main/res/navigation"
private const val SEC = 1000L

// Does not work in the Android Studio
@RunWith(JUnit4::class)
class PluginTest {

    @Suppress("MemberVisibilityCanBePrivate")
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private var buildFile: File = File("")
    private var compileSdkVersion = ""
    private var buildToolsVersion = ""

    private fun projectRoot(): File = testProjectDir.root

    private fun assertGenerated(name: String) = assertExists(name, true)

    private fun assertNotGenerated(name: String) = assertExists(name, false)

    private fun assertExists(name: String, ex: Boolean): File {
        val generatedFile = File(projectRoot(), "build/$GENERATED_PATH/$name")
        assertThat(generatedFile.exists(), `is`(ex))
        return generatedFile
    }

    private fun navResource(name: String) = File(projectRoot(), "$NAV_RESOURCES/$name")

    private fun gradleBuilder(vararg args: String) = GradleRunner.create()
            .withProjectDir(projectRoot()).withPluginClasspath().withArguments(*args)

    private fun runGradle(vararg args: String) = gradleBuilder(*args).build()
    private fun runAndFailGradle(vararg args: String) = gradleBuilder(*args).buildAndFail()

    @Before
    fun setup() {
        projectRoot().mkdirs()
        buildFile = File(projectRoot(), "build.gradle")
        buildFile.createNewFile()
        // copy local.properties
        val appToolkitProperties = File("../../app-toolkit/local.properties")
        if (appToolkitProperties.exists()) {
            appToolkitProperties.copyTo(File(projectRoot(), "local.properties"), overwrite = true)
        } else {
            File("../../local.properties").copyTo(
                    File(projectRoot(), "local.properties"), overwrite = true)
        }
        val stream = PluginTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        compileSdkVersion = properties.getProperty("compileSdkVersion")
        buildToolsVersion = properties.getProperty("buildToolsVersion")
        testData("app-project").copyRecursively(projectRoot())
    }

    private fun setupSimpleBuildGradle() {
        buildFile.writeText("""
            plugins {
                id('com.android.application')
                id('androidx.navigation.safeargs')
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"
            }
        """.trimIndent())
    }

    @Test
    fun runGenerateTask() {
        buildFile.writeText("""
            plugins {
                id('com.android.application')
                id('androidx.navigation.safeargs')
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"
                flavorDimensions "mode"
                productFlavors {
                    foo {
                        dimension "mode"
                        applicationIdSuffix ".foo"
                    }
                    notfoo {
                        dimension "mode"
                    }

                }
            }
        """.trimIndent())

        runGradle("generateSafeArgsNotfooDebug", "generateSafeArgsFooDebug")
                .assertSuccessfulTask("generateSafeArgsNotfooDebug")
                .assertSuccessfulTask("generateSafeArgsFooDebug")

        assertGenerated("notfoo/debug/$NEXT_DIRECTIONS")
        assertNotGenerated("foo/debug/$NEXT_DIRECTIONS")
        assertGenerated("foo/debug/$FOO_DIRECTIONS")
    }

    @Test
    fun incrementalAdd() {
        setupSimpleBuildGradle()
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val nextLastMod = assertGenerated("debug/$NEXT_DIRECTIONS").lastModified()

        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))

        // lastModified has one second precision on certain platforms and jdk versions
        // so sleep for a second
        Thread.sleep(SEC)
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        assertGenerated("debug/$ADDITIONAL_DIRECTIONS")
        val newNextLastMod = assertGenerated("debug/$NEXT_DIRECTIONS").lastModified()
        assertThat(newNextLastMod, `is`(nextLastMod))
    }

    @Test
    fun incrementalModify() {
        setupSimpleBuildGradle()
        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))

        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val mainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS").lastModified()
        val additionalLastMod = assertGenerated("debug/$ADDITIONAL_DIRECTIONS").lastModified()
        assertGenerated("debug/$NEXT_DIRECTIONS")

        testData("incremental-test-data/modified_nav.xml").copyTo(navResource("nav_test.xml"), true)

        // lastModified has one second precision on certain platforms and jdk versions
        // so sleep for a second
        Thread.sleep(SEC)
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val newMainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS").lastModified()
        // main directions were regenerated
        assertThat(newMainLastMod, not(mainLastMod))

        // but additional directions weren't touched
        val newAdditionalLastMod = assertGenerated("debug/$ADDITIONAL_DIRECTIONS").lastModified()
        assertThat(newAdditionalLastMod, `is`(additionalLastMod))

        assertGenerated("debug/$MODIFIED_NEXT_DIRECTIONS")
        assertNotGenerated("debug/$NEXT_DIRECTIONS")
    }

    @Test
    fun incrementalRemove() {
        setupSimpleBuildGradle()
        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))

        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val mainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS").lastModified()
        assertGenerated("debug/$ADDITIONAL_DIRECTIONS")

        val wasRemoved = navResource("add_nav.xml").delete()
        assertThat(wasRemoved, `is`(true))

        // lastModified has one second precision on certain platforms and jdk versions
        // so sleep for a second
        Thread.sleep(SEC)
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val newMainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS").lastModified()
        // main directions weren't touched
        assertThat(newMainLastMod, `is`(mainLastMod))

        // but additional directions are removed
        assertNotGenerated("debug/$ADDITIONAL_DIRECTIONS")
    }

    @Test
    fun invalidModify() {
        setupSimpleBuildGradle()
        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val step1MainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS").lastModified()
        val step1AdditionalLastMod = assertGenerated("debug/$ADDITIONAL_DIRECTIONS").lastModified()
        assertGenerated("debug/$NEXT_DIRECTIONS")

        testData("invalid/failing_nav.xml").copyTo(navResource("nav_test.xml"), true)
        Thread.sleep(SEC)
        runAndFailGradle("generateSafeArgsDebug").assertFailingTask("generateSafeArgsDebug")
        val step2MainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS").lastModified()
        // main directions were regenerated
        assertThat(step2MainLastMod, not(step1MainLastMod))

        // but additional directions weren't touched
        val step2AdditionalLastMod = assertGenerated("debug/$ADDITIONAL_DIRECTIONS").lastModified()
        assertThat(step2AdditionalLastMod, `is`(step1AdditionalLastMod))

        val step2ModifiedTime = assertGenerated("debug/$MODIFIED_NEXT_DIRECTIONS").lastModified()
        assertNotGenerated("debug/$NEXT_DIRECTIONS")

        testData("incremental-test-data/modified_nav.xml").copyTo(navResource("nav_test.xml"), true)
        Thread.sleep(SEC)
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")

        // additional directions are touched because once task failed,
        // gradle next time makes full run
        val step3AdditionalLastMod = assertGenerated("debug/$ADDITIONAL_DIRECTIONS").lastModified()
        assertThat(step3AdditionalLastMod, not(step2AdditionalLastMod))

        val step3ModifiedTime = assertGenerated("debug/$MODIFIED_NEXT_DIRECTIONS").lastModified()
        assertThat(step2ModifiedTime, not(step3ModifiedTime))
    }
}

private fun testData(name: String) = File("src/test/test-data", name)

private fun BuildResult.assertSuccessfulTask(name: String): BuildResult {
    assertThat(task(":$name")!!.outcome, `is`(TaskOutcome.SUCCESS))
    return this
}

private fun BuildResult.assertFailingTask(name: String): BuildResult {
    assertThat(task(":$name")!!.outcome, `is`(TaskOutcome.FAILED))
    return this
}