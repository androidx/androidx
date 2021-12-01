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

import static androidx.work.impl.utils.DurationApi26Impl.toMillisCompat;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A {@link WorkRequest} for repeating work.  This work executes multiple times until it is
 * cancelled, with the first execution happening immediately or as soon as the given
 * {@link Constraints} are met.  The next execution will happen during the period interval; note
 * that execution may be delayed because {@link WorkManager} is subject to OS battery optimizations,
 * such as doze mode.
 * <p>
 * You can control when the work executes in the period interval more exactly - see
 * {@link PeriodicWorkRequest.Builder} for documentation on {@code flexInterval}s.
 * <p>
 * Periodic work has a minimum interval of 15 minutes.
 * <p>
 * Periodic work is intended for use cases where you want a fairly consistent delay between
 * consecutive runs, and you are willing to accept inexactness due to battery optimizations and doze
 * mode.  Please note that if your periodic work has constraints, it will not execute until the
 * constraints are met, even if the delay between periods has been met.
 * <p>
 * If you need to schedule work that happens exactly at a certain time or only during a certain time
 * window, you should consider using {@link OneTimeWorkRequest}s.
 * <p>
 * The normal lifecycle of a PeriodicWorkRequest is {@code ENQUEUED -> RUNNING -> ENQUEUED}.  By
 * definition, periodic work cannot terminate in a succeeded or failed state, since it must recur.
 * It can only terminate if explicitly cancelled.  However, in the case of retries, periodic work
 * will still back off according to
 * {@link PeriodicWorkRequest.Builder#setBackoffCriteria(BackoffPolicy, long, TimeUnit)}.
 * <p>
 * Periodic work cannot be part of a chain or graph of work.
 */

public final class PeriodicWorkRequest extends WorkRequest {

    /**
     * The minimum interval duration for {@link PeriodicWorkRequest} (in milliseconds).
     */
    @SuppressLint("MinMaxConstant")
    public static final long MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L; // 15 minutes.
    /**
     * The minimum flex duration for {@link PeriodicWorkRequest} (in milliseconds).
     */
    @SuppressLint("MinMaxConstant")
    public static final long MIN_PERIODIC_FLEX_MILLIS = 5 * 60 * 1000L; // 5 minutes.

    PeriodicWorkRequest(Builder builder) {
        super(builder.mId, builder.mWorkSpec, builder.mTags);
    }

    /**
     * Builder for {@link PeriodicWorkRequest}s.
     */
    public static final class Builder extends WorkRequest.Builder<Builder, PeriodicWorkRequest> {

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once every interval period. The
         * {@link PeriodicWorkRequest} is guaranteed to run exactly one time during this interval
         * (subject to OS battery optimizations, such as doze mode). The repeat interval must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS}. It
         * may run immediately, at the end of the period, or any time in between so long as the
         * other conditions are satisfied at the time. The run time of the
         * {@link PeriodicWorkRequest} can be restricted to a flex period within an interval (see
         * {@code #Builder(Class, long, TimeUnit, long, TimeUnit)}).
         *
         * @param workerClass The {@link ListenableWorker} class to run for this work
         * @param repeatInterval The repeat interval in {@code repeatIntervalTimeUnit} units
         * @param repeatIntervalTimeUnit The {@link TimeUnit} for {@code repeatInterval}
         */
        public Builder(
                @NonNull Class<? extends ListenableWorker> workerClass,
                long repeatInterval,
                @NonNull TimeUnit repeatIntervalTimeUnit) {
            super(workerClass);
            mWorkSpec.setPeriodic(repeatIntervalTimeUnit.toMillis(repeatInterval));
        }

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once every interval period. The
         * {@link PeriodicWorkRequest} is guaranteed to run exactly one time during this interval
         * (subject to OS battery optimizations, such as doze mode). The repeat interval must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS}. It
         * may run immediately, at the end of the period, or any time in between so long as the
         * other conditions are satisfied at the time. The run time of the
         * {@link PeriodicWorkRequest} can be restricted to a flex period within an interval (see
         * {@code #Builder(Class, Duration, Duration)}).
         *
         * @param workerClass The {@link ListenableWorker} class to run for this work
         * @param repeatInterval The repeat interval
         */
        @RequiresApi(26)
        public Builder(
                @NonNull Class<? extends ListenableWorker> workerClass,
                @NonNull Duration repeatInterval) {
            super(workerClass);
            mWorkSpec.setPeriodic(toMillisCompat(repeatInterval));
        }

        /**
         * Creates a {@link PeriodicWorkRequest} to run periodically once within the
         * <strong>flex period</strong> of every interval period. See diagram below.  The flex
         * period begins at {@code repeatInterval - flexInterval} to the end of the interval.
         * The repeat interval must be greater than or equal to
         * {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS} and the flex interval must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_FLEX_MILLIS}.
         *
         * <p><pre>
         * [     before flex     |     flex     ][     before flex     |     flex     ]...
         * [   cannot run work   | can run work ][   cannot run work   | can run work ]...
         * \____________________________________/\____________________________________/...
         *                interval 1                            interval 2             ...(repeat)
         * </pre></p>
         *
         * @param workerClass The {@link ListenableWorker} class to run for this work
         * @param repeatInterval The repeat interval in {@code repeatIntervalTimeUnit} units
         * @param repeatIntervalTimeUnit The {@link TimeUnit} for {@code repeatInterval}
         * @param flexInterval The duration in {@code flexIntervalTimeUnit} units for which this
         *                     work repeats from the end of the {@code repeatInterval}
         * @param flexIntervalTimeUnit The {@link TimeUnit} for {@code flexInterval}
         */
        public Builder(
                @NonNull Class<? extends ListenableWorker> workerClass,
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
         * <strong>flex period</strong> of every interval period. See diagram below.  The flex
         * period begins at {@code repeatInterval - flexInterval} to the end of the interval.
         * The repeat interval must be greater than or equal to
         * {@link PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS} and the flex interval must
         * be greater than or equal to {@link PeriodicWorkRequest#MIN_PERIODIC_FLEX_MILLIS}.
         *
         * <p><pre>
         * [     before flex     |     flex     ][     before flex     |     flex     ]...
         * [   cannot run work   | can run work ][   cannot run work   | can run work ]...
         * \____________________________________/\____________________________________/...
         *                interval 1                            interval 2             ...(repeat)
         * </pre></p>
         *
         * @param workerClass The {@link ListenableWorker} class to run for this work
         * @param repeatInterval The repeat interval
         * @param flexInterval The duration in for which this work repeats from the end of the
         *                     {@code repeatInterval}
         */
        @RequiresApi(26)
        public Builder(
                @NonNull Class<? extends ListenableWorker> workerClass,
                @NonNull Duration repeatInterval,
                @NonNull Duration flexInterval) {
            super(workerClass);
            mWorkSpec.setPeriodic(toMillisCompat(repeatInterval),
                    toMillisCompat(flexInterval));
        }

        @Override
        @NonNull PeriodicWorkRequest buildInternal() {
            if (mBackoffCriteriaSet
                    && Build.VERSION.SDK_INT >= 23
                    && mWorkSpec.constraints.requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            if (mWorkSpec.expedited) {
                throw new IllegalArgumentException(
                        "PeriodicWorkRequests cannot be expedited");
            }
            return new PeriodicWorkRequest(this);
        }

        @Override
        @NonNull Builder getThis() {
            return this;
        }
    }
}
