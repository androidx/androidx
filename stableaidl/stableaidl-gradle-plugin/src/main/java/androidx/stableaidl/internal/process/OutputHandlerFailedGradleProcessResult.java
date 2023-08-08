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
import com.android.ide.common.process.ProcessResult;

/**
 * Cloned from
 * <code>com.android.build.gradle.internal.process.OutputHandlerFailedGradleProcessResult</code>.
 */
public class OutputHandlerFailedGradleProcessResult implements ProcessResult {
    @NonNull
    private final ProcessException failure;

    OutputHandlerFailedGradleProcessResult(@NonNull ProcessException failure) {
        this.failure = failure;
    }

    @NonNull
    @Override
    public ProcessResult assertNormalExitValue() throws ProcessException {
        throw failure;
    }

    @Override
    public int getExitValue() {
        return -1;
    }

    @NonNull
    @Override
    public ProcessResult rethrowFailure() throws ProcessException {
        throw failure;
    }
}