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

package android.arch.background.workmanager;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

/**
 * A builder for {@link Constants}.
 *
 * @param <W> The {@link Constants} class being created by this builder
 * @param <B> The concrete implementation of this builder
 */

public interface BaseWorkBuilder<W, B extends BaseWorkBuilder<W, B>> {

    /**
     * Set the initial status for this work.  Used in testing only.
     *
     * @param status The {@link Constants.WorkStatus} to set
     * @return The current {@link BaseWorkBuilder}
     */
    @VisibleForTesting
    B withInitialStatus(@Constants.WorkStatus int status);

    /**
     * Set the initial run attempt count for this work.  Used in testing only.
     *
     * @param runAttemptCount The initial run attempt count
     * @return The current {@link BaseWorkBuilder}
     */
    @VisibleForTesting
    B withInitialRunAttemptCount(int runAttemptCount);

    /**
     * Set the period start time for this work. Used in testing only.
     *
     * @param periodStartTime the period start time
     * @return The current {@link BaseWorkBuilder}
     */
    @VisibleForTesting
    B withPeriodStartTime(long periodStartTime);

    /**
     * Change backoff policy and delay for the work.
     * Default is {@value Constants#BACKOFF_POLICY_EXPONENTIAL} and 30 seconds.
     * Maximum backoff delay duration is {@value Constants#MAX_BACKOFF_MILLIS}.
     *
     * @param backoffPolicy Backoff Policy to use for work
     * @param backoffDelayMillis Time to wait before restarting {@link Worker} (in milliseconds)
     * @return The current {@link BaseWorkBuilder}.
     */
    B withBackoffCriteria(@Constants.BackoffPolicy int backoffPolicy,
            long backoffDelayMillis);

    /**
     * Add constraints to the {@link Work}.
     *
     * @param constraints The constraints for the work
     * @return The current {@link Work.Builder}.
     */
    B withConstraints(@NonNull Constraints constraints);

    /**
     * Add arguments to the work.
     *
     * @param arguments key/value pairs that will be provided to the {@link Worker} class
     * @return The current {@link BaseWorkBuilder}.
     */
    B withArguments(@NonNull Arguments arguments);

    /**
     * Add an optional tag for the work.  This is particularly useful for modules or
     * libraries who want to query for or cancel all of their own work.
     *
     * @param tag A tag for identifying the work in queries.
     * @return The current {@link BaseWorkBuilder}.
     */
    B addTag(String tag);

    /**
     * Builds this work object.
     *
     * @return The concrete implementation of the work associated with this builder
     */
    W build();
}
