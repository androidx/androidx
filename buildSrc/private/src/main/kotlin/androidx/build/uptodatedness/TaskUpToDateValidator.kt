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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
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
        // caching disabled for now while we look for a fix for b/273294710
        "createAllArchives",
        // https://youtrack.jetbrains.com/issue/KT-52632
        "commonizeNativeDistribution",
        "createDiffArchiveForAll",
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

        // https://youtrack.jetbrains.com/issue/KT-49933
        "generateProjectStructureMetadata",

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
        // TODO(aurimas): add back when upgrading to AGP 8.0.0-beta01
        "com.android.build.gradle.internal.tasks.BundleLibraryJavaRes_Decorated",
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
            if (isCausedByAKlibChange(result)) {
                // ignore these until this bug in the KMP plugin is fixed.
                // see the method for details.
                return
            }
            if (!isAllowedToRerunTask(name)) {
                throw GradleException(
                    "Ran two consecutive builds of the same tasks, and in the " +
                        "second build, observed:\n" +
                        "task $name not UP-TO-DATE. It was out-of-date because:\n" +
                        "${result.executionReasons}"
                )
            }
        }
    }

    companion object {
        // Tells whether to create a TaskUpToDateValidator listener
        private fun shouldEnable(project: Project): Boolean {
            return project.providers.gradleProperty(ENABLE_FLAG_NAME).isPresent
        }

        /**
         * Currently, klibs are not reproducible, which means any task that depends on them might
         * get invalidated at no fault of their own.
         *
         * https://youtrack.jetbrains.com/issue/KT-52741
         */
        private fun isCausedByAKlibChange(result: TaskExecutionResult): Boolean {
            // the actual message looks something like:
            // Input property 'rootSpec$1$3' file <some-path>.klib has changed
            return result.executionReasons.orEmpty().any { it.contains(".klib has changed") }
        }

        private fun isAllowedToRerunTask(taskPath: String): Boolean {
            if (ALLOW_RERUNNING_TASKS.contains(taskPath)) {
                return true
            }
            val taskName = taskPath.substringAfterLast(":")
            if (ALLOW_RERUNNING_TASKS.contains(taskName)) {
                return true
            }
            if (taskName.startsWith("compile") && taskName.endsWith("KotlinMetadata")) {
                // these tasks' up-to-date checks might flake.
                // https://youtrack.jetbrains.com/issue/KT-52675
                // We are not adding the task type to the DONT_TRY_RERUNNING_TASKS list because it
                // is a common compilation task that is shared w/ other kotlin native compilations.
                // (e.g. similar to the Exec task in Gradle)
                return true
            }
            return false
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
            // create listener for validating that any task that reran was expected to rerun
            val validatorProvider =
                project.gradle.sharedServices.registerIfAbsent(
                    "TaskUpToDateValidator",
                    TaskUpToDateValidator::class.java
                ) { spec ->
                    spec.parameters.validate = validate
                }
            registry.onTaskCompletion(validatorProvider)

            // skip rerunning tasks that are known to be unnecessary to rerun
            project.tasks.configureEach { task ->
                task.onlyIf { shouldTryRerunningTask(task) || !validate.get() }
            }
        }
    }
}
