/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.impl;

import android.arch.background.workmanager.Worker;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * Listener that reports the result of a {@link Worker}'s execution.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ExecutionListener {
    /**
     * Called when a {@link Worker} has executed.
     * @param workSpecId work id corresponding to the {@link Worker}
     * @param needsReschedule {@code true} if work should be rescheduled
     *                        according to backoff policy.
     */
    void onExecuted(@NonNull String workSpecId, boolean needsReschedule);
}
