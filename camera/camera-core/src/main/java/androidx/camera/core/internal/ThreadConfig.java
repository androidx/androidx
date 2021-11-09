/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ReadableConfig;

import java.util.concurrent.Executor;

/**
 * Configuration containing options pertaining to threads used by the configured object.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ThreadConfig extends ReadableConfig {

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.thread.backgroundExecutor
     */
    Option<Executor> OPTION_BACKGROUND_EXECUTOR =
            Option.create("camerax.core.thread.backgroundExecutor", Executor.class);

    // *********************************************************************************************

    /**
     * Returns the executor that will be used for background tasks.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default Executor getBackgroundExecutor(@Nullable Executor valueIfMissing) {
        return retrieveOption(OPTION_BACKGROUND_EXECUTOR, valueIfMissing);
    }


    /**
     * Returns the executor that will be used for background tasks.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default Executor getBackgroundExecutor() {
        return retrieveOption(OPTION_BACKGROUND_EXECUTOR);
    }

    /**
     * Builder for a {@link ThreadConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     */
    interface Builder<B> {

        /**
         * Sets the default executor that will be used for background tasks.
         *
         * @param executor The executor which will be used for background tasks.
         * @return the current Builder.
         */
        @NonNull
        B setBackgroundExecutor(@NonNull Executor executor);
    }
}
