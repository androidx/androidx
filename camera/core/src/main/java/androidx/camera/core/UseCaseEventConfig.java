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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** Configuration containing options pertaining to EventListener object. */
public interface UseCaseEventConfig {

    /**
     * Option: camerax.core.useCaseEventListener
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Config.Option<UseCase.EventListener> OPTION_USE_CASE_EVENT_LISTENER =
            Config.Option.create("camerax.core.useCaseEventListener", UseCase.EventListener.class);

    /**
     * Returns the EventListener.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    UseCase.EventListener getUseCaseEventListener(@Nullable UseCase.EventListener valueIfMissing);

    /**
     * Returns the EventListener.
     *
     * @return The stored value, if it exists in this configuration.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    UseCase.EventListener getUseCaseEventListener();

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Builder for a {@link UseCaseEventConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     */
    interface Builder<B> {

        /**
         * Sets the EventListener.
         *
         * @param eventListener The EventListener.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        B setUseCaseEventListener(UseCase.EventListener eventListener);
    }
}
