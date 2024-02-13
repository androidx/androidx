/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.uptodatedness

import androidx.build.VERIFY_UP_TO_DATE
import java.io.File
import java.util.Date
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult

/**
 * Validates that all tasks (except a temporary exception list) are considered up-to-date. The
 * expected usage of this is that the user will invoke a build with the TaskUpToDateValidator
 * disabled, and then reinvoke the same build with the TaskUpToDateValidator enabled. If the second
 * build actually runs any tasks, then some tasks don't have the correct inputs/outputs declared and
 * are running more often than necessary.
 */
const val DISALLOW_TASK_EXECUTION_VAR_NAME = "DISALLOW_TASK_EXECUTION"

private const val ENABLE_FLAG_NAME = VERIFY_UP_TO_DATE

// Temporary set of exempt tasks that are known to still be out-of-date after running once
// Entries in this set may be task names (like assembleRelease) or task paths
// (like :core:core:assembleRelease)
// Entries in this set do still get rerun because they might produce files that are needed by
// subsequent tasks
val ALLOW_RERUNNING_TASKS =
    setOf(
        "buildOnServer",
        "checkExternalLicenses",
        // verifies the existence of some archives to check for caching bugs: http://b/273294710
        "createAllArchives",
        "externalNativeBuildDebug",
        "externalNativeBuildRelease",
        "generateDebugUnitTestConfig",
        "generateJsonModelDebug",
        "generateJsonModelRelease",
        /**
         * relocateShadowJar is used to configure the ShadowJar hence it does not have any outputs.
         * https://github.com/johnrengelman/shadow/issues/561
         */
        "relocateShadowJar",
        "testDebugUnitTest",
        "verifyDependencyVersions",
        "zipTestConfigsWithApks",
        "zipHtmlResultsOfTestDebugUnitTest",
        "zipXmlResultsOfTestDebugUnitTest",
        ":camera:integration-tests:camera-testapp-core:mergeLibDexDebug",
        ":camera:integration-tests:camera-testapp-core:packageDebug",
        ":camera:integration-tests:camera-testapp-extensions:mergeLibDexDebug",
        ":camera:integration-tests:camera-testapp-extensions:packageDebug",
        ":camera:integration-tests:camera-testapp-extensions:" +
            "GenerateTestConfigurationdebugAndroidTest",
        ":camera:integration-tests:camera-testapp-uiwidgets:mergeLibDexDebug",
        ":camera:integration-tests:camera-testapp-uiwidgets:packageDebug",
        ":camera:integration-tests:camera-testapp-core:GenerateTestConfigurationdebug",
        ":camera:integration-tests:camera-testapp-core:GenerateTestConfigurationdebugAndroidTest",
        ":camera:integration-tests:camera-testapp-view:GenerateTestConfigurationdebug",
        ":camera:integration-tests:camera-testapp-view:GenerateTestConfigurationdebugAndroidTest",
        ":camera:integration-tests:camera-testapp-view:mergeLibDexDebug",
        ":camera:integration-tests:camera-testapp-view:packageDebug",
        "configureCMakeDebug[armeabi-v7a]",
        "configureCMakeDebug[arm64-v8a]",
        "configureCMakeDebug[x86]",
        "configureCMakeDebug[x86_64]",
        "buildCMakeDebug[armeabi-v7a]",
        "buildCMakeDebug[arm64-v8a]",
        "buildCMakeDebug[x86]",
        "buildCMakeDebug[x86_64]",
        "configureCMakeRelWithDebInfo[armeabi-v7a]",
        "configureCMakeRelWithDebInfo[arm64-v8a]",
        "configureCMakeRelWithDebInfo[x86]",
        "configureCMakeRelWithDebInfo[x86_64]",
        "buildCMakeRelWithDebInfo[armeabi-v7a]",
        "buildCMakeRelWithDebInfo[arm64-v8a]",
        "buildCMakeRelWithDebInfo[x86]",
        "buildCMakeRelWithDebInfo[x86_64]",
        ":appsearch:appsearch-local-storage:buildCMakeDebug[armeabi-v7a][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeDebug[arm64-v8a][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeDebug[x86][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeDebug[x86_64][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeRelWithDebInfo[armeabi-v7a][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeRelWithDebInfo[arm64-v8a][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeRelWithDebInfo[x86][icing]",
        ":appsearch:appsearch-local-storage:buildCMakeRelWithDebInfo[x86_64][icing]",
        ":external:libyuv:buildCMakeDebug[armeabi-v7a][yuv]",
        ":external:libyuv:buildCMakeDebug[arm64-v8a][yuv]",
        ":external:libyuv:buildCMakeDebug[x86][yuv]",
        ":external:libyuv:buildCMakeDebug[x86_64][yuv]",
        ":external:libyuv:buildCMakeRelWithDebInfo[armeabi-v7a][yuv]",
        ":external:libyuv:buildCMakeRelWithDebInfo[arm64-v8a][yuv]",
        ":external:libyuv:buildCMakeRelWithDebInfo[x86][yuv]",
        ":external:libyuv:buildCMakeRelWithDebInfo[x86_64][yuv]",
        ":lint-checks:integration-tests:copyDebugAndroidLintReports",

        // https://github.com/google/protobuf-gradle-plugin/issues/667
        ":appactions:interaction:interaction-service-proto:extractIncludeTestProto",
        ":datastore:datastore-preferences-proto:extractIncludeTestProto",
        ":glance:glance-appwidget-proto:extractIncludeTestProto",
        ":health:connect:connect-client-proto:extractIncludeTestProto",
        ":privacysandbox:tools:tools-core:extractIncludeTestProto",
        ":test:screenshot:screenshot-proto:extractIncludeTestProto",
        ":wear:protolayout:protolayout-proto:extractIncludeTestProto",
        ":wear:tiles:tiles-proto:extractIncludeTestProto",

        // https://youtrack.jetbrains.com/issue/KT-61931
        "checkKotlinGradlePluginConfigurationErrors"
    )

// Additional tasks that are expected to be temporarily out-of-date after running once
// Tasks in this set we don't even try to rerun, because they're known to be unnecessary
val DONT_TRY_RERUNNING_TASKS =
    setOf(
        "listTaskOutputs",
        "tasks",

        // More information about the fact that these dackka tasks rerun can be found at b/167569304
        "docs",

        // We know that these tasks are never up to date due to maven-metadata.xml changing
        // https://github.com/gradle/gradle/issues/11203
        "partiallyDejetifyArchive",
        "stripArchiveForPartialDejetification",
        "createArchive",

        // https://github.com/spdx/spdx-gradle-plugin/issues/18
        "spdxSbomForRelease",
    )

val DONT_TRY_RERUNNING_TASK_TYPES =
    setOf(
        "com.android.build.gradle.internal.lint.AndroidLintTextOutputTask_Decorated",
        // lint report tasks
        "com.android.build.gradle.internal.lint.AndroidLintTask_Decorated",
        // lint analysis tasks b/223287425
        "com.android.build.gradle.internal.lint.AndroidLintAnalysisTask_Decorated",
        // https://github.com/gradle/gradle/issues/11717
        "org.gradle.api.publish.tasks.GenerateModuleMetadata_Decorated",
        "org.gradle.api.publish.maven.tasks.GenerateMavenPom_Decorated",
        // due to GenerateModuleMetadata re-running
        "androidx.build.GMavenZipTask_Decorated",
        "org.gradle.api.publish.maven.tasks.PublishToMavenRepository_Decorated",
        // This task is not cacheable by design due to large number of inputs
        "androidx.build.license.CheckExternalDependencyLicensesTask_Decorated",
    )

abstract class TaskUpToDateValidator :
    BuildService<TaskUpToDateValidator.Parameters>, OperationCompletionListener {
    interface Parameters : BuildServiceParameters {
        // We check <validate> during task execution rather than during project configuration
        // so that any configuration cache created during the first build can be reused during the
        // second build, saving build time
        var validate: Provider<Boolean>
        // Directory for saving metadata about task executions
        var metadataDir: Provider<Directory>
    }

    override fun onFinish(event: FinishEvent) {
        if (!parameters.validate.get()) {
            return
        }
        val result = event.result
        if (result is TaskExecutionResult) {
            val name = event.descriptor.name
            val executionReasons = result.executionReasons
            if (executionReasons.isNullOrEmpty()) {
                // empty list means task was actually up-to-date, see docs for
                // TaskExecutionResult.executionReasons
                // null list means the task already failed, so we'll skip emitting our error
                return
            }
            if (!isAllowedToRerunTask(name)) {
                throw GradleException(
                    "Ran two consecutive builds of the same tasks, and in the " +
                        "second build, observed:\n" +
                        "task $name not UP-TO-DATE. It was out-of-date because:\n" +
                        "\n" +
                        "  ${result.executionReasons}.\n" +
                        "\n" +
                        "Some additional diagnostics: \n" +
                        "\n" +
                        "  " + tryToExplainTaskExecution(name)
                            .replace("\n", "\n  ")
                )
            }
        }
    }

    fun getPreviousTaskExecutionCompletionTimestamp(taskPath: String): Date {
        // we're already saving the inputs of the task into a file,
        // so we can check the timestamp of that file to know when the task last reran
        val inputsFile = getTaskInputListPath(taskPath, parameters.metadataDir, false)
        return Date(inputsFile.lastModified())
    }

    fun checkForChangingSetOfInputs(taskPath: String): String {
        val previousInputs = loadTaskInputs(taskPath, parameters.metadataDir, false)
        val currentInputs = loadTaskInputs(taskPath, parameters.metadataDir, true)

        val addedInputs = currentInputs.minus(previousInputs)
        val removedInputs = previousInputs.minus(currentInputs)
        val addedMessage = if (addedInputs.size > 0) {
            "Added these " + addedInputs.size + " inputs: " +
                addedInputs.joinToString("\n") + "\n"
        } else {
            ""
        }
        val removedMessage = if (removedInputs.size > 0) {
            "Removed these " + removedInputs.size + " inputs: " +
                removedInputs.joinToString("\n") + "\n"
        } else {
            ""
        }
        return addedMessage + removedMessage
    }

    fun tryToExplainTaskExecution(taskPath: String): String {
        val numOutputFiles = loadTaskOutputs(taskPath, parameters.metadataDir, true).size
        val outputsMessage = if (numOutputFiles > 0) {
            taskPath + " declares " + numOutputFiles + " output files. This seems fine.\n"
        } else {
            taskPath + " declares " + numOutputFiles + " output files. This is probably " +
                "an error.\n"
        }

        val inputSetModifiedMessage = checkForChangingSetOfInputs(taskPath)
        val inputsMessage = if (inputSetModifiedMessage != "") {
            inputSetModifiedMessage
        } else {
            val inputFiles = loadTaskInputs(taskPath, parameters.metadataDir, true)
            var lastModifiedFile: File? = null
            var lastModifiedWhen = Date(0)
            for (inputFile in inputFiles) {
                val modifiedWhen = Date(inputFile.lastModified())
                if (modifiedWhen.compareTo(lastModifiedWhen) > 0) {
                    lastModifiedFile = inputFile
                    lastModifiedWhen = modifiedWhen
                }
            }

            if (lastModifiedFile != null) {
                taskPath + " declares " + inputFiles.size + " input files. The " +
                    "last modified input file is\n" + lastModifiedFile + "\nmodified at " +
                    lastModifiedWhen + " (the previous execution of this task completed at " +
                    getPreviousTaskExecutionCompletionTimestamp(taskPath) + ")."
            } else {
                taskPath + " declares " + inputFiles.size + " input files.\n"
            }
        }

        return outputsMessage + inputsMessage
    }

    companion object {
        // Tells whether to create a TaskUpToDateValidator listener
        private fun shouldEnable(project: Project): Boolean {
            return project.providers.gradleProperty(ENABLE_FLAG_NAME).isPresent
        }

        private fun isAllowedToRerunTask(taskPath: String): Boolean {
            if (ALLOW_RERUNNING_TASKS.contains(taskPath)) {
                return true
            }
            val taskName = taskPath.substringAfterLast(":")
            if (ALLOW_RERUNNING_TASKS.contains(taskName)) {
                return true
            }
            return false
        }

        private fun isAllowedToRerunTask(task: Task): Boolean {
            return isAllowedToRerunTask(task.path)
        }

        private fun shouldTryRerunningTask(task: Task): Boolean {
            return !(DONT_TRY_RERUNNING_TASKS.contains(task.name) ||
                DONT_TRY_RERUNNING_TASKS.contains(task.path) ||
                DONT_TRY_RERUNNING_TASK_TYPES.contains(task::class.qualifiedName))
        }

        fun setup(project: Project, registry: BuildEventsListenerRegistry) {
            if (!shouldEnable(project)) {
                return
            }
            val validate =
                project.providers
                    .environmentVariable(DISALLOW_TASK_EXECUTION_VAR_NAME)
                    .map { true }
                    .orElse(false)
            val metadataDir = project.rootProject.layout.buildDirectory.dir("TaskUpToDateValidator")

            // create listener for validating that any task that reran was expected to rerun
            val validatorProvider =
                project.gradle.sharedServices.registerIfAbsent(
                    "TaskUpToDateValidator",
                    TaskUpToDateValidator::class.java
                ) { spec ->
                    spec.parameters.validate = validate
                    spec.parameters.metadataDir = metadataDir
                }
            registry.onTaskCompletion(validatorProvider)

            // skip rerunning tasks that are known to be unnecessary to rerun
            project.tasks.configureEach { task ->
                task.onlyIf {
                    recordTaskData(task, metadataDir, validate)
                    shouldTryRerunningTask(task) || !validate.get()
                }
            }
        }

        private fun recordTaskData(
            task: Task,
            metadataDir: Provider<Directory>,
            isValidateRun: Provider<Boolean> // whether this run is expected to be all UP-TO-DATE
        ) {
            recordTaskInputs(task, metadataDir, isValidateRun)
            recordTaskOutputs(task, metadataDir, isValidateRun)
        }

        private fun recordTaskInputs(
            task: Task,
            metadataDir: Provider<Directory>,
            isValidateRun: Provider<Boolean>
        ) {
            val text = task.inputs.files.files.joinToString("\n")
            val destFile = getTaskInputListPath(task.path, metadataDir, isValidateRun.get())
            destFile.parentFile.mkdirs()
            destFile.writeText(text)
        }

        private fun loadTaskInputs(
            taskPath: String,
            metadataDir: Provider<Directory>,
            isValidateRun: Boolean
        ): List<File> {
            val dataFile = getTaskInputListPath(taskPath, metadataDir, isValidateRun)
            return dataFile.readLines().map { line -> File(line) }
        }

        private fun recordTaskOutputs(
            task: Task,
            metadataDir: Provider<Directory>,
            isValidateRun: Provider<Boolean>
        ) {
            val text = task.outputs.files.files.joinToString("\n")
            val destFile = getTaskOutputListPath(task.path, metadataDir, isValidateRun.get())
            destFile.parentFile.mkdirs()
            destFile.writeText(text)
        }

        private fun loadTaskOutputs(
            taskPath: String,
            metadataDir: Provider<Directory>,
            isValidateRun: Boolean
        ): List<File> {
            val dataFile = getTaskOutputListPath(taskPath, metadataDir, isValidateRun)
            return dataFile.readLines().map { line -> File(line) }
        }

        // returns the file for storing the inputs of the given task
        private fun getTaskInputListPath(
            taskPath: String,
            metadataDir: Provider<Directory>,
            isValidateRun: Boolean
        ): File {
            val baseDir = getTaskMetadataPath(taskPath, metadataDir, isValidateRun)
            return File(baseDir, "inputs")
        }

        // returns the file for storing the outputs of the given task
        private fun getTaskOutputListPath(
            taskPath: String,
            metadataDir: Provider<Directory>,
            isValidateRun: Boolean
        ): File {
            val baseDir = getTaskMetadataPath(taskPath, metadataDir, isValidateRun)
            return File(baseDir, "outputs")
        }

        // returns the directory for storing metadata about the given task
        private fun getTaskMetadataPath(
            taskPath: String,
            metadataDir: Provider<Directory>,
            isValidateRun: Boolean
        ): File {
            val baseDir = metadataDir.get().getAsFile()
            // convert from ":<project>:<subproject>:<taskname>" to "<project>/<subproject>/<taskname>"
            val taskDir = File(baseDir, taskPath.substringAfter(":").replace(":", "/"))
            val validateDirName = if (isValidateRun) "up-to-date" else "clean"
            return File(taskDir, validateDirName)
        }
    }
}
