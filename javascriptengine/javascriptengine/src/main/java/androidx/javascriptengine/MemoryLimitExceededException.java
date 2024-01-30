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

package androidx.javascriptengine;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
 * Indicates that a JavaScriptIsolate's evaluation failed due to the isolate exceeding its heap
 * size limit.
 * <p>
 * This exception may be thrown when exceeding the heap size limit configured for the isolate via
 * {@link IsolateStartupParameters}, or the default limit. Beware that it will not be thrown if the
 * Android system as a whole has run out of memory before the JavaScript environment has reached
 * its configured heap limit.
 * <p>
 * If an evaluation fails with a MemoryLimitExceededException, it does not imply that that
 * particular evaluation was in any way responsible for any excessive memory usage.
 * MemoryLimitExceededException will be raised for all unresolved and future requested evaluations
 * regardless of their culpability.
 * <p>
 * An isolate may run out of memory outside of an explicit evaluation (such as in a microtask), so
 * you should generally not use this exception to detect out of memory issues - instead, use
 * {@link JavaScriptIsolate#addOnTerminatedCallback(Executor, Consumer)} and check
 * for an isolate termination status of {@link TerminationInfo#STATUS_MEMORY_LIMIT_EXCEEDED}.
 */
public final class MemoryLimitExceededException extends IsolateTerminatedException {
    public MemoryLimitExceededException() {
        super();
    }
    public MemoryLimitExceededException(@NonNull String error) {
        super(error);
    }
}
