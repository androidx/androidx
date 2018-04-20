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
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import androidx.work.impl.model.WorkSpec;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The base interface for work requests.
 */

public abstract class WorkRequest {

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#77}
     */
    public static final long DEFAULT_BACKOFF_DELAY_MILLIS = 30000L;

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#82}
     */
    public static final long MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000; // 5 hours.

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#119}
     */
    public static final long MIN_BACKOFF_MILLIS = 10 * 1000; // 10 seconds.


    private WorkSpec mWorkSpec;
    private Set<String> mTags;

    protected WorkRequest(@NonNull WorkSpec workSpec, @NonNull Set<String> tags) {
        mWorkSpec = workSpec;
        mTags = tags;
    }

    /**
     * Gets the unique identifier associated with this unit of work.
     *
     * @return The identifier for this unit of work
     */
    public String getId() {
        return mWorkSpec.id;
    }

    /**
     * Gets the {@link WorkSpec} associated with this unit of work.
     *
     * @return The {@link WorkSpec} for this unit of work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    /**
     * Gets the tags associated with this unit of work.
     *
     * @return The tags for this unit of work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Set<String> getTags() {
        return mTags;
    }

    /**
     * A builder for {@link WorkRequest}.
     *
     * @param <B> The concrete implementation of of this Builder
     * @param <W> The type of work object built by this Builder
     */
    public abstract static class Builder<B extends Builder, W extends WorkRequest> {

        protected boolean mBackoffCriteriaSet = false;
        protected WorkSpec mWorkSpec;
        protected Set<String> mTags = new HashSet<>();

        public Builder(@NonNull Class<? extends Worker> workerClass) {
            mWorkSpec = new WorkSpec(UUID.randomUUID().toString(), workerClass.getName());
        }

        /**
         * Change backoff policy and delay for the work.  The default is
         * {@link BackoffPolicy#EXPONENTIAL} and
         * {@value WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS}.  The maximum backoff delay
         * duration is {@value WorkRequest#MAX_BACKOFF_MILLIS}.
         *
         * @param backoffPolicy The {@link BackoffPolicy} to use for work
         * @param backoffDelay Time to wait before restarting {@link Worker} in {@code timeUnit}
         *                     units
         * @param timeUnit The {@link TimeUnit} for {@code backoffDelay}
         * @return The current {@link Builder}
         */
        public B withBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                long backoffDelay,
                @NonNull TimeUnit timeUnit) {
            mBackoffCriteriaSet = true;
            mWorkSpec.backoffPolicy = backoffPolicy;
            mWorkSpec.setBackoffDelayDuration(timeUnit.toMillis(backoffDelay));
            return getThis();
        }

        /**
         * Add constraints to the {@link OneTimeWorkRequest}.
         *
         * @param constraints The constraints for the work
         * @return The current {@link Builder}
         */
        public B withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.constraints = constraints;
            return getThis();
        }

        /**
         * Add input {@link Data} to the work.
         *
         * @param inputData key/value pairs that will be provided to the {@link Worker} class
         * @return The current {@link Builder}
         */
        public B withInputData(@NonNull Data inputData) {
            mWorkSpec.input = inputData;
            return getThis();
        }

        /**
         * Add an optional tag for the work.  This is particularly useful for modules or
         * libraries who want to query for or cancel all of their own work.
         *
         * @param tag A tag for identifying the work in queries.
         * @return The current {@link Builder}
         */
        public B addTag(@NonNull String tag) {
            mTags.add(tag);
            return getThis();
        }

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
        public B keepResultsForAtLeast(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.minimumRetentionDuration = timeUnit.toMillis(duration);
            return getThis();
        }

        /**
         * Builds this work object.
         *
         * @return The concrete implementation of the work associated with this builder
         */
        public abstract W build();

        protected abstract B getThis();

        /**
         * Set the initial state for this work.  Used in testing only.
         *
         * @param state The {@link State} to set
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public B withInitialState(@NonNull State state) {
            mWorkSpec.state = state;
            return getThis();
        }

        /**
         * Set the initial run attempt count for this work.  Used in testing only.
         *
         * @param runAttemptCount The initial run attempt count
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public B withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.runAttemptCount = runAttemptCount;
            return getThis();
        }

        /**
         * Set the period start time for this work. Used in testing only.
         *
         * @param periodStartTime the period start time in {@code timeUnit} units
         * @param timeUnit The {@link TimeUnit} for {@code periodStartTime}
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public B withPeriodStartTime(long periodStartTime, @NonNull TimeUnit timeUnit) {
            mWorkSpec.periodStartTime = timeUnit.toMillis(periodStartTime);
            return getThis();
        }

        /**
         * Set when the scheduler actually schedules the worker.
         *
         * @param scheduleRequestedAt The time at which the scheduler scheduled a worker.
         * @param timeUnit            The {@link TimeUnit} for {@code scheduleRequestedAt}
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public B withScheduleRequestedAt(
                long scheduleRequestedAt,
                @NonNull TimeUnit timeUnit) {
            mWorkSpec.scheduleRequestedAt = timeUnit.toMillis(scheduleRequestedAt);
            return getThis();
        }
    }
}
