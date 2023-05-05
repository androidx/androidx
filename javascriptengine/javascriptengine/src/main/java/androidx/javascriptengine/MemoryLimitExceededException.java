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

/**
 * Indicates that a JavaScriptIsolate's evaluation failed due to exceeding its heap size limit.
 * <p>
 * This exception may be thrown when exceeding the heap size limit configured for the isolate via
 * {@link IsolateStartupParameters}, or the default limit. Beware that it will not be thrown if the
 * Android system as a whole has run out of memory before the JavaScript environment has reached
 * its configured heap limit.
 * <p>
 * The isolate may not continue to be used after this exception has been thrown, and other pending
 * evalutions for the isolate will fail. The isolate may continue to hold onto resources (even if
 * explicitly closed) until the sandbox has been shutdown. Therefore, it is recommended that the
 * sandbox be restarted at the earliest opportunity in order to reclaim these resources.
 * <p>
 * Other isolates within the same sandbox may continue to be used, created, and closed as normal.
 * <p>
 * Beware that not all JavaScript sandbox service implementations (particularly older ones)
 * handle memory exhaustion equally gracefully, and may instead crash the entire sandbox (see
 * {@link SandboxDeadException}).
 */
public final class MemoryLimitExceededException extends JavaScriptException {
    public MemoryLimitExceededException(@NonNull String error) {
        super(error);
    }
    public MemoryLimitExceededException() {
        super();
    }
}
