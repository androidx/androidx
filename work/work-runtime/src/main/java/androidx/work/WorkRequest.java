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

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.DurationApi26Impl;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The base class for specifying parameters for work that should be enqueued in {@link WorkManager}.
 * There are two concrete implementations of this class: {@link OneTimeWorkRequest} and
 * {@link PeriodicWorkRequest}.
 */

public abstract class WorkRequest {

    /**
     * The default initial backoff time (in milliseconds) for work that has to be retried.
     */
    public static final long DEFAULT_BACKOFF_DELAY_MILLIS = 30000L;

    /**
     * The maximum backoff time (in milliseconds) for work that has to be retried.
     */
    @SuppressLint("MinMaxConstant")
    public static final long MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000; // 5 hours.

    /**
     * The minimum backoff time for work (in milliseconds) that has to be retried.
     */
    @SuppressLint("MinMaxConstant")
    public static final long MIN_BACKOFF_MILLIS = 10 * 1000; // 10 seconds.

    private @NonNull UUID mId;
    private @NonNull WorkSpec mWorkSpec;
    private @NonNull Set<String> mTags;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected WorkRequest(@NonNull UUID id, @NonNull WorkSpec workSpec, @NonNull Set<String> tags) {
        mId = id;
        mWorkSpec = workSpec;
        mTags = tags;
    }

    /**
     * Gets the unique identifier associated with this unit of work.
     *
     * @return The identifier for this unit of work
     */
    public @NonNull UUID getId() {
        return mId;
    }

    /**
     * Gets the string for the unique identifier associated with this unit of work.
     *
     * @return The string identifier for this unit of work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull String getStringId() {
        return mId.toString();
    }

    /**
     * Gets the {@link WorkSpec} associated with this unit of work.
     *
     * @return The {@link WorkSpec} for this unit of work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    /**
     * Gets the tags associated with this unit of work.
     *
     * @return The tags for this unit of work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Set<String> getTags() {
        return mTags;
    }

    /**
     * A builder for {@link WorkRequest}s.  There are two concrete implementations of this class:
     * {@link OneTimeWorkRequest.Builder} and {@link PeriodicWorkRequest.Builder}.
     *
     * @param <B> The concrete implementation of this Builder
     * @param <W> The type of work object built by this Builder
     */
    public abstract static class Builder<B extends Builder<?, ?>, W extends WorkRequest> {

        boolean mBackoffCriteriaSet = false;
        UUID mId;
        WorkSpec mWorkSpec;
        Set<String> mTags = new HashSet<>();
        Class<? extends ListenableWorker> mWorkerClass;

        Builder(@NonNull Class<? extends ListenableWorker> workerClass) {
            mId = UUID.randomUUID();
            mWorkerClass = workerClass;
            mWorkSpec = new WorkSpec(mId.toString(), workerClass.getName());
            addTag(workerClass.getName());
        }

        /**
         * Sets the backoff policy and backoff delay for the work.  The default values are
         * {@link BackoffPolicy#EXPONENTIAL} and
         * {@value WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS}, respectively.  {@code backoffDelay}
         * will be clamped between {@link WorkRequest#MIN_BACKOFF_MILLIS} and
         * {@link WorkRequest#MAX_BACKOFF_MILLIS}.
         *
         * @param backoffPolicy The {@link BackoffPolicy} to use when increasing backoff time
         * @param backoffDelay Time to wait before retrying the work in {@code timeUnit} units
         * @param timeUnit The {@link TimeUnit} for {@code backoffDelay}
         * @return The current {@link Builder}
         */
        public final @NonNull B setBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                long backoffDelay,
                @NonNull TimeUnit timeUnit) {
            mBackoffCriteriaSet = true;
            mWorkSpec.backoffPolicy = backoffPolicy;
            mWorkSpec.setBackoffDelayDuration(timeUnit.toMillis(backoffDelay));
            return getThis();
        }

        /**
         * Sets the backoff policy and backoff delay for the work.  The default values are
         * {@link BackoffPolicy#EXPONENTIAL} and
         * {@value WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS}, respectively.  {@code duration} will
         * be clamped between {@link WorkRequest#MIN_BACKOFF_MILLIS} and
         * {@link WorkRequest#MAX_BACKOFF_MILLIS}.
         *
         * @param backoffPolicy The {@link BackoffPolicy} to use when increasing backoff time
         * @param duration Time to wait before retrying the work
         * @return The current {@link Builder}
         */
        @RequiresApi(26)
        public final @NonNull B setBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                @NonNull Duration duration) {
            mBackoffCriteriaSet = true;
            mWorkSpec.backoffPolicy = backoffPolicy;
            mWorkSpec.setBackoffDelayDuration(DurationApi26Impl.toMillisCompat(duration));
            return getThis();
        }

        /**
         * Adds constraints to the {@link WorkRequest}.
         *
         * @param constraints The constraints for the work
         * @return The current {@link Builder}
         */
        public final @NonNull B setConstraints(@NonNull Constraints constraints) {
            mWorkSpec.constraints = constraints;
            return getThis();
        }

        /**
         * Adds input {@link Data} to the work.  If a worker has prerequisites in its chain, this
         * Data will be merged with the outputs of the prerequisites using an {@link InputMerger}.
         *
         * @param inputData key/value pairs that will be provided to the worker
         * @return The current {@link Builder}
         */
        public final @NonNull B setInputData(@NonNull Data inputData) {
            mWorkSpec.input = inputData;
            return getThis();
        }

        /**
         * Adds a tag for the work.  You can query and cancel work by tags.  Tags are particularly
         * useful for modules or libraries to find and operate on their own work.
         *
         * @param tag A tag for identifying the work in queries.
         * @return The current {@link Builder}
         */
        public final @NonNull B addTag(@NonNull String tag) {
            mTags.add(tag);
            return getThis();
        }

        /**
         * Specifies that the results of this work should be kept for at least the specified amount
         * of time.  After this time has elapsed, the results <b>may</b> be pruned at the discretion
         * of WorkManager when there are no pending dependent jobs.
         * <p>
         * When the results of a work are pruned, it becomes impossible to query for its
         * {@link WorkInfo}.
         * <p>
         * Specifying a long duration here may adversely affect performance in terms of app storage
         * and database query time.
         *
         * @param duration The minimum duration of time (in {@code timeUnit} units) to keep the
         *                 results of this work
         * @param timeUnit The unit of time for {@code duration}
         * @return The current {@link Builder}
         */
        public final @NonNull B keepResultsForAtLeast(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.minimumRetentionDuration = timeUnit.toMillis(duration);
            return getThis();
        }

        /**
         * Specifies that the results of this work should be kept for at least the specified amount
         * of time.  After this time has elapsed, the results <p>may</p> be pruned at the discretion
         * of WorkManager when this WorkRequest has reached a finished state (see
         * {@link WorkInfo.State#isFinished()}) and there are no pending dependent jobs.
         * <p>
         * When the results of a work are pruned, it becomes impossible to query for its
         * {@link WorkInfo}.
         * <p>
         * Specifying a long duration here may adversely affect performance in terms of app storage
         * and database query time.
         *
         * @param duration The minimum duration of time to keep the results of this work
         * @return The current {@link Builder}
         */
        @RequiresApi(26)
        public final @NonNull B keepResultsForAtLeast(@NonNull Duration duration) {
            mWorkSpec.minimumRetentionDuration = DurationApi26Impl.toMillisCompat(duration);
            return getThis();
        }

        /**
         * Sets an initial delay for the {@link WorkRequest}.
         *
         * @param duration The length of the delay in {@code timeUnit} units
         * @param timeUnit The units of time for {@code duration}
         * @return The current {@link Builder}
         * @throws IllegalArgumentException if the given initial delay will push the execution time
         *         past {@code Long.MAX_VALUE} and cause an overflow
         */
        public @NonNull B setInitialDelay(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.initialDelay = timeUnit.toMillis(duration);
            if (Long.MAX_VALUE - System.currentTimeMillis() <= mWorkSpec.initialDelay) {
                throw new IllegalArgumentException("The given initial delay is too large and will"
                        + " cause an overflow!");
            }
            return getThis();
        }

        /**
         * Sets an initial delay for the {@link WorkRequest}.
         *
         * @param duration The length of the delay
         * @return The current {@link Builder}         *
         * @throws IllegalArgumentException if the given initial delay will push the execution time
         *         past {@code Long.MAX_VALUE} and cause an overflow
         */
        @RequiresApi(26)
        public @NonNull B setInitialDelay(@NonNull Duration duration) {
            mWorkSpec.initialDelay = DurationApi26Impl.toMillisCompat(duration);
            if (Long.MAX_VALUE - System.currentTimeMillis() <= mWorkSpec.initialDelay) {
                throw new IllegalArgumentException("The given initial delay is too large and will"
                        + " cause an overflow!");
            }
            return getThis();
        }

        /**
         * Marks the {@link WorkRequest} as important to the user.  In this case, WorkManager
         * provides an additional signal to the OS that this work is important.
         *
         * @param policy The {@link OutOfQuotaPolicy} to be used.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull B setExpedited(@NonNull OutOfQuotaPolicy policy) {
            mWorkSpec.expedited = true;
            mWorkSpec.outOfQuotaPolicy = policy;
            return getThis();
        }

        /**
         * Builds a {@link WorkRequest} based on this {@link Builder}.
         *
         * @return A {@link WorkRequest} based on this {@link Builder}
         */
        public final @NonNull W build() {
            W returnValue = buildInternal();
            Constraints constraints = mWorkSpec.constraints;
            // Check for unsupported constraints.
            boolean hasUnsupportedConstraints =
                    (Build.VERSION.SDK_INT >= 24 && constraints.hasContentUriTriggers())
                            || constraints.requiresBatteryNotLow()
                            || constraints.requiresCharging()
                            || (Build.VERSION.SDK_INT >= 23 && constraints.requiresDeviceIdle());

            if (mWorkSpec.expedited) {
                if (hasUnsupportedConstraints) {
                    throw new IllegalArgumentException(
                            "Expedited jobs only support network and storage constraints");
                }
                if (mWorkSpec.initialDelay > 0) {
                    throw new IllegalArgumentException("Expedited jobs cannot be delayed");
                }
            }
            // Create a new id and WorkSpec so this WorkRequest.Builder can be used multiple times.
            mId = UUID.randomUUID();
            mWorkSpec = new WorkSpec(mWorkSpec);
            mWorkSpec.id = mId.toString();
            return returnValue;
        }

        abstract @NonNull W buildInternal();

        abstract @NonNull B getThis();

        /**
         * Sets the initial state for this work.  Used in testing only.
         *
         * @param state The {@link WorkInfo.State} to set
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public final @NonNull B setInitialState(@NonNull WorkInfo.State state) {
            mWorkSpec.state = state;
            return getThis();
        }

        /**
         * Sets the initial run attempt count for this work.  Used in testing only.
         *
         * @param runAttemptCount The initial run attempt count
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public final @NonNull B setInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.runAttemptCount = runAttemptCount;
            return getThis();
        }

        /**
         * Sets the period start time for this work. Used in testing only.
         *
         * @param periodStartTime the period start time in {@code timeUnit} units
         * @param timeUnit The {@link TimeUnit} for {@code periodStartTime}
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public final @NonNull B setPeriodStartTime(
                long periodStartTime,
                @NonNull TimeUnit timeUnit) {
            mWorkSpec.periodStartTime = timeUnit.toMillis(periodStartTime);
            return getThis();
        }

        /**
         * Sets when the scheduler actually schedules the worker.
         *
         * @param scheduleRequestedAt The time at which the scheduler scheduled a worker.
         * @param timeUnit            The {@link TimeUnit} for {@code scheduleRequestedAt}
         * @return The current {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public final @NonNull B setScheduleRequestedAt(
                long scheduleRequestedAt,
                @NonNull TimeUnit timeUnit) {
            mWorkSpec.scheduleRequestedAt = timeUnit.toMillis(scheduleRequestedAt);
            return getThis();
        }
    }
}
