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

package androidx.baselineprofile.gradle.producer.tasks

import androidx.baselineprofile.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofile.gradle.utils.TASK_NAME_SUFFIX
import androidx.baselineprofile.gradle.utils.camelCase
import com.android.build.api.variant.TestVariant
import com.android.build.gradle.internal.tasks.BuildAnalyzer
import com.android.buildanalyzer.common.TaskCategory
import com.google.testing.platform.proto.api.core.TestSuiteResultProto
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * Collects the generated baseline profile from the instrumentation results of a previous run of the
 * ui tests.
 */
@BuildAnalyzer(primaryTaskCategory = TaskCategory.OPTIMIZATION)
abstract class CollectBaselineProfileTask : DefaultTask() {

    companion object {

        private const val COLLECT_TASK_NAME = "collect"

        private const val PROP_KEY_PREFIX_INSTRUMENTATION_RUNNER_ARG =
            "android.testInstrumentationRunnerArguments."

        private const val PROP_KEY_INSTRUMENTATION_RUNNER_ARG_CLASS =
            "${PROP_KEY_PREFIX_INSTRUMENTATION_RUNNER_ARG}class"

        private const val GOOGLE_STORAGE_SCHEMA = "gs:"

        private val PROFILE_NAMES = listOf("-baseline-prof-", "-startup-prof-")

        private val PROFILE_LABELS =
            listOf("additionaltestoutput.benchmark.trace", "firebase.toolOutput")

        internal fun registerForVariant(
            project: Project,
            variant: TestVariant,
            testTaskDependencies: List<InstrumentationTestTaskWrapper>,
            shouldSkipGeneration: Boolean,
        ): TaskProvider<CollectBaselineProfileTask> {

            val flavorName = variant.flavorName
            val buildType = variant.buildType

            return project.tasks.register(
                camelCase(COLLECT_TASK_NAME, variant.name, TASK_NAME_SUFFIX),
                CollectBaselineProfileTask::class.java
            ) {
                var outputDir = project.layout.buildDirectory.dir("$INTERMEDIATES_BASE_FOLDER/")
                if (!flavorName.isNullOrBlank()) {
                    outputDir = outputDir.map { d -> d.dir(flavorName) }
                }
                if (!buildType.isNullOrBlank()) {
                    outputDir = outputDir.map { d -> d.dir(buildType) }
                }

                // Sets the baseline-prof output path.
                it.outputDir.set(outputDir)

                // Sets the test results inputs
                it.testResultDirs.setFrom(testTaskDependencies.map { t -> t.resultsDir })

                // Sets the project testInstrumentationRunnerArguments
                it.testInstrumentationRunnerArguments.set(
                    project.providers.gradlePropertiesPrefixedBy(
                        PROP_KEY_PREFIX_INSTRUMENTATION_RUNNER_ARG
                    )
                )

                // Disables the task if requested
                if (shouldSkipGeneration) it.enabled = false
            }
        }
    }

    init {
        group = "Baseline Profile"
        description = "Collects a baseline profile previously generated through integration tests."
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val testResultDirs: ConfigurableFileCollection

    @get:Input abstract val testInstrumentationRunnerArguments: MapProperty<String, Any>

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun exec() {

        // Determines if this is a partial result based on whether the property
        // `android.testInstrumentationRunnerArguments.class` is set
        val isPartialResult =
            testInstrumentationRunnerArguments
                .get()
                .containsKey(PROP_KEY_INSTRUMENTATION_RUNNER_ARG_CLASS)

        // Prepares list with test results to read. Note that these are the output directories
        // from the instrumentation task. We're interested only in `test-result.pb`.
        val testResultProtoFiles = testResultDirs.files.map { File(it, "test-result.pb") }

        // A test-result.pb file must exist as output of connected and managed device tests.
        // If it doesn't exist it's because there were no tests to run. If there are no devices,
        // the test task will simply fail. The following check is to give a meaningful error
        // message if something like that happens.
        if (testResultProtoFiles.none { it.exists() }) {
            throw GradleException(
                """
                Expected test results were not found. This is most likely because there are no
                tests to run. Please check that there are ui tests to execute. You can find more
                information at https://d.android.com/studio/test/advanced-test-setup. To create a
                baseline profile test instead, please check the documentation at
                https://d.android.com/baselineprofiles.
                """
                    .trimIndent()
            )
        }

        val profileFiles =
            testResultProtoFiles
                .asSequence()
                .onEach { logger.info("Parsing test-result.pb in `${it.absolutePath}`.") }
                .map { TestSuiteResultProto.TestSuiteResult.parseFrom(it.readBytes()) }
                .flatMap {
                    // Artifacts can be per test results (when running locally) or
                    // global (when running on ftl).
                    it.testResultList.flatMap { r -> r.outputArtifactList } + it.outputArtifactList
                }
                .filter {
                    // The label for this artifact is either `additionaltestoutput.benchmark.trace`
                    // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:utp/android-test-plugin-host-additional-test-output/src/main/java/com/android/tools/utp/plugins/host/additionaltestoutput/AndroidAdditionalTestOutputPlugin.kt;l=199?q=additionaltestoutput.benchmark.trace
                    // or "firebase.toolOutput" when using ftl. There could be also artifacts stored
                    // on google storage when running on ftl, so we need to skip those.
                    it.label.label in PROFILE_LABELS &&
                        !it.sourcePath.path.startsWith(GOOGLE_STORAGE_SCHEMA)
                }
                .map { File(it.sourcePath.path) }
                .filter {
                    // NOTE: If the below logic must be changed, be sure to update
                    // OutputsTest#sanitizeFilename_baselineProfileGradlePlugin
                    // as that covers library -> plugin file handoff testing
                    it.extension == "txt" && PROFILE_NAMES.any { n -> n in it.name }
                }
                .onEach { logger.info("Found profile file `$it`.") }
                .toSet()

        // If this is not a partial result delete the content of the output dir.
        if (!isPartialResult) {
            outputDir.get().asFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }

        // Saves the merged baseline profile file in the final destination. Existing tests are
        // overwritten, in case this is a partial result that needs to update an existing profile.
        profileFiles.forEach { it.copyTo(outputDir.file(it.name).get().asFile, overwrite = true) }
    }
}
