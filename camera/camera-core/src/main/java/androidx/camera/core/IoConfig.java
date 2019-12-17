/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.Config.Option;

import java.util.concurrent.Executor;

/**
 * Configuration containing IO related options.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
interface IoConfig {

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.io.ioExecutor
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Executor> OPTION_IO_EXECUTOR =
            Option.create("camerax.core.io.ioExecutor", Executor.class);

    // *********************************************************************************************

    /**
     * Returns the executor that will be used for IO tasks.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    Executor getIoExecutor(@Nullable Executor valueIfMissing);


    /**
     * Returns the executor that will be used for IO tasks.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    Executor getIoExecutor();

    /**
     * Builder for a {@link IoConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     * @hide
     */

    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder<B> {

        /**
         * Sets the default executor that will be used for IO tasks.
         *
         * @param executor The executor which will be used for IO tasks.
         * @return the current Builder.
         */
        @NonNull
        B setIoExecutor(@NonNull Executor executor);
    }
}
