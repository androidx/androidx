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

package androidx.build.playground

import androidx.build.playground.VerifyPlaygroundGradleConfigurationTask.Companion.exceptedProperties
import androidx.build.playground.VerifyPlaygroundGradleConfigurationTask.Companion.ignoredProperties
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.google.common.annotations.VisibleForTesting
import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * Compares the playground Gradle configuration with the main androidx Gradle configuration to
 * ensure playgrounds do not define any property in their own build that conflicts with the main
 * build.
 */
@CacheableTask
abstract class VerifyPlaygroundGradleConfigurationTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidxProperties: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val playgroundProperties: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidxGradleWrapper: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val playgroundGradleWrapper: RegularFileProperty

    @TaskAction
    fun checkPlaygroundGradleConfiguration() {
        val errors = mutableListOf<String>()

        val propertyMismatches = compareProperties()
        if (propertyMismatches.isNotEmpty()) {
            val message = StringBuilder()
            message.append(
                """
                The following properties are defined in ${androidxProperties.get().asFile.absolutePath}
                with values that differ from ${playgroundProperties.get().asFile.absolutePath}:

                """
                    .trimIndent()
            )
            propertyMismatches.forEach { mismatch -> message.append(" - $mismatch\n") }
            message.append(
                """

                If this change is intentional, you can ignore it by adding it to ignoredProperties
                in VerifyPlaygroundGradleConfigurationTask.kt

                Note: Having inconsistent properties in playground projects might trigger wrong
                compilation output in the main AndroidX build, so if a property is defined in
                playground properties, its value **MUST** match that of regular AndroidX build.
                """
                    .trimIndent()
            )
            errors.add(message.toString())
        }

        compareGradleWrapperVersion()?.let { errors.add(it) }

        if (errors.isNotEmpty()) {
            throw GradleException(
                "Found the following configuration errors in the playground project:\n\n" +
                    errors.joinToString("\n\n")
            )
        }
    }

    private fun compareProperties(): List<String> {
        val rootProperties = loadPropertiesFile(androidxProperties.get().asFile)
        val playgroundProperties = loadPropertiesFile(playgroundProperties.get().asFile)
        return validateProperties(
            rootProperties,
            playgroundProperties,
            exceptedProperties,
            ignoredProperties,
        )
    }

    // Returns an error message if there is a mismatch, otherwise null
    private fun compareGradleWrapperVersion(): String? {
        val androidxGradleVersion =
            readGradleVersionFromWrapperProperties(androidxGradleWrapper.get().asFile)
        val playgroundGradleVersion =
            readGradleVersionFromWrapperProperties(playgroundGradleWrapper.get().asFile)
        if (androidxGradleVersion != playgroundGradleVersion) {
            return """
                Playground gradle version ($playgroundGradleVersion) must match the AndroidX main
                build gradle version ($androidxGradleVersion).
                """
                .trimIndent()
        }
        return null
    }

    private fun readGradleVersionFromWrapperProperties(file: File): String {
        val distributionUrl = loadPropertiesFile(file).getProperty("distributionUrl")
        checkNotNull(distributionUrl) {
            "cannot read distribution url from gradle wrapper file: ${file.canonicalPath}"
        }
        val gradleVersion = extractGradleVersion(distributionUrl)
        return checkNotNull(gradleVersion) {
            "Failed to extract gradle version from gradle wrapper file. Input: $distributionUrl"
        }
    }

    private fun loadPropertiesFile(file: File) =
        file.inputStream().use { inputStream -> Properties().apply { load(inputStream) } }

    companion object {
        private const val TASK_NAME = "verifyPlaygroundGradleConfiguration"

        // A mapping of the expected override in playground, which should generally follow AOSP on
        // androidx-main. Generally, should only be used for conflicting properties which have
        // different values in different built targets on AOSP, but still should be declared in
        // playground.
        private val exceptedProperties = mapOf("androidx.writeVersionedApiFiles" to "true")

        private val ignoredProperties =
            setOf(
                "android.builder.sdkDownload",
                "android.suppressUnsupportedCompileSdk",
                "android.r8.maxWorkers",
                "androidx.constraints",
                "androidx.yarnOfflineMode",
                "kotlin.native.distribution.downloadFromMaven",
                "kotlin.project.persistent.dir",
                "kotlin.user.home",
                "org.gradle.jvmargs",
                "org.gradle.daemon",
                "org.gradle.java.installations.auto-download",
                "org.gradle.java.installations.auto-detect",
                "org.gradle.java.installations.fromEnv",
                "org.gradle.projectcachedir",
                "org.gradle.dependency.verification",
                "ksp.version.check",
            )

        /**
         * Regular expression to extract the gradle version from a distributionUrl property. Sample
         * input looks like: <some-path>/gradle-7.3-rc-2-all.zip
         */
        private val GRADLE_VERSION_REGEX = """/gradle-(.+)-(all|bin)\.zip$""".toRegex()

        @VisibleForTesting // make it accessible for buildSrc-tests
        fun extractGradleVersion(distributionUrl: String): String? {
            return GRADLE_VERSION_REGEX.find(distributionUrl)?.groupValues?.getOrNull(1)
        }

        /**
         * Creates the task to verify playground properties if an only if we have the
         * playground-common folder to check against.
         */
        fun createIfNecessary(
            project: Project
        ): TaskProvider<VerifyPlaygroundGradleConfigurationTask>? {
            return if (project.projectDir.resolve("playground-common").exists()) {
                project.tasks
                    .register(TASK_NAME, VerifyPlaygroundGradleConfigurationTask::class.java) {
                        it.androidxProperties.set(
                            project.layout.projectDirectory.file("gradle.properties")
                        )
                        it.playgroundProperties.set(
                            project.layout.projectDirectory.file(
                                "playground-common/androidx-shared.properties"
                            )
                        )
                        it.androidxGradleWrapper.set(
                            project.layout.projectDirectory.file(
                                "gradle/wrapper/gradle-wrapper.properties"
                            )
                        )
                        it.playgroundGradleWrapper.set(
                            project.layout.projectDirectory.file(
                                "playground-common/gradle/wrapper/gradle-wrapper.properties"
                            )
                        )
                    }
                    .also { it.configure { task -> task.cacheEvenIfNoOutputs() } }
            } else {
                null
            }
        }
    }
}

// Returns a list of mismatches properties, empty if the configurations match
@VisibleForTesting
internal fun validateProperties(
    rootProperties: Properties,
    playgroundProperties: Properties,
    exceptedProperties: Map<String, String>,
    ignoredProperties: Set<String>,
): List<String> {
    // ensure we don't define properties that do not match the root file
    // this includes properties that are not defined in the root androidx build as they might
    // be properties which can alter the build output. We might consider allow listing certain
    // properties in the future if necessary.
    val propertyKeys = (rootProperties.keys + playgroundProperties.keys).filterNotNull()
    val expectedPropertyErrors =
        propertyKeys
            .filter { key -> exceptedProperties.containsKey(key) }
            .map { key ->
                val playgroundValue = playgroundProperties[key]
                val expectedValue = exceptedProperties[key]
                if (playgroundValue != exceptedProperties[key]) {
                    "Expected playground property '$key' to have value '$expectedValue' but was '$playgroundValue'"
                } else {
                    null
                }
            }
    val mismatchedPropertyErrors =
        propertyKeys
            .filterNot { key -> exceptedProperties.containsKey(key) }
            .map { key ->
                val rootValue = rootProperties[key]
                val playgroundValue = playgroundProperties[key]
                if (rootValue != playgroundValue && !ignoredProperties.contains(key)) {
                    "$key: AndroidX value: $rootValue, Playground value: $playgroundValue"
                } else {
                    null
                }
            }
    return (expectedPropertyErrors + mismatchedPropertyErrors).filterNotNull()
}
