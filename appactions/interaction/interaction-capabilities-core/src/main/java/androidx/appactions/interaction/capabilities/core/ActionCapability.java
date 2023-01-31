/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;

import java.util.Optional;

/**
 * <b>Do not implement this interface yourself.</b>
 *
 * <p>An ActionCapability represents some supported App Action that can be given to App Control.
 *
 * <p>Use helper classes provided by the capability library to get instances of this interface.
 */
public interface ActionCapability {

    /** Returns the unique Id of this capability declaration. */
    @NonNull
    Optional<String> getId();

    /**
     * Returns an app action proto describing how to fulfill this capability.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    AppAction getAppAction();

    /**
     * Executes the action and returns the result of execution.
     *
     * @param argumentsWrapper The arguments send from assistant to the activity.
     * @param callback The callback to receive app action result.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void execute(@NonNull ArgumentsWrapper argumentsWrapper, @NonNull CallbackInternal callback);

    /**
     * Support for manual input. This method should be invoked by AppInteraction SDKs
     * (background/foreground), so the developers have a way to report state updates back to
     * Assistant.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default void setTouchEventCallback(@NonNull TouchEventCallback callback) {}
}
