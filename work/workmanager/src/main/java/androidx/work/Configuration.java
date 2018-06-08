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

import androidx.work.impl.utils.IdGenerator;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for {@link WorkManager}.
 */
public final class Configuration {

    private final Executor mExecutor;
    private final int mMinJobSchedulerId;
    private final int mMaxJobSchedulerId;

    private Configuration(@NonNull Configuration.Builder builder) {
        if (builder.mExecutor == null) {
            mExecutor = createDefaultExecutor();
        } else {
            mExecutor = builder.mExecutor;
        }
        mMinJobSchedulerId = builder.mMinJobSchedulerId;
        mMaxJobSchedulerId = builder.mMaxJobSchedulerId;
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

    private Executor createDefaultExecutor() {
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
        Executor mExecutor;

        /**
         * Specifies a custom {@link Executor} for WorkManager.
         *
         * @param executor An {@link Executor} for processing work
         * @return This {@link Builder} instance
         */
        public Builder setExecutor(@NonNull Executor executor) {
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
        public Builder setJobSchedulerJobIdRange(int minJobSchedulerId, int maxJobSchedulerId) {
            if ((maxJobSchedulerId - minJobSchedulerId) < 1000) {
                throw new IllegalArgumentException(
                        "WorkManager needs a range of at least 1000 job ids.");
            }

            mMinJobSchedulerId = minJobSchedulerId;
            mMaxJobSchedulerId = maxJobSchedulerId;
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
        public Builder withExecutor(@NonNull Executor executor) {
            mExecutor = executor;
            return this;
        }

        /**
         * Builds a {@link Configuration} object.
         *
         * @return A {@link Configuration} object with this {@link Builder}'s parameters.
         */
        public Configuration build() {
            return new Configuration(this);
        }
    }
}
