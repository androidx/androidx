/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.ftl

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.TestVariant
import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * Task to run instrumentation tests on FTL.
 *
 * This task is only enabled on playground projects and requires gcloud CLI to be available on
 * the device with the right permissions.
 *
 * Due to the limitations of FTL, this task only support application instrumentation tests for now.
 */
@Suppress("UnstableApiUsage") // for gradle property APIs
abstract class RunTestOnFTLTask : DefaultTask() {
    /**
     * The test APK for the instrumentation test.
     */
    @InputFile
    val testApk: RegularFileProperty = project.objects.fileProperty()

    /**
     * The tested application APK.
     */
    @InputFile
    val testedApk: RegularFileProperty = project.objects.fileProperty()

    /**
     * Output file to write the results
     */
    @OutputFile
    val testResults: RegularFileProperty = project.objects.fileProperty()
    @TaskAction
    fun executeTest() {
        val testApk = testApk.asFile.get()
        val testedApk = testedApk.asFile.get()
        println(testApk.path + "/" + testedApk.path)
        val gcloud = GCloudCLIWrapper(project)
        val result = gcloud.runTest(
            testedApk = testedApk,
            testApk = testApk
        )
        val outFile = testResults.asFile.get()
        outFile.parentFile.mkdirs()
        val gson = Gson()
        outFile.bufferedWriter(Charsets.UTF_8).use {
            gson.toJson(
                result,
                it
            )
        }
        val failed = result.filterNot {
            it.passed
        }
        if (failed.isNotEmpty()) {
            throw GradleException("These tests failed: $failed")
        }
    }

    companion object {
        private const val TASK_SUFFIX = "OnFirebaseTestLab"
        private const val TEST_OUTPUT_FILE_NAME = "testResults.json"

        /**
         * Creates an FTL test runner task and returns it.
         * Note that only application tests are supported hence this will return `null` for
         * library projects.
         */
        fun create(project: Project, testVariant: TestVariant): TaskProvider<RunTestOnFTLTask>? {
            if (testVariant.testedVariant is LibraryVariant) {
                // TODO add support for library project, which might require synthesizing another
                //  APK :facepalm:
                // see: // https://stackoverflow.com/questions/59827750/execute-instrumented-test-for-an-android-library-with-firebase-test-lab
                return null
            }
            val taskName = testVariant.name + TASK_SUFFIX
            return project.tasks.register(taskName, RunTestOnFTLTask::class.java) { task ->
                task.description = "Run ${testVariant.name} tests on Firebase Test Lab"
                task.group = "Verification"
                task.testResults.set(
                    project.layout.buildDirectory.dir(
                        "ftl-results"
                    ).map {
                        it.file(TEST_OUTPUT_FILE_NAME)
                    }
                )
                task.dependsOn(testVariant.assembleProvider)
                task.dependsOn(testVariant.testedVariant.assembleProvider)
                testVariant.outputs.withType(ApkVariantOutput::class.java).firstOrNull()
                task.testApk.set(
                    testVariant.outputs.withType(ApkVariantOutput::class.java).firstOrNull()
                        ?.outputFile
                )
                task.testedApk.set(
                    testVariant.testedVariant.outputs
                        .withType(ApkVariantOutput::class.java)
                        .firstOrNull()
                        ?.outputFile
                )
            }
        }
    }
}