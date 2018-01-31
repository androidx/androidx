/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.arch.background.workmanager.impl.PeriodicWorkImpl;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

/**
 * A class to execute a logical unit of repeating work.
 */

public abstract class PeriodicWork implements BaseWork {

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

    /**
     * Builder for {@link PeriodicWork} class.
     */
    public static class Builder implements BaseWork.Builder<PeriodicWork, Builder> {

        private PeriodicWorkImpl.Builder mInternalBuilder;

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
         * @param intervalMillis Duration in milliseconds for which {@link Work} repeats
         */
        public Builder(Class<? extends Worker> workerClass, long intervalMillis) {
            mInternalBuilder = new PeriodicWorkImpl.Builder(workerClass, intervalMillis);
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
         * @param intervalMillis Duration in milliseconds of the interval
         * @param flexMillis Duration in milliseconds for which {@link Work} repeats from the end of
         *                   the interval
         */
        public Builder(Class<? extends Worker> workerClass, long intervalMillis, long flexMillis) {
            mInternalBuilder =
                    new PeriodicWorkImpl.Builder(workerClass, intervalMillis, flexMillis);
        }

        @Override
        public Builder withBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                long backoffDelayMillis) {
            mInternalBuilder.withBackoffCriteria(backoffPolicy, backoffDelayMillis);
            return this;
        }

        @Override
        public Builder withConstraints(@NonNull Constraints constraints) {
            mInternalBuilder.withConstraints(constraints);
            return this;
        }

        @Override
        public Builder withArguments(@NonNull Arguments arguments) {
            mInternalBuilder.withArguments(arguments);
            return this;
        }

        @Override
        public Builder addTag(@NonNull String tag) {
            mInternalBuilder.addTag(tag);
            return this;
        }

        @Override
        public PeriodicWork build() {
            return mInternalBuilder.build();
        }

        @VisibleForTesting
        @Override
        public Builder withInitialStatus(@NonNull WorkStatus status) {
            mInternalBuilder.withInitialStatus(status);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withInitialRunAttemptCount(int runAttemptCount) {
            mInternalBuilder.withInitialRunAttemptCount(runAttemptCount);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withPeriodStartTime(long periodStartTime) {
            mInternalBuilder.withPeriodStartTime(periodStartTime);
            return this;
        }
    }
}
