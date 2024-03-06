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
package androidx.navigation

import kotlin.jvm.JvmOverloads

/**
 * NavOptions stores special options for navigate actions
 */
public expect class NavOptions {
    /**
     * Route for the destination to pop up to before navigating. When set, all non-matching
     * destinations should be popped from the back stack.
     * @return the destination route to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    public var popUpToRoute: String?
        private set

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     */
    public fun shouldLaunchSingleTop(): Boolean

    /**
     * Whether this navigation action should restore any state previously saved
     * by [Builder.setPopUpTo] or the `popUpToSaveState` attribute.
     */
    public fun shouldRestoreState(): Boolean

    /**
     * Whether the destination set in [popUpToRoute] should be popped from the back stack.
     * @see Builder.setPopUpTo
     *
     * @see NavOptions.popUpToRoute
     */
    public fun isPopUpToInclusive(): Boolean

    /**
     * Whether the back stack and the state of all destinations between the
     * current destination and [popUpToRoute] should be saved for later restoration via
     * [Builder.setRestoreState] or the `restoreState` attribute using the same route
     * as [popUpToRoute] (note: this matching ID is true whether [isPopUpToInclusive] is true or
     * false).
     */
    public fun shouldPopUpToSaveState(): Boolean

    /**
     * Builder for constructing new instances of NavOptions.
     */
    public class Builder() {
        /**
         * Launch a navigation target as single-top if you are making a lateral navigation
         * between instances of the same target (e.g. detail pages about similar data items)
         * that should not preserve history.
         *
         * @param singleTop true to launch as single-top
         */
        public fun setLaunchSingleTop(singleTop: Boolean): Builder

        /**
         * Whether this navigation action should restore any state previously saved
         * by [setPopUpTo] or the `popUpToSaveState` attribute. If no state was
         * previously saved with the destination ID being navigated to, this has no effect.
         */
        public fun setRestoreState(restoreState: Boolean): Builder

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param route route for destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         * current destination and [route] should be saved for later restoration via
         * [setRestoreState] or the `restoreState` attribute using the same route
         * as [popUpToRoute] (note: this matching ID is true whether [inclusive] is true or
         * false).
         * @return this Builder
         *
         * @see NavOptions.popUpToRoute
         * @see NavOptions.isPopUpToInclusive
         */
        @JvmOverloads
        public fun setPopUpTo(
            route: String?,
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder

        /**
         * @return a constructed NavOptions
         */
        public fun build(): NavOptions
    }
}
