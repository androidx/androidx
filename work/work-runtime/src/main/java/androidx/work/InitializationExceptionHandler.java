/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * An Exception Handler that can be used to determine if there were issues when trying
 * to initialize {@link WorkManager}.
 * <p>
 * This usually happens when WorkManager cannot access its internal datastore.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface InitializationExceptionHandler {
    /**
     * Allows the application to handle a {@link Throwable} throwable typically caused when
     * trying to initialize {@link WorkManager}.
     * <p>
     * This exception handler will be invoked a thread bound to
     * {@link Configuration#getTaskExecutor()}.
     *
     * @param throwable The underlying throwable that was caused when trying to initialize
     * {@link WorkManager}
     */
    void handleException(@NonNull Throwable throwable);
}
