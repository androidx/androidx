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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.annotation.Retention;
import java.util.UUID;

/**
 * A class to create a logical unit of work.
 */

public class Work {

    @Retention(SOURCE)
    @IntDef({STATUS_ENQUEUED, STATUS_RUNNING, STATUS_SUCCEEDED, STATUS_FAILED, STATUS_BLOCKED})
    public @interface WorkStatus {
    }

    @Retention(SOURCE)
    @IntDef({BACKOFF_POLICY_EXPONENTIAL, BACKOFF_POLICY_LINEAR})
    public @interface BackoffPolicy {
    }

    public static final int STATUS_ENQUEUED = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_SUCCEEDED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_BLOCKED = 4;

    public static final int BACKOFF_POLICY_EXPONENTIAL = 0;
    public static final int BACKOFF_POLICY_LINEAR = 1;
    public static final long DEFAULT_BACKOFF_DELAY_DURATION = 30000L;

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#82}
     */
    public static final long MAX_BACKOFF_DURATION = 5 * 60 * 60 * 1000; // 5 hours.

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

    private static final String TAG = "Work";

    private WorkSpec mWorkSpec;

    private Work(WorkSpec workSpec) {
        mWorkSpec = workSpec;
    }

    /**
     * @return The id for this set of work.
     */
    public String getId() {
        return mWorkSpec.getId();
    }

    WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    /**
     * Builder for {@link Work} class.
     */
    public static class Builder {
        private WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());
        private boolean mBackoffCriteriaSet = false;

        public Builder(Class<? extends Worker> workerClass) {
            mWorkSpec.setWorkerClassName(workerClass.getName());
        }

        @VisibleForTesting
        Builder withInitialStatus(@WorkStatus int status) {
            mWorkSpec.setStatus(status);
            return this;
        }

        @VisibleForTesting
        Builder withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.setRunAttemptCount(runAttemptCount);
            return this;
        }

        /**
         * Add constraints to the {@link Work}.
         *
         * @param constraints The constraints for the {@link Work}
         * @return The current {@link Builder}.
         */
        public Builder withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.setConstraints(constraints);
            return this;
        }

        /**
         * Change backoff policy and delay for the {@link Work}.
         * Default is {@value Work#BACKOFF_POLICY_EXPONENTIAL} and 30 seconds.
         * Maximum backoff delay duration is {@value #MAX_BACKOFF_DURATION}.
         *
         * @param backoffPolicy        Backoff Policy to use for {@link Work}
         * @param backoffDelayDuration Time to wait before restarting {@link Worker}
         *                             (in milliseconds)
         * @return The current {@link Builder}.
         */
        public Builder withBackoffCriteria(@BackoffPolicy int backoffPolicy,
                                           long backoffDelayDuration) {
            // TODO(xbhatnag): Enforce minimum backoff delay to 10 seconds
            mBackoffCriteriaSet = true;
            if (backoffDelayDuration > MAX_BACKOFF_DURATION) {
                Log.w(TAG, "Backoff delay duration exceeds maximum value");
                backoffDelayDuration = MAX_BACKOFF_DURATION;
            }
            mWorkSpec.setBackoffPolicy(backoffPolicy);
            mWorkSpec.setBackoffDelayDuration(backoffDelayDuration);
            return this;
        }

        /**
         * Add arguments to the {@link Work}.
         *
         * @param arguments key/value pairs that will be provided to the {@link Worker} class
         * @return The current {@link Builder}.
         */
        public Builder withArguments(Arguments arguments) {
            mWorkSpec.setArguments(arguments);
            return this;
        }

        /**
         * Add an optional tag to the {@link Work}.  This is particularly useful for modules or
         * libraries who want to query for or cancel all of their own work.
         *
         * @param tag A tag for identifying the {@link Work} in queries.
         * @return The current {@link Builder}.
         */
        public Builder withTag(String tag) {
            mWorkSpec.setTag(tag);
            return this;
        }

        /**
         * Specify whether {@link Work} should run with an initial delay. Default is 0ms.
         *
         * <p><strong>Note:</strong> An error will result when setting this function on a
         * {@link Builder} with {@link Builder#setPeriodic(long)} or
         * {@link Builder#setPeriodic(long, long)}.</p>
         *
         * @param duration initial delay before running WorkSpec (in milliseconds)
         * @return The current {@link Builder}.
         */
        public Builder withInitialDelay(long duration) {
            mWorkSpec.setInitialDelay(duration);
            return this;
        }

        /**
         * Sets the {@link Work} to run periodically once every interval period. The {@link Work}
         * is guaranteed to run exactly one time during this interval. The {@code intervalDuration}
         * must be greater than or equal to {@link Work#MIN_PERIODIC_INTERVAL_DURATION}. It may run
         * immediately, at the end of the period, or any time in between so long as the other
         * conditions are satisfied at the time. The run time of the {@link Work} can be restricted
         * to a flex period within an interval, see {@link Builder#setPeriodic(long, long)}.
         *
         * <p><strong>Note:</strong> An error will result when setting this function on a
         * {@link Builder} with {@link Builder#withInitialDelay(long)}.</p>
         *
         * @param intervalDuration Duration in milliseconds for which {@link Work} will repeat.
         * @return The current {@link Builder}.
         */
        public Builder setPeriodic(long intervalDuration) {
            mWorkSpec.setPeriodic(intervalDuration);
            return this;
        }

        /**
         * Sets the {@link Work} to run periodically once within the <strong>flex period</strong> of
         * every interval period. See diagram below. The flex period begins at
         * {@code intervalDuration - flexDuration} to the end of the interval.
         * {@code intervalDuration} must be greater than or equal to
         * {@link Work#MIN_PERIODIC_INTERVAL_DURATION} and {@code flexDuration} must be greater
         * than or equal to {@link Work#MIN_PERIODIC_FLEX_DURATION}.
         *
         * <p><strong>Note:</strong> An error will result when setting this function on a
         * {@link Builder} with {@link Builder#withInitialDelay(long)}.</p>
         *
         * <p><pre>
         * [     before flex     |     flex     ][     before flex     |     flex     ]...
         * [   cannot run work   | can run work ][   cannot run work   | can run work ]...
         * \____________________________________/\____________________________________/...
         *                interval 1                            interval 2             ...(repeat)
         * </pre></p>
         *
         * @param intervalDuration Duration in milliseconds of the interval.
         * @param flexDuration     Duration in millisecond for which {@link Work} will repeat from
         *                         the end of the interval.
         * @return The current {@link Builder}.
         */
        public Builder setPeriodic(long intervalDuration, long flexDuration) {
            mWorkSpec.setPeriodic(intervalDuration, flexDuration);
            return this;
        }

        /**
         * Generates the {@link Work} from {@link Builder}.
         *
         * @return new {@link Work}
         */
        public Work build() {
            if (mWorkSpec.isPeriodic()) {
                // TODO(janclarin): Consider throwing exceptions for odd cases like interval < flex,
                // if interval < MIN_INTERVAL_DURATION, or flex < MIN_FLEX_DURATION.
                // Currently, JobInfo.Builder silently changes these values to make them function.
                // This might throw exceptions that affect users at runtime.
                if (mWorkSpec.hasInitialDelay()) {
                    throw new IllegalArgumentException("Cannot set initial delay on periodic work");
                }
            }

            if (mBackoffCriteriaSet && mWorkSpec.getConstraints().requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            return new Work(mWorkSpec);
        }
    }
}
