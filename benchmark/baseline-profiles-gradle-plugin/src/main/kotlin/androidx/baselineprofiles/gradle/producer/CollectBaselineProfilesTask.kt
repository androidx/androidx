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

package androidx.baselineprofiles.gradle.producer

import com.google.testing.platform.proto.api.core.TestSuiteResultProto
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Collects the generated baseline profiles from the instrumentation results of a previous run of
 * the ui tests.
 */
@DisableCachingByDefault(because = "Not worth caching.")
abstract class CollectBaselineProfilesTask : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val connectedAndroidTestOutputDir: DirectoryProperty

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val managedAndroidTestOutputDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "Baseline Profiles"
        description = "Collects baseline profiles previously generated through integration tests."
    }

    @TaskAction
    fun exec() {

        // Prepares list with test results to read
        val testResultProtoFiles =
            listOf(connectedAndroidTestOutputDir, managedAndroidTestOutputDir)
                .filter { it.isPresent }
                .map { it.file("test-result.pb").get().asFile }

        // A test-result.pb file must exist as output of connected and managed device tests.
        // If it doesn't exist it's because there were no tests to run. If there are no devices,
        // the test task will simply fail. The following check is to give a meaningful error
        // message if something like that happens.
        if (testResultProtoFiles.filter { !it.exists() }.isNotEmpty()) {
            throw GradleException(
                """
                Expected test results were not found. This is most likely because there are no
                tests to run. Please check that there are ui tests to execute. You can find more
                information at https://d.android.com/studio/test/advanced-test-setup. To create a
                baseline profile test instead, please check the documentation at
                https://d.android.com/baselineprofiles.
                """.trimIndent()
            )
        }

        val profiles = mutableSetOf<String>()
        testResultProtoFiles
            .map { TestSuiteResultProto.TestSuiteResult.parseFrom(it.readBytes()) }
            .forEach { testSuiteResult ->
                for (testResult in testSuiteResult.testResultList) {

                    // Baseline profile files are extracted by the test task. Here we find their
                    // location checking the test-result.pb proto. Note that the BaselineProfileRule
                    // produces one baseline profile file per test.
                    val baselineProfileFiles = testResult.outputArtifactList
                        .filter {
                            // The label for this artifact is `additionaltestoutput.benchmark.trace`
                            // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:utp/android-test-plugin-host-additional-test-output/src/main/java/com/android/tools/utp/plugins/host/additionaltestoutput/AndroidAdditionalTestOutputPlugin.kt;l=199?q=additionaltestoutput.benchmark.trace
                            it.label.label == "additionaltestoutput.benchmark.trace" &&
                                "-baseline-prof-" in it.sourcePath.path
                        }
                        .map { File(it.sourcePath.path) }
                    if (baselineProfileFiles.isEmpty()) {
                        continue
                    }

                    // Merge each baseline profile file from the test results into the aggregated
                    // baseline file, removing duplicate lines.
                    for (baselineProfileFile in baselineProfileFiles) {
                        profiles.addAll(baselineProfileFile.readLines())
                    }
                }
            }

        if (profiles.isEmpty()) {
            throw GradleException("No baseline profiles found in test outputs.")
        }

        // Saves the merged baseline profile file in the final destination
        val file = outputFile.get().asFile
        file.writeText(profiles.joinToString(System.lineSeparator()))
        logger.info("Aggregated baseline profile generated at ${file.absolutePath}")
    }
}
