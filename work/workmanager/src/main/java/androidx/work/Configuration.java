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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for {@link WorkManager}.
 */
public class Configuration {

    private final ExecutorService mExecutorService;

    private Configuration(@NonNull Configuration.Builder builder) {
        if (builder.mExecutorService == null) {
            mExecutorService = createDefaultExecutorService();
        } else {
            mExecutorService = builder.mExecutorService;
        }
    }

    public @NonNull ExecutorService getExecutorService() {
        return mExecutorService;
    }

    private ExecutorService createDefaultExecutorService() {
        return Executors.newFixedThreadPool(
                // This value is the same as the core pool size for AsyncTask#THREAD_POOL_EXECUTOR.
                Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4)));
    }

    /**
     * A Builder for {@link Configuration}.
     */
    public static class Builder {

        ExecutorService mExecutorService;

        /**
         * Specifies a custom {@link ExecutorService} for WorkManager.
         *
         * @param executorService An {@link ExecutorService} for processing work.  Exercise care
         *                        when using this API, as it may lead to performance problems.
         * @return This {@link Builder} instance
         */
        public Builder withExecutorService(@NonNull ExecutorService executorService) {
            mExecutorService = executorService;
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
