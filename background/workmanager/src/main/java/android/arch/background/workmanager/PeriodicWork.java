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

import android.arch.background.workmanager.model.WorkSpec;
import android.util.Log;

/**
 * A class to create a logical unit of repeating work.
 */

public class PeriodicWork extends BaseWork {

    /**
     * The minimum interval duration for periodic {@link Work}, in milliseconds.
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#110}.
     */
    public static final long MIN_PERIODIC_INTERVAL_DURATION = 15 * 60 * 1000L; // 15 minutes.

    /**
     * The minimum flex duration for periodic {@link Work}, in milliseconds.
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#113}.
     */
    public static final long MIN_PERIODIC_FLEX_DURATION = 5 * 60 * 1000L; // 5 minutes.

    private static final String TAG = "PeriodicWork";

    PeriodicWork(WorkSpec workSpec) {
        super(workSpec);
    }

    /**
     * Builder for {@link PeriodicWork} class.
     */
    public static class Builder extends BaseWork.Builder<PeriodicWork, PeriodicWork.Builder> {

        /**
         * Creates a {@link PeriodicWork} to run periodically once every interval period. The
         * {@link PeriodicWork} is guaranteed to run exactly one time during this interval. The
         * {@code intervalDuration} must be greater than or equal to
         * {@link PeriodicWork#MIN_PERIODIC_INTERVAL_DURATION}. It may run
         * immediately, at the end of the period, or any time in between so long as the other
         * conditions are satisfied at the time. The run time of the {@link PeriodicWork} can be
         * restricted to a flex period within an interval.
         *
         * @param workerClass      The {@link Worker} class to run with this job.
         * @param intervalDuration Duration in milliseconds for which {@link Work} will repeat.
         */
        public Builder(Class<? extends Worker> workerClass, long intervalDuration) {
            super(workerClass);
            if (intervalDuration < MIN_PERIODIC_INTERVAL_DURATION) {
                Log.w(TAG, "Interval duration lesser than minimum allowed value; "
                        + "Changed to " + MIN_PERIODIC_INTERVAL_DURATION);
                intervalDuration = MIN_PERIODIC_INTERVAL_DURATION;
            }
            mWorkSpec.setPeriodic(intervalDuration, intervalDuration);
        }

        /**
         * Creates a {@link PeriodicWork} to run periodically once within the
         * <strong>flex period</strong> of every interval period. See diagram below. The flex period
         * begins at {@code intervalDuration - flexDuration} to the end of the interval.
         * {@code intervalDuration} must be greater than or equal to
         * {@link PeriodicWork#MIN_PERIODIC_INTERVAL_DURATION} and {@code flexDuration} must be
         * greater than or equal to {@link PeriodicWork#MIN_PERIODIC_FLEX_DURATION}.
         *
         * <p><pre>
         * [     before flex     |     flex     ][     before flex     |     flex     ]...
         * [   cannot run work   | can run work ][   cannot run work   | can run work ]...
         * \____________________________________/\____________________________________/...
         *                interval 1                            interval 2             ...(repeat)
         * </pre></p>
         *
         * @param workerClass      The {@link Worker} class to run with this job.
         * @param intervalDuration Duration in milliseconds of the interval.
         * @param flexDuration     Duration in millisecond for which {@link Work} will repeat from
         *                         the end of the interval.
         */
        public Builder(
                Class<? extends Worker> workerClass,
                long intervalDuration,
                long flexDuration) {
            super(workerClass);
            if (intervalDuration < MIN_PERIODIC_INTERVAL_DURATION) {
                Log.w(TAG, "Interval duration lesser than minimum allowed value; "
                        + "Changed to " + MIN_PERIODIC_INTERVAL_DURATION);
                intervalDuration = MIN_PERIODIC_INTERVAL_DURATION;
            }
            if (flexDuration < MIN_PERIODIC_FLEX_DURATION) {
                Log.w(TAG, "Flex duration lesser than minimum allowed value; "
                        + "Changed to " + MIN_PERIODIC_FLEX_DURATION);
                flexDuration = MIN_PERIODIC_FLEX_DURATION;
            }
            if (flexDuration > intervalDuration) {
                Log.w(TAG, "Flex duration greater than interval duration; "
                        + "Changed to " + intervalDuration);
                flexDuration = intervalDuration;
            }
            mWorkSpec.setPeriodic(intervalDuration, flexDuration);
        }

        @Override
        Builder getThis() {
            return this;
        }

        @Override
        public PeriodicWork build() {
            return new PeriodicWork(mWorkSpec);
        }
    }
}
