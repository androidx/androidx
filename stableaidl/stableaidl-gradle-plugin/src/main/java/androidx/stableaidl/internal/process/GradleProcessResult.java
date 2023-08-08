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
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessInfo;
import com.android.ide.common.process.ProcessResult;

import com.google.common.base.Joiner;

import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecException;

/**
 * Cloned from <code>com.android.build.gradle.internal.process.GradleProcessResult</code>.
 */
class GradleProcessResult implements ProcessResult {

    @NonNull
    private final ExecResult result;

    @NonNull
    private final ProcessInfo processInfo;

    GradleProcessResult(@NonNull ExecResult result, @NonNull ProcessInfo processInfo) {
        this.result = result;
        this.processInfo = processInfo;
    }

    @NonNull
    @Override
    public ProcessResult assertNormalExitValue() throws ProcessException {
        try {
            result.assertNormalExitValue();
        } catch (ExecException e) {
            throw buildProcessException(e);
        }

        return this;
    }

    @Override
    public int getExitValue() {
        return result.getExitValue();
    }

    @NonNull
    @Override
    public ProcessResult rethrowFailure() throws ProcessException {
        try {
            result.rethrowFailure();
        } catch (ExecException e) {
            throw buildProcessException(e);
        }
        return this;
    }

    @NonNull
    private ProcessException buildProcessException(@NonNull ExecException e) {
        return new ProcessException(
                String.format(
                        "Error while executing %s with arguments {%s}",
                        processInfo.getDescription(),
                        Joiner.on(' ').join(processInfo.getArgs())),
                e);
    }
}
