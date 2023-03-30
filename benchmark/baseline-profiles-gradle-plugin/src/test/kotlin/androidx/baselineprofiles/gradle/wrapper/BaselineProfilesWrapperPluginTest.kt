/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.baselineprofiles.gradle.wrapper

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
class BaselineProfilesWrapperPluginTest {

    private val rootFolder = TemporaryFolder().also { it.create() }

    @get:Rule
    val projectSetup = ProjectSetupRule(rootFolder.root)

    // This additional application project setup is needed to test `com.android.test`, because this
    // requires specifying `targetProjectPath` with a valid `com.android.application` module.
    @get:Rule
    val applicationProjectSetup = ProjectSetupRule(rootFolder.root)

    private lateinit var moduleName: String
    private lateinit var applicationModuleName: String
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        moduleName = projectSetup.rootDir.relativeTo(rootFolder.root).name
        applicationModuleName = applicationProjectSetup.rootDir.relativeTo(rootFolder.root).name

        rootFolder.newFile("settings.gradle").writeText(
            """
            include '$moduleName'
            include '$applicationModuleName'
        """.trimIndent()
        )
        gradleRunner = GradleRunner.create()
            .withProjectDir(projectSetup.rootDir)
            .withPluginClasspath()
    }

    @Test
    fun testWrapperAppliedToLibraryModule() {
        setupProjectAndAssertOnPrintPluginsOutput(
            buildGradleContent = """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofiles")
                }
                android {
                    namespace 'com.example.namespace'
                }
            """.trimIndent()
        ) {
            contains("class $CLASS_CONSUMER_PLUGIN")
        }
    }

    @Test
    fun testWrapperAppliedToApplicationModule() {
        setupProjectAndAssertOnPrintPluginsOutput(
            buildGradleContent = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofiles")
                }
                android {
                    namespace 'com.example.namespace'
                }
            """.trimIndent()
        ) {
            containsAtLeast(
                "class $CLASS_CONSUMER_PLUGIN",
                "class $CLASS_APK_PROVIDER_PLUGIN"
            )
        }
    }

    @Test
    fun testWrapperAppliedToTestModule() {
        applicationProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofiles")
                }
                android {
                    namespace 'com.example.namespace.test'
                }
            """.trimIndent(),
            suffix = ""
        )

        setupProjectAndAssertOnPrintPluginsOutput(
            buildGradleContent = """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofiles")
                }
                android {
                    targetProjectPath = ":$applicationModuleName"
                    namespace 'com.example.namespace.test'
                }
            """.trimIndent()
        ) {
            contains(
                "class $CLASS_PRODUCER_PLUGIN"
            )
        }
    }

    private fun setupProjectAndAssertOnPrintPluginsOutput(
        buildGradleContent: String,
        assertBlock: IterableSubject.() -> (Unit)
    ) {
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                $buildGradleContent

                $taskPrintPlugins
            """.trimIndent(),
            suffix = ""
        )

        val output = gradleRunner
            .withArguments("printPlugins", "--stacktrace")
            .build()
            .output
            .lines()

        assertBlock(assertThat(output))
    }
}

private const val CLASS_CONSUMER_PLUGIN =
    "androidx.baselineprofiles.gradle.consumer.BaselineProfilesConsumerPlugin"
private const val CLASS_APK_PROVIDER_PLUGIN =
    "androidx.baselineprofiles.gradle.apkprovider.BaselineProfilesApkProviderPlugin"
private const val CLASS_PRODUCER_PLUGIN =
    "androidx.baselineprofiles.gradle.producer.BaselineProfilesProducerPlugin"

private val taskPrintPlugins = """
tasks.register("printPlugins") {
    project.plugins.each { println(it.class) }
}
""".trimIndent()