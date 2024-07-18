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

import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import androidx.baselineprofile.gradle.utils.Module
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfileWrapperPluginTest {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule()

    @Test
    fun testWrapperGeneratingForApplication() {
        projectSetup.consumer.setBuildGradle(
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile")
                }
                android { namespace 'com.example.namespace.test' }
                dependencies { baselineProfile(project(":${projectSetup.producer.name}")) }

                $taskPrintPlugins
            """.trimIndent()
        )
        projectSetup.producer.setBuildGradle(
            """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile")
                }
                android {
                    targetProjectPath = ":${projectSetup.consumer.name}"
                    namespace 'com.example.namespace.test'
                }

                $taskPrintPlugins
            """.trimIndent()
        )

        projectSetup.consumer.printPluginsAndAssertOutput {
            contains("class $CLASS_APP_TARGET_PLUGIN")
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
        projectSetup.producer.printPluginsAndAssertOutput {
            contains("class $CLASS_PRODUCER_PLUGIN")
        }
    }

    @Test
    fun testWrapperGeneratingForLibraries() {
        projectSetup.appTarget.setBuildGradle(
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile")
                }
                android { namespace 'com.example.namespace.test' }

                $taskPrintPlugins
            """.trimIndent()
        )
        projectSetup.consumer.setBuildGradle(
            """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofile")
                }
                android { namespace 'com.example.namespace.test' }
                dependencies { baselineProfile(project(":${projectSetup.producer.name}")) }

                $taskPrintPlugins
            """.trimIndent()
        )
        projectSetup.producer.setBuildGradle(
            """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile")
                }
                android {
                    targetProjectPath = ":${projectSetup.consumer.name}"
                    namespace 'com.example.namespace.test'
                }

                $taskPrintPlugins
            """.trimIndent()
        )

        projectSetup.appTarget.printPluginsAndAssertOutput {
            contains("class $CLASS_APP_TARGET_PLUGIN")
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
        projectSetup.producer.printPluginsAndAssertOutput {
            contains("class $CLASS_PRODUCER_PLUGIN")
        }
        projectSetup.consumer.printPluginsAndAssertOutput {
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
    }

    private fun Module.printPluginsAndAssertOutput(
        assertBlock: IterableSubject.() -> (Unit)
    ) {
        val output = gradleRunner
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
tasks.register("printPlugins", PrintTask) { t ->
    def pluginsList = project.plugins.collect { it.class.toString() }.join("\n")
    t.text.set(pluginsList)
}
""".trimIndent()
