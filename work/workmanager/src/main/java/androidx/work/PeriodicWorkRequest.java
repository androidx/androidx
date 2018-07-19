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
import android.support.annotation.RequiresApi;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A class that represents a request for repeating work.
 */

public final class PeriodicWorkRequest extends WorkRequest {

    /**
     * The minimum interval duration for {@link PeriodicWorkRequest} (in milliseconds).
     */
    public static final long MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L; // 15 minutes.
    /**
     * The minimum flex duration for {@link PeriodicWorkRequest} (in milliseconds).
     */
    public static final long MIN_PERIODIC_FLEX_MILLIS = 5 * 60 * 1000L; // 5 minutes.

    PeriodicWorkRequest(Builder builder) {
        super(builder.mId, builder.mWorkSpec, builder.mTags);
    }

    /**
     * Builder for {@link PeriodicWorkRequest} class.
     */
    public static final class Builder extends WorkRequest.Builder<Builder, PeriodicWorkRequest> {

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once every interval period. The
         * {@link PeriodicWorkRequest} is guaranteed to run exactly one time during this interval
         * (subject to OS battery optimizations, such as doze mode). The {@code intervalMillis} must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS}. It
         * may run immediately, at the end of the period, or any time in between so long as the
         * other conditions are satisfied at the time. The run time of the
         * {@link PeriodicWorkRequest} can be restricted to a flex period within an interval.
         *
         * @param workerClass The {@link Worker} class to run with this job
         * @param repeatInterval The repeat interval in {@code repeatIntervalTimeUnit} units
         * @param repeatIntervalTimeUnit The {@link TimeUnit} for {@code repeatInterval}
         */
        public Builder(
                @NonNull Class<? extends Worker> workerClass,
                long repeatInterval,
                @NonNull TimeUnit repeatIntervalTimeUnit) {
            super(workerClass);
            mWorkSpec.setPeriodic(repeatIntervalTimeUnit.toMillis(repeatInterval));
        }

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once every interval period. The
         * {@link PeriodicWorkRequest} is guaranteed to run exactly one time during this interval
         * (subject to OS battery optimizations, such as doze mode). The {@code intervalMillis} must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS}. It
         * may run immediately, at the end of the period, or any time in between so long as the
         * other conditions are satisfied at the time. The run time of the
         * {@link PeriodicWorkRequest} can be restricted to a flex period within an interval.
         *
         * @param workerClass The {@link Worker} class to run with this job
         * @param repeatInterval The repeat interval
         */
        @RequiresApi(26)
        public Builder(
                @NonNull Class<? extends Worker> workerClass,
                @NonNull Duration repeatInterval) {
            super(workerClass);
            mWorkSpec.setPeriodic(repeatInterval.toMillis());
        }

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once within the
         * <strong>flex period</strong> of every interval period. See diagram below. The flex period
         * begins at {@code intervalMillis - flexMillis} to the end of the interval.
         * {@code intervalMillis} must be greater than or equal to
         * {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS} and {@code flexMillis} must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_FLEX_MILLIS}.
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
            super(workerClass);
            mWorkSpec.setPeriodic(
                    repeatIntervalTimeUnit.toMillis(repeatInterval),
                    flexIntervalTimeUnit.toMillis(flexInterval));
        }

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once within the
         * <strong>flex period</strong> of every interval period. See diagram below. The flex period
         * begins at {@code intervalMillis - flexMillis} to the end of the interval.
         * {@code intervalMillis} must be greater than or equal to
         * {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS} and {@code flexMillis} must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_FLEX_MILLIS}.
         *
         * <p><pre>
         * [     before flex     |     flex     ][     before flex     |     flex     ]...
         * [   cannot run work   | can run work ][   cannot run work   | can run work ]...
         * \____________________________________/\____________________________________/...
         *                interval 1                            interval 2             ...(repeat)
         * </pre></p>
         *
         * @param workerClass The {@link Worker} class to run with this job
         * @param repeatInterval The repeat interval
         * @param flexInterval The duration in for which this work repeats from the end of the
         *                     {@code repeatInterval}
         */
        @RequiresApi(26)
        public Builder(
                @NonNull Class<? extends Worker> workerClass,
                @NonNull Duration repeatInterval,
                @NonNull Duration flexInterval) {
            super(workerClass);
            mWorkSpec.setPeriodic(repeatInterval.toMillis(), flexInterval.toMillis());
        }

        @Override
        public @NonNull PeriodicWorkRequest build() {
            if (mBackoffCriteriaSet
                    && Build.VERSION.SDK_INT >= 23
                    && mWorkSpec.constraints.requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            return new PeriodicWorkRequest(this);
        }

        @Override
        @NonNull Builder getThis() {
            return this;
        }
    }
}
