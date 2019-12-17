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
import androidx.camera.core.impl.Config;

/**
 * Configuration containing options pertaining to EventCallback object.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface UseCaseEventConfig {

    /**
     * Option: camerax.core.useCaseEventCallback
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Config.Option<UseCase.EventCallback> OPTION_USE_CASE_EVENT_CALLBACK =
            Config.Option.create("camerax.core.useCaseEventCallback", UseCase.EventCallback.class);

    /**
     * Returns the EventCallback.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    UseCase.EventCallback getUseCaseEventCallback(@Nullable UseCase.EventCallback valueIfMissing);

    /**
     * Returns the EventCallback.
     *
     * @return The stored value, if it exists in this configuration.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    UseCase.EventCallback getUseCaseEventCallback();

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Builder for a {@link UseCaseEventConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Builder<B> {

        /**
         * Sets the EventCallback.
         *
         * @param eventCallback The EventCallback.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        B setUseCaseEventCallback(@NonNull UseCase.EventCallback eventCallback);
    }
}
