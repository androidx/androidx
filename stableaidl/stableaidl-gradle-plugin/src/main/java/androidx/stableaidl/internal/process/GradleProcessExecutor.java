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

package androidx.stableaidl.internal.process;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessExecutor;
import com.android.ide.common.process.ProcessInfo;
import com.android.ide.common.process.ProcessOutput;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.ide.common.process.ProcessResult;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.gradle.api.Action;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of ProcessExecutor that uses Gradle's mechanism to execute external processes.
 *
 * Cloned from <code>com.android.build.gradle.internal.process.GradleProcessExecutor</code>.
 */
public class GradleProcessExecutor implements ProcessExecutor {

    @NonNull private final Function<Action<? super ExecSpec>, ExecResult> execOperations;

    // Lambda is stored but not compared
    @SuppressWarnings("ImplicitSamInstance")
    public GradleProcessExecutor(
            @NonNull Function<Action<? super ExecSpec>, ExecResult> execOperations) {
        this.execOperations = execOperations;
    }

    @NonNull
    @Override
    public ListenableFuture<ProcessResult> submit(@NonNull final ProcessInfo processInfo,
            @NonNull final ProcessOutputHandler processOutputHandler) {
        final SettableFuture<ProcessResult> res = SettableFuture.create();
        new Thread() {
            @Override
            public void run() {
                try {
                    ProcessResult result = execute(processInfo, processOutputHandler);
                    res.set(result);
                } catch (Throwable e) {
                    res.setException(e);
                }
            }
        }.start();

        return res;
    }

    @NonNull
    @Override
    public ProcessResult execute(
            @NonNull ProcessInfo processInfo,
            @NonNull ProcessOutputHandler processOutputHandler) {
        ProcessOutput output = processOutputHandler.createOutput();

        ExecResult result;
        try {
            result = execOperations.apply(new ExecAction(processInfo, output));
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                LoggerWrapper.getLogger(GradleProcessExecutor.class)
                        .warning(
                                "Exception while closing sub process streams: "
                                        + Throwables.getStackTraceAsString(e));
            }
        }
        try {
            processOutputHandler.handleOutput(output);
        } catch (final ProcessException e) {
            return new OutputHandlerFailedGradleProcessResult(e);
        }
        return new GradleProcessResult(result, processInfo);
    }

    private static class ExecAction implements Action<ExecSpec> {

        @NonNull
        private final ProcessInfo processInfo;

        @NonNull
        private final ProcessOutput processOutput;

        ExecAction(@NonNull final ProcessInfo processInfo,
                @NonNull final ProcessOutput processOutput) {
            this.processInfo = processInfo;
            this.processOutput = processOutput;
        }

        @Override
        public void execute(ExecSpec execSpec) {

            /*
             * Gradle doesn't work correctly when there are empty args.
             */
            List<String> args =
                    processInfo.getArgs().stream()
                            .map(a -> a.isEmpty()? "\"\"" : a)
                            .collect(Collectors.toList());
            execSpec.setExecutable(processInfo.getExecutable());
            execSpec.args(args);
            execSpec.environment(processInfo.getEnvironment());
            execSpec.setStandardOutput(processOutput.getStandardOutput());
            execSpec.setErrorOutput(processOutput.getErrorOutput());
            File directory = processInfo.getWorkingDirectory();
            if (directory != null) {
                execSpec.setWorkingDir(directory);
            }

            // we want the caller to be able to do its own thing.
            execSpec.setIgnoreExitValue(true);
        }
    }
}
