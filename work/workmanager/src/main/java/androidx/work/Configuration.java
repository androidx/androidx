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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for {@link WorkManager}.
 */
public final class Configuration {

    private final Executor mExecutor;

    private Configuration(@NonNull Configuration.Builder builder) {
        if (builder.mExecutor == null) {
            mExecutor = createDefaultExecutor();
        } else {
            mExecutor = builder.mExecutor;
        }
    }

    public @NonNull Executor getExecutor() {
        return mExecutor;
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
