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

package android.arch.navigation.safeargs.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

// Does not work in the Android Studio
@RunWith(JUnit4::class)
class PluginTest {

    @Suppress("MemberVisibilityCanPrivate")
    @get:Rule
    val testProjectDir = TemporaryFolder()

    private var buildFile: File = File("")
    private var compileSdkVersion = ""
    private var buildToolsVersion = ""

    private fun projectRoot(): File = testProjectDir.root

    @Before
    fun setup() {
        projectRoot().mkdirs()
        buildFile = File(projectRoot(), "build.gradle")
        buildFile.createNewFile()
        // copy local.properties
        File("../../app-toolkit/local.properties").copyTo(File(projectRoot(),
                "local.properties"), overwrite = true)
        val stream = PluginTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        compileSdkVersion = properties.getProperty("compileSdkVersion")
        buildToolsVersion = properties.getProperty("buildToolsVersion")
    }

    @Test
    fun runGenerateTask() {
        File("src/test/test-data/app-project/").copyRecursively(projectRoot())
        buildFile.writeText("""
            plugins {
                id('com.android.application')
                id('android.arch.navigation.safeargs')
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"
            }
        """.trimIndent())

        val result = GradleRunner.create().withProjectDir(projectRoot())
                .withPluginClasspath()
                .withDebug(true)
                .withArguments("generateSafeArgsDebug").build()
        assertThat(result.task(":generateSafeArgsDebug")!!.outcome, `is`(TaskOutcome.SUCCESS))
    }
}