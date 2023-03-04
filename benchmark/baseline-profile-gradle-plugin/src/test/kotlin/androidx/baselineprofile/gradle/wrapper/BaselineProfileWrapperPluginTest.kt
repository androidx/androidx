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

package androidx.baselineprofile.gradle.wrapper

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfileWrapperPluginTest {

    private val rootFolder = TemporaryFolder().also { it.create() }

    @get:Rule
    val appTargetProjectSetup = ProjectSetupRule(rootFolder.root)
    @get:Rule
    val consumerProjectSetup = ProjectSetupRule(rootFolder.root)
    @get:Rule
    val producerProjectSetup = ProjectSetupRule(rootFolder.root)

    private lateinit var appTargetModuleName: String
    private lateinit var producerModuleName: String
    private lateinit var consumerModuleName: String

    @Before
    fun setUp() {
        appTargetModuleName = appTargetProjectSetup.rootDir.relativeTo(rootFolder.root).name
        producerModuleName = consumerProjectSetup.rootDir.relativeTo(rootFolder.root).name
        consumerModuleName = producerProjectSetup.rootDir.relativeTo(rootFolder.root).name

        rootFolder.newFile("settings.gradle").writeText(
            """
            include '$appTargetModuleName'
            include '$producerModuleName'
            include '$consumerModuleName'
        """.trimIndent()
        )
    }

    @Test
    fun testWrapperGeneratingForApplication() {
        consumerProjectSetup.setupProject(
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile")
                }
                android { namespace 'com.example.namespace.test' }
                dependencies { baselineProfile(project(":$producerModuleName")) }
            """.trimIndent()
        )
        producerProjectSetup.setupProject(
            """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile")
                }
                android {
                    targetProjectPath = ":$consumerModuleName"
                    namespace 'com.example.namespace.test'
                }
            """.trimIndent()
        )

        consumerProjectSetup.printPluginsAndAssertOutput {
            contains("class $CLASS_APP_TARGET_PLUGIN")
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
        producerProjectSetup.printPluginsAndAssertOutput {
            contains("class $CLASS_PRODUCER_PLUGIN")
        }
    }

    @Test
    fun testWrapperGeneratingForLibraries() {
        appTargetProjectSetup.setupProject(
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile")
                }
                android { namespace 'com.example.namespace.test' }
            """.trimIndent()
        )
        consumerProjectSetup.setupProject(
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile")
                }
                android { namespace 'com.example.namespace.test' }
                dependencies { baselineProfile(project(":$producerModuleName")) }
            """.trimIndent()
        )
        producerProjectSetup.setupProject(
            """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile")
                }
                android {
                    targetProjectPath = ":$consumerModuleName"
                    namespace 'com.example.namespace.test'
                }
            """.trimIndent()
        )

        appTargetProjectSetup.printPluginsAndAssertOutput {
            contains("class $CLASS_APP_TARGET_PLUGIN")
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
        producerProjectSetup.printPluginsAndAssertOutput {
            contains("class $CLASS_PRODUCER_PLUGIN")
        }
        consumerProjectSetup.printPluginsAndAssertOutput {
            contains("class $CLASS_APP_TARGET_PLUGIN")
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
    }

    private fun ProjectSetupRule.setupProject(buildGradleContent: String) {
        writeDefaultBuildGradle(
            prefix = """
                $buildGradleContent

                $taskPrintPlugins
            """.trimIndent(),
            suffix = ""
        )
    }

    private fun ProjectSetupRule.printPluginsAndAssertOutput(
        assertBlock: IterableSubject.() -> (Unit)
    ) {
        val output = GradleRunner
            .create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("printPlugins", "--stacktrace")
            .build()
            .output
            .lines()
        assertBlock(assertThat(output))
    }
}

private const val CLASS_CONSUMER_PLUGIN =
    "androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerPlugin"
private const val CLASS_APP_TARGET_PLUGIN =
    "androidx.baselineprofile.gradle.apptarget.BaselineProfileAppTargetPlugin"
private const val CLASS_PRODUCER_PLUGIN =
    "androidx.baselineprofile.gradle.producer.BaselineProfileProducerPlugin"

private val taskPrintPlugins = """
tasks.register("printPlugins") {
    project.plugins.each { println(it.class) }
}
""".trimIndent()