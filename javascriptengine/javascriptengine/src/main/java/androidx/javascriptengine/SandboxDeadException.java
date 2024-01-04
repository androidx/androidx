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
import androidx.annotation.RestrictTo;

/**
 * Exception thrown when evaluation is terminated due the {@link JavaScriptSandbox} being dead.
 * This can happen when {@link JavaScriptSandbox#close()} is called or when the sandbox process
 * is killed by the framework.
 */
public final class SandboxDeadException extends IsolateTerminatedException {
    public SandboxDeadException() {
        super();
    }
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SandboxDeadException(@NonNull String message) {
        super(message);
    }
}
