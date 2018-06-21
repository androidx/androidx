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

import static androidx.work.impl.Scheduler.MAX_SCHEDULER_LIMIT;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.impl.utils.IdGenerator;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for {@link WorkManager}.
 */
public final class Configuration {

    /**
     * The minimum number of system requests which can be enqueued by {@link WorkManager}
     * when using {@link android.app.job.JobScheduler} or {@link android.app.AlarmManager}.
     */
    public static final int MIN_SCHEDULER_LIMIT = 20;

    private final Executor mExecutor;
    private final int mMinJobSchedulerId;
    private final int mMaxJobSchedulerId;
    private final int mMaxSchedulerLimit;

    private Configuration(@NonNull Configuration.Builder builder) {
        if (builder.mExecutor == null) {
            mExecutor = createDefaultExecutor();
        } else {
            mExecutor = builder.mExecutor;
        }
        mMinJobSchedulerId = builder.mMinJobSchedulerId;
        mMaxJobSchedulerId = builder.mMaxJobSchedulerId;
        mMaxSchedulerLimit = builder.mMaxSchedulerLimit;
    }

    /**
     * @return The {@link Executor} used by {@link WorkManager} to execute {@link Worker}s.
     */
    public @NonNull Executor getExecutor() {
        return mExecutor;
    }

    /**
     * @return The first valid id (inclusive) used by {@link WorkManager} when
     * creating new instances of {@link android.app.job.JobInfo}s.
     *
     * If the current {@code jobId} goes beyond the bounds of the defined range of
     * ({@link Configuration.Builder#getMinJobSchedulerID()},
     *  {@link Configuration.Builder#getMaxJobSchedulerID()}), it is reset to
     *  ({@link Configuration.Builder#getMinJobSchedulerID()}).
     */
    public int getMinJobSchedulerID() {
        return mMinJobSchedulerId;
    }

    /**
     * @return The last valid id (inclusive) used by {@link WorkManager} when
     * creating new instances of {@link android.app.job.JobInfo}s.
     *
     * If the current {@code jobId} goes beyond the bounds of the defined range of
     * ({@link Configuration.Builder#getMinJobSchedulerID()},
     *  {@link Configuration.Builder#getMaxJobSchedulerID()}), it is reset to
     *  ({@link Configuration.Builder#getMinJobSchedulerID()}).
     */
    public int getMaxJobSchedulerID() {
        return mMaxJobSchedulerId;
    }

    /**
     * @return The maximum number of system requests which can be enqueued by {@link WorkManager}
     * when using {@link android.app.job.JobScheduler} or {@link android.app.AlarmManager}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getMaxSchedulerLimit() {
        // We double schedule jobs in SDK 23. So use half the number of max slots specified.
        if (Build.VERSION.SDK_INT == 23) {
            return mMaxSchedulerLimit / 2;
        } else {
            return mMaxSchedulerLimit;
        }
    }

    private @NonNull Executor createDefaultExecutor() {
        return Executors.newFixedThreadPool(
                // This value is the same as the core pool size for AsyncTask#THREAD_POOL_EXECUTOR.
                Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4)));
    }

    /**
     * A Builder for {@link Configuration}.
     */
    public static final class Builder {

        int mMinJobSchedulerId = IdGenerator.INITIAL_ID;
        int mMaxJobSchedulerId = Integer.MAX_VALUE;
        int mMaxSchedulerLimit = MIN_SCHEDULER_LIMIT;
        Executor mExecutor;

        /**
         * Specifies a custom {@link Executor} for WorkManager.
         *
         * @param executor An {@link Executor} for processing work
         * @return This {@link Builder} instance
         */
        public @NonNull Builder setExecutor(@NonNull Executor executor) {
            mExecutor = executor;
            return this;
        }

        /**
         * Specifies the range of {@link android.app.job.JobInfo} IDs that can be used by
         * {@link WorkManager}. {@link WorkManager} needs a range of at least {@code 1000} IDs.
         *
         * @param minJobSchedulerId The first valid {@link android.app.job.JobInfo} ID inclusive.
         * @param maxJobSchedulerId The last valid {@link android.app.job.JobInfo} ID inclusive.
         * @return This {@link Builder} instance
         * @throws IllegalArgumentException when the size of the range is < 1000
         */
        public @NonNull Builder setJobSchedulerJobIdRange(
                int minJobSchedulerId,
                int maxJobSchedulerId) {
            if ((maxJobSchedulerId - minJobSchedulerId) < 1000) {
                throw new IllegalArgumentException(
                        "WorkManager needs a range of at least 1000 job ids.");
            }

            mMinJobSchedulerId = minJobSchedulerId;
            mMaxJobSchedulerId = maxJobSchedulerId;
            return this;
        }

        /**
         * Specifies the maximum number of system requests made by {@link WorkManager}
         * when using {@link android.app.job.JobScheduler} or {@link android.app.AlarmManager}.
         * When the application exceeds this limit {@link WorkManager} maintains an internal queue
         * of {@link WorkRequest}s, and enqueues when slots become free.
         *
         * {@link WorkManager} requires a minimum of {@link Configuration#MIN_SCHEDULER_LIMIT}
         * slots. The total number of slots also cannot exceed {@code 100} which is
         * the {@link android.app.job.JobScheduler} limit.
         *
         * @param maxSchedulerLimit The total number of jobs which can be enqueued by
         *                                {@link WorkManager} when using
         *                                {@link android.app.job.JobScheduler}.
         * @return This {@link Builder} instance
         * @throws IllegalArgumentException when the number of jobs <
         *                                  {@link Configuration#MIN_SCHEDULER_LIMIT}
         */
        public @NonNull Builder setMaxSchedulerLimit(int maxSchedulerLimit) {
            if (maxSchedulerLimit < MIN_SCHEDULER_LIMIT) {
                throw new IllegalArgumentException(
                        "WorkManager needs to be able to schedule at least 20 jobs in "
                                + "JobScheduler.");
            }
            mMaxSchedulerLimit = Math.min(maxSchedulerLimit, MAX_SCHEDULER_LIMIT);
            return this;
        }

        /**
         * Specifies a custom {@link Executor} for WorkManager.
         *
         * @param executor An {@link Executor} for processing work
         * @return This {@link Builder} instance
         * @deprecated Use the {@link Configuration.Builder#setExecutor(Executor)} method instead
         */
        @Deprecated
        public @NonNull Builder withExecutor(@NonNull Executor executor) {
            mExecutor = executor;
            return this;
        }

        /**
         * Builds a {@link Configuration} object.
         *
         * @return A {@link Configuration} object with this {@link Builder}'s parameters.
         */
        public @NonNull Configuration build() {
            return new Configuration(this);
        }
    }
}
