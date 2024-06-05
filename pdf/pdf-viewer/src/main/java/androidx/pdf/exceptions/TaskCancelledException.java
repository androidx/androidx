/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.exceptions;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * An exception used to report that a specific fetch task was cancelled, for whatever
 * reason.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TaskCancelledException extends Exception {

    /** Constructs this exception with the current stack trace. */
    public TaskCancelledException() {
    }

    /**
     * Constructs this exception with the current stack trace and the specified detail message.
     */
    public TaskCancelledException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
