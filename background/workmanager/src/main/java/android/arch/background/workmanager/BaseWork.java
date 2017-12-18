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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.lang.annotation.Retention;

/**
 * The base interface for units of work.
 */

public interface BaseWork {

    @Retention(SOURCE)
    @IntDef({STATUS_ENQUEUED, STATUS_RUNNING, STATUS_SUCCEEDED, STATUS_FAILED, STATUS_BLOCKED,
            STATUS_CANCELLED})
    @interface WorkStatus {
    }

    @Retention(SOURCE)
    @IntDef({BACKOFF_POLICY_EXPONENTIAL, BACKOFF_POLICY_LINEAR})
    @interface BackoffPolicy {
    }

    int STATUS_ENQUEUED = 0;
    int STATUS_RUNNING = 1;
    int STATUS_SUCCEEDED = 2;
    int STATUS_FAILED = 3;
    int STATUS_BLOCKED = 4;
    int STATUS_CANCELLED = 5;

    int BACKOFF_POLICY_EXPONENTIAL = 0;
    int BACKOFF_POLICY_LINEAR = 1;
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
         * Set the initial status for this work.  Used in testing only.
         *
         * @param status The {@link WorkStatus} to set
         * @return The current {@link Builder}
         */
        @VisibleForTesting
        B withInitialStatus(@WorkStatus int status);

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
         * @param periodStartTime the period start time
         * @return The current {@link Builder}
         */
        @VisibleForTesting
        B withPeriodStartTime(long periodStartTime);

        /**
         * Change backoff policy and delay for the work.
         * Default is {@value BaseWork#BACKOFF_POLICY_EXPONENTIAL} and 30 seconds.
         * Maximum backoff delay duration is {@value BaseWork#MAX_BACKOFF_MILLIS}.
         *
         * @param backoffPolicy Backoff Policy to use for work
         * @param backoffDelayMillis Time to wait before restarting {@link Worker} (in milliseconds)
         * @return The current {@link Builder}.
         */
        B withBackoffCriteria(@BackoffPolicy int backoffPolicy,
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
         * Builds this work object.
         *
         * @return The concrete implementation of the work associated with this builder
         */
        W build();
    }
}
