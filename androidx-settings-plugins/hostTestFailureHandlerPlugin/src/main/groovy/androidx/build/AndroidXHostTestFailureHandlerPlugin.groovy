/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build

import org.gradle.api.Plugin
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.execution.MultipleBuildFailures
import javax.inject.Inject


abstract class HostTestFailureFlowParameters implements FlowParameters {
    @Input
    abstract Property<Throwable> getFailure()

    @Input
    abstract Property<File> getOnlyTestTaskFailedSignalFileProperty()
}

/**
 * A FlowAction that records test failures to a signal file when a build continues on failure.
 * This allows CI systems or other tools to detect that a test task has failed even if the
 * overall build succeeds.
 */
class RecordTestFailureFlowAction implements FlowAction<HostTestFailureFlowParameters> {

    // TODO(https://github.com/gradle/gradle/issues/34506): Remove when `execute` is stable
    @SuppressWarnings("UnstableApiUsage")
    @Override
    void execute(HostTestFailureFlowParameters parameters) {
        if (!parameters.failure.present) {
            return
        }
        Throwable buildResultFailure = parameters.failure.get()
        def individualFailures = buildResultFailure instanceof MultipleBuildFailures ?
                buildResultFailure.causes : [buildResultFailure]

        if (individualFailures.isEmpty()) {
            return
        }

        boolean onlyTestTasksFailed = individualFailures.every {
            def cause = it.cause
            cause instanceof TaskExecutionException && cause.task instanceof AbstractTestTask
        }

        if (onlyTestTasksFailed) {
            List<String> failureMessages = individualFailures.collect { currentFailureException ->
                def taskExecutionException = (TaskExecutionException) currentFailureException.cause
                "Task path: ${taskExecutionException.task.path}, " +
                        "Exception: ${currentFailureException.message}"
            }

            File signalFile = parameters.onlyTestTaskFailedSignalFileProperty.get()
            if (signalFile.exists()) {
                signalFile.delete()
            }
            try {
                signalFile.parentFile?.mkdirs()
                String messageToLog = "Only test tasks failed. Build continued due to --continue.\n" +
                        failureMessages.join("\n")
                signalFile.write(messageToLog)
            } catch (IOException e) {
                e.printStackTrace(System.err)
            }
        }
    }
}

/**
 * A Gradle Settings plugin that uses the Flow API to handle host test failures.
 */
@SuppressWarnings("unused")
abstract class AndroidXHostTestFailureHandlerPlugin implements Plugin<Settings> {
    @Inject
    abstract FlowScope getFlowScope()

    @Inject
    abstract FlowProviders getFlowProviders()

    @Override
    void apply(Settings settings) {
        def outDirProvider = settings.providers.environmentVariable("OUT_DIR")

        def signalFileProvider = outDirProvider.map { out ->
            new File(new File(out, "androidx-settings-plugins"), "only_test_task_failed_signal.txt")
        }

        getFlowScope().always(RecordTestFailureFlowAction.class) { spec ->
            spec.parameters.failure.set(getFlowProviders().buildWorkResult.map {
                it.failure.orElse(null)
            })
            spec.parameters.onlyTestTaskFailedSignalFileProperty.set(signalFileProvider)
        }
    }
}
