/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.testutils.gradle

import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.Properties

/**
 * Test rule that helps to setup android project in tests that run gradle.
 *
 * It should be used along side with SdkResourceGenerator in your build.gradle file
 */
class ProjectSetupRule : ExternalResource() {
    val testProjectDir = TemporaryFolder()

    lateinit var props: ProjectProps

    val rootDir: File
        get() = testProjectDir.root

    val buildFile: File
        get() = File(rootDir, "build.gradle")

    val gradlePropertiesFile: File
        get() = File(rootDir, "gradle.properties")

    /**
     * Combined list of local build repo and remote repositories (prebuilts etc).
     * Local build repo is the first in line to ensure it is prioritized.
     */
    val allRepositoryPaths: List<String> by lazy {
        listOf(props.localSupportRepo) + props.repositoryUrls
    }

    private val repositories: String
        get() = buildString {
            appendLine("repositories {")
            props.repositoryUrls.forEach {
                appendLine("    maven { url '$it' }")
            }
            appendLine("}")
        }

    val androidProject: String
        get() = """
            android {
                compileSdkVersion ${props.compileSdkVersion}
                buildToolsVersion "${props.buildToolsVersion}"

                defaultConfig {
                    minSdkVersion ${props.minSdkVersion}
                }

                signingConfigs {
                    debug {
                        storeFile file("${props.debugKeystore}")
                    }
                }
            }
        """.trimIndent()

    private val defaultBuildGradle: String
        get() = "\n$repositories\n\n$androidProject\n\n"

    fun writeDefaultBuildGradle(prefix: String, suffix: String) {
        buildFile.writeText(prefix)
        buildFile.appendText(defaultBuildGradle)
        buildFile.appendText(suffix)
    }

    override fun apply(base: Statement, description: Description): Statement {
        return testProjectDir.apply(super.apply(base, description), description)
    }

    override fun before() {
        props = ProjectProps.load()
        buildFile.createNewFile()
        copyLocalProperties()
        writeGradleProperties()
    }

    private fun copyLocalProperties() {
        val localProperties = File(props.rootProjectPath, "local.properties")
        if (localProperties.exists()) {
            localProperties.copyTo(File(rootDir, "local.properties"), overwrite = true)
        } else {
            throw IllegalStateException("local.properties doesn't exist at: $localProperties")
        }
    }

    private fun writeGradleProperties() {
        gradlePropertiesFile.writer().use {
            val props = Properties()
            props.setProperty("android.useAndroidX", "true")
            props.store(it, null)
        }
    }
}

data class ProjectProps(
    val prebuiltsRoot: String,
    val compileSdkVersion: String,
    val buildToolsVersion: String,
    val minSdkVersion: String,
    val debugKeystore: String,
    var navigationRuntime: String,
    val kotlinStblib: String,
    val kotlinVersion: String,
    val kspVersion: String,
    val rootProjectPath: String,
    val localSupportRepo: String,
    val agpDependency: String,
    val repositoryUrls: List<String>
) {
    companion object {
        fun load(): ProjectProps {
            val stream = ProjectSetupRule::class.java.classLoader.getResourceAsStream("sdk.prop")
            val properties = Properties()
            properties.load(stream)
            return ProjectProps(
                prebuiltsRoot = properties.getProperty("prebuiltsRoot"),
                compileSdkVersion = properties.getProperty("compileSdkVersion"),
                buildToolsVersion = properties.getProperty("buildToolsVersion"),
                minSdkVersion = properties.getProperty("minSdkVersion"),
                debugKeystore = properties.getProperty("debugKeystore"),
                navigationRuntime = properties.getProperty("navigationRuntime"),
                kotlinStblib = properties.getProperty("kotlinStdlib"),
                kotlinVersion = properties.getProperty("kotlinVersion"),
                kspVersion = properties.getProperty("kspVersion"),
                rootProjectPath = properties.getProperty("rootProjectPath"),
                localSupportRepo = properties.getProperty("localSupportRepo"),
                agpDependency = properties.getProperty("agpDependency"),
                repositoryUrls = properties.getProperty("repositoryUrls").split(",")
            )
        }
    }
}
