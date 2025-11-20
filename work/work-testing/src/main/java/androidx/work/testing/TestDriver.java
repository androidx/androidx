/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.testing;

import androidx.work.StopReason;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Additional functionality exposed for {@link androidx.work.WorkManager} that are useful in the
 * context of testing.
 */
public interface TestDriver {

    /**
     * Tells {@link TestDriver} to pretend that all constraints on the
     * {@link androidx.work.WorkRequest} with the given {@code workSpecId} are met.  This may
     * trigger execution of the work.
     *
     * @param workSpecId The {@link androidx.work.WorkRequest}'s id
     * @throws IllegalArgumentException if {@code workSpecId} is not enqueued
     */
    void setAllConstraintsMet(@NonNull UUID workSpecId);

    /**
     * Tells {@link TestDriver} to pretend that the initial delay the
     * {@link androidx.work.OneTimeWorkRequest} with the given {@code workSpecId} is met.  This may
     * trigger execution of the work.
     *
     * @param workSpecId The {@link androidx.work.OneTimeWorkRequest}'s id
     * @throws IllegalArgumentException if {@code workSpecId} is not enqueued
     */
    void setInitialDelayMet(@NonNull UUID workSpecId);

    /**
     * Tells {@link TestDriver} to pretend that the period delay on the
     * {@link androidx.work.PeriodicWorkRequest} with the given {@code workSpecId} is met.  This may
     * trigger execution of the work.
     *
     * @param workSpecId The {@link androidx.work.PeriodicWorkRequest}'s id
     * @throws IllegalArgumentException if {@code workSpecId} is not enqueued
     */
    void setPeriodDelayMet(@NonNull UUID workSpecId);

    /**
     * Tells {@link TestDriver} to pretend that a running worker should be stopped with the provided
     * {@link StopReason}.
     *
     * @param workSpecId The {@link androidx.work.WorkRequest}'s id
     * @param reason The {@link StopReason} that will be available to the worker
     */
    default void stopRunningWorkWithReason(@NonNull UUID workSpecId, @StopReason int reason) {
        throw new UnsupportedOperationException("stopRunningWorkWithReason is not implemented");
    }
}
