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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Can be used to schedule {@link Runnable}s after a delay in milliseconds.
 * <br/>
 * This is used by the in-process scheduler to schedule timed work.
 */
public interface RunnableScheduler {

    /**
     * Schedules a {@link Runnable} to run after a delay (in milliseconds).
     *
     * @param delayInMillis The delay in milliseconds relative to the current time.
     * @param runnable      The {@link Runnable} to be scheduled
     */
    void scheduleWithDelay(@IntRange(from = 0) long delayInMillis, @NonNull Runnable runnable);

    /**
     * Cancels the {@link Runnable} which was previously scheduled using
     * {@link #scheduleWithDelay(long, Runnable)}.
     *
     * @param runnable The {@link Runnable} to be cancelled
     */
    void cancel(@NonNull Runnable runnable);
}
