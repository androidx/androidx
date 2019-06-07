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

package androidx.navigation;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

/**
 * Navigation actions provide a level of indirection between your navigation code and the
 * underlying destinations. This allows you to define common actions that change their destination
 * or {@link NavOptions} based on the current {@link NavDestination}.
 *
 * <p>The {@link NavOptions} associated with a NavAction are used by default when navigating
 * to this action via {@link NavController#navigate(int)} or
 * {@link NavController#navigate(int, Bundle)}.</p>
 *
 * <p>Actions should be added via {@link NavDestination#putAction(int, int)} or
 * {@link NavDestination#putAction(int, NavAction)}.</p>
 */
public final class NavAction {
    @IdRes
    private final int mDestinationId;
    private NavOptions mNavOptions;
    private Bundle mDefaultArguments;

    /**
     * Creates a new NavAction for the given destination.
     *
     * @param destinationId the ID of the destination that should be navigated to when this
     *                      action is used.
     */
    public NavAction(@IdRes int destinationId) {
        this(destinationId, null);
    }

    /**
     * Creates a new NavAction for the given destination.
     *
     * @param destinationId the ID of the destination that should be navigated to when this
     *                      action is used.
     * @param navOptions special options for this action that should be used by default
     */
    public NavAction(@IdRes int destinationId, @Nullable NavOptions navOptions) {
        this(destinationId, navOptions, null);
    }

    /**
     * Creates a new NavAction for the given destination.
     *
     * @param destinationId the ID of the destination that should be navigated to when this
     *                      action is used.
     * @param navOptions special options for this action that should be used by default
     * @param defaultArgs argument bundle to be used by default
     */
    public NavAction(@IdRes int destinationId,
            @Nullable NavOptions navOptions,
            @Nullable Bundle defaultArgs) {
        mDestinationId = destinationId;
        mNavOptions = navOptions;
        mDefaultArguments = defaultArgs;
    }

    /**
     * Gets the ID of the destination that should be navigated to when this action is used
     */
    public int getDestinationId() {
        return mDestinationId;
    }

    /**
     * Sets the NavOptions to be used by default when navigating to this action.
     *
     * @param navOptions special options for this action that should be used by default
     */
    public void setNavOptions(@Nullable NavOptions navOptions) {
        mNavOptions = navOptions;
    }

    /**
     * Gets the NavOptions to be used by default when navigating to this action.
     */
    @Nullable
    public NavOptions getNavOptions() {
        return mNavOptions;
    }

    /**
     * Gets the argument bundle to be used by default when navigating to this action.
     * @return bundle of default argument values
     */
    @Nullable
    public Bundle getDefaultArguments() {
        return mDefaultArguments;
    }

    /**
     * Sets the argument bundle to be used by default when navigating to this action.
     *
     * @param defaultArgs argument bundle that should be used by default
     */
    public void setDefaultArguments(@Nullable Bundle defaultArgs) {
        mDefaultArguments = defaultArgs;
    }
}
