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
package androidx.work;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;

/**
 * The base interface for units of work.
 */

public interface BaseWork {

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#77}
     */
    long DEFAULT_BACKOFF_DELAY_MILLIS = 30000L;

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#82}
     */
    long MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000; // 5 hours.

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#119}
     */
    long MIN_BACKOFF_MILLIS = 10 * 1000; // 10 seconds.

    /**
     * Gets the unique identifier associated with this unit of work.
     *
     * @return The identifier for this unit of work
     */
    String getId();

    /**
     * A builder for {@link BaseWork}.
     *
     * @param <W> The {@link BaseWork} class being created by this builder
     * @param <B> The concrete implementation of this builder
     */
    interface Builder<W, B extends Builder<W, B>> {

        /**
         * Change backoff policy and delay for the work.  The default is
         * {@link BackoffPolicy#EXPONENTIAL} and {@value BaseWork#DEFAULT_BACKOFF_DELAY_MILLIS}.
         * The maximum backoff delay duration is {@value BaseWork#MAX_BACKOFF_MILLIS}.
         *
         * @param backoffPolicy The {@link BackoffPolicy} to use for work
         * @param backoffDelay Time to wait before restarting {@link Worker} in {@code timeUnit}
         *                     units
         * @param timeUnit The {@link TimeUnit} for {@code backoffDelay}
         * @return The current {@link Builder}.
         */
        B withBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                long backoffDelay,
                @NonNull TimeUnit timeUnit);

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
         * @return The current {@link Builder}.
         */
        B withArguments(@NonNull Arguments arguments);

        /**
         * Add an optional tag for the work.  This is particularly useful for modules or
         * libraries who want to query for or cancel all of their own work.
         *
         * @param tag A tag for identifying the work in queries.
         * @return The current {@link Builder}.
         */
        B addTag(@NonNull String tag);

        /**
         * Specifies that the results of this work should be kept for at least the specified amount
         * of time.  After this time has elapsed, the results may be pruned at the discretion of
         * WorkManager when there are no pending dependent jobs.
         *
         * When the results of a work are pruned, it becomes impossible to query for its
         * {@link WorkStatus}.
         *
         * Specifying a long duration here may adversely affect performance in terms of app storage
         * and database query time.
         *
         * @param duration The minimum duration of time (in {@code timeUnit} units) to keep the
         *                 results of this work
         * @param timeUnit The unit of time for {@code duration}
         * @return The current {@link Builder}
         */
        B keepResultsForAtLeast(long duration, @NonNull TimeUnit timeUnit);

        /**
         * Builds this work object.
         *
         * @return The concrete implementation of the work associated with this builder
         */
        W build();

        /**
         * Set the initial state for this work.  Used in testing only.
         *
         * @param state The {@link State} to set
         * @return The current {@link Builder}
         */
        @VisibleForTesting
        B withInitialState(@NonNull State state);

        /**
         * Set the initial run attempt count for this work.  Used in testing only.
         *
         * @param runAttemptCount The initial run attempt count
         * @return The current {@link Builder}
         */
        @VisibleForTesting
        B withInitialRunAttemptCount(int runAttemptCount);

        /**
         * Set the period start time for this work. Used in testing only.
         *
         * @param periodStartTime the period start time in {@code timeUnit} units
         * @param timeUnit The {@link TimeUnit} for {@code periodStartTime}
         * @return The current {@link Builder}
         */
        @VisibleForTesting
        B withPeriodStartTime(long periodStartTime, @NonNull TimeUnit timeUnit);
    }

    /**
     * A builder for {@link Work}.
     *
     * @param <W> The {@link Work} class being created by this builder
     * @param <B> The concrete implementation of this builder
     */
    interface WorkBuilder<W, B extends Builder<W, B>> extends Builder<W, B> {

        /**
         * Specify whether {@link Work} should run with an initial delay. The default is 0 ms.
         *
         * @param duration The initial delay before running WorkSpec in {@code timeUnit} units
         * @param timeUnit The {@link TimeUnit} for {@code duration}
         * @return The current {@link WorkBuilder}
         */
        B withInitialDelay(long duration, @NonNull TimeUnit timeUnit);

        /**
         * Specify an {@link InputMerger}.  The default is {@link OverwritingInputMerger}.
         *
         * @param inputMerger The class name of the {@link InputMerger} to use for this {@link Work}
         * @return The current {@link WorkBuilder}
         */
        B withInputMerger(@NonNull Class<? extends InputMerger> inputMerger);
    }
}
