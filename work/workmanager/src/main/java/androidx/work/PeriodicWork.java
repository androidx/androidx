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

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import androidx.work.impl.model.WorkSpec;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A class to execute a logical unit of repeating work.
 */

public class PeriodicWork extends BaseWork {

    /**
     * The minimum interval duration for {@link PeriodicWork}, in milliseconds.
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#110}.
     */
    public static final long MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L; // 15 minutes.
    /**
     * The minimum flex duration for {@link PeriodicWork}, in milliseconds.
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#113}.
     */
    public static final long MIN_PERIODIC_FLEX_MILLIS = 5 * 60 * 1000L; // 5 minutes.

    PeriodicWork(Builder builder) {
        super(builder.mWorkSpec, builder.mTags);
    }

    /**
     * Builder for {@link PeriodicWork} class.
     */
    public static class Builder implements BaseWork.Builder<PeriodicWork, Builder> {

        private boolean mBackoffCriteriaSet = false;
        WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());
        Set<String> mTags = new HashSet<>();

        /**
         * Creates a {@link PeriodicWork} to run periodically once every interval period. The
         * {@link PeriodicWork} is guaranteed to run exactly one time during this interval. The
         * {@code intervalMillis} must be greater than or equal to
         * {@link PeriodicWork#MIN_PERIODIC_INTERVAL_MILLIS}. It may run immediately, at the end of
         * the period, or any time in between so long as the other conditions are satisfied at the
         * time. The run time of the {@link PeriodicWork} can be restricted to a flex period within
         * an interval.
         *
         * @param workerClass The {@link Worker} class to run with this job
         * @param repeatInterval The repeat interval in {@code repeatIntervalTimeUnit} units
         * @param repeatIntervalTimeUnit The {@link TimeUnit} for {@code repeatInterval}
         */
        public Builder(
                @NonNull Class<? extends Worker> workerClass,
                long repeatInterval,
                @NonNull TimeUnit repeatIntervalTimeUnit) {
            mWorkSpec.setWorkerClassName(workerClass.getName());
            mWorkSpec.setPeriodic(repeatIntervalTimeUnit.toMillis(repeatInterval));
        }

        /**
         * Creates a {@link PeriodicWork} to run periodically once within the
         * <strong>flex period</strong> of every interval period. See diagram below. The flex period
         * begins at {@code intervalMillis - flexMillis} to the end of the interval.
         * {@code intervalMillis} must be greater than or equal to
         * {@link PeriodicWork#MIN_PERIODIC_INTERVAL_MILLIS} and {@code flexMillis} must
         * be greater than or equal to {@link PeriodicWork#MIN_PERIODIC_FLEX_MILLIS}.
         *
         * <p><pre>
         * [     before flex     |     flex     ][     before flex     |     flex     ]...
         * [   cannot run work   | can run work ][   cannot run work   | can run work ]...
         * \____________________________________/\____________________________________/...
         *                interval 1                            interval 2             ...(repeat)
         * </pre></p>
         *
         * @param workerClass The {@link Worker} class to run with this job
         * @param repeatInterval The repeat interval in {@code repeatIntervalTimeUnit} units
         * @param repeatIntervalTimeUnit The {@link TimeUnit} for {@code repeatInterval}
         * @param flexInterval The duration in {@code flexIntervalTimeUnit} units for which this
         *                     work repeats from the end of the {@code repeatInterval}
         * @param flexIntervalTimeUnit The {@link TimeUnit} for {@code flexInterval}
         */
        public Builder(
                @NonNull Class<? extends Worker> workerClass,
                long repeatInterval,
                @NonNull TimeUnit repeatIntervalTimeUnit,
                long flexInterval,
                @NonNull TimeUnit flexIntervalTimeUnit) {
            mWorkSpec.setWorkerClassName(workerClass.getName());
            mWorkSpec.setPeriodic(
                    repeatIntervalTimeUnit.toMillis(repeatInterval),
                    flexIntervalTimeUnit.toMillis(flexInterval));
        }

        @VisibleForTesting
        @Override
        public Builder withInitialState(@NonNull State state) {
            mWorkSpec.setState(state);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.setRunAttemptCount(runAttemptCount);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withPeriodStartTime(long periodStartTime, @NonNull TimeUnit timeUnit) {
            mWorkSpec.setPeriodStartTime(timeUnit.toMillis(periodStartTime));
            return this;
        }

        @Override
        public Builder withBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                long backoffDelay,
                @NonNull TimeUnit timeUnit) {
            mBackoffCriteriaSet = true;
            mWorkSpec.setBackoffPolicy(backoffPolicy);
            mWorkSpec.setBackoffDelayDuration(timeUnit.toMillis(backoffDelay));
            return this;
        }

        @Override
        public Builder withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.setConstraints(constraints);
            return this;
        }

        @Override
        public Builder withArguments(@NonNull Arguments arguments) {
            mWorkSpec.setArguments(arguments);
            return this;
        }

        @Override
        public Builder addTag(@NonNull String tag) {
            mTags.add(tag);
            return this;
        }

        @Override
        public Builder keepResultsForAtLeast(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.setMinimumRetentionDuration(timeUnit.toMillis(duration));
            return this;
        }

        @Override
        public PeriodicWork build() {
            if (mBackoffCriteriaSet
                    && Build.VERSION.SDK_INT >= 23
                    && mWorkSpec.getConstraints().requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            return new PeriodicWork(this);
        }
    }
}
