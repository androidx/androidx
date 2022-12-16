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

package androidx.camera.core.impl.utils;

import androidx.annotation.NonNull;

/**
 * The runtime version of the checked exception {@link InterruptedException}.
 *
 * <p>It is useful when re-throwing an InterruptedException is needed but don't want to force the
 * caller to handle the exception.
 */
public class InterruptedRuntimeException extends RuntimeException {

    /** Constructs the exception. */
    public InterruptedRuntimeException() {
        super();
    }

    /** Constructs the exception with a message. */
    public InterruptedRuntimeException(@NonNull String message) {
        super(message);
    }

    /** Constructs the exception with a message and a cause. */
    public InterruptedRuntimeException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }

    /** Constructs the exception with a cause. */
    public InterruptedRuntimeException(@NonNull Throwable cause) {
        super(cause);
    }
}
