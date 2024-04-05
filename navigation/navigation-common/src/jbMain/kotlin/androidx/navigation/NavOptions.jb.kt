/*
 * Copyright 2024 The Android Open Source Project
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

public actual class NavOptions internal constructor(
    private val singleTop: Boolean,
    private val restoreState: Boolean,
    popUpToRoute: String?,
    private val popUpToInclusive: Boolean,
    private val popUpToSaveState: Boolean,
) {
    /**
     * Route for the destination to pop up to before navigating. When set, all non-matching
     * destinations should be popped from the back stack.
     * @return the destination route to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    public actual var popUpToRoute: String? = popUpToRoute
        private set

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     */
    public actual fun shouldLaunchSingleTop(): Boolean {
        return singleTop
    }

    /**
     * Whether this navigation action should restore any state previously saved
     * by [Builder.setPopUpTo] or the `popUpToSaveState` attribute.
     */
    public actual fun shouldRestoreState(): Boolean {
        return restoreState
    }

    /**
     * Whether the destination set in [popUpToRoute] should be popped from the back stack.
     * @see Builder.setPopUpTo
     */
    public actual fun isPopUpToInclusive(): Boolean {
        return popUpToInclusive
    }

    /**
     * Whether the back stack and the state of all destinations between the
     * current destination and [popUpToRoute] should be saved for later restoration via
     * [Builder.setRestoreState] or the `restoreState` attribute using the same ID
     * as [popUpToRoute] (note: this matching ID is true whether [isPopUpToInclusive] is true or
     * false).
     */
    public actual fun shouldPopUpToSaveState(): Boolean {
        return popUpToSaveState
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavOptions) return false
        return singleTop == other.singleTop &&
            restoreState == other.restoreState &&
            popUpToRoute == other.popUpToRoute &&
            popUpToInclusive == other.popUpToInclusive &&
            popUpToSaveState == other.popUpToSaveState
    }

    override fun hashCode(): Int {
        var result = if (shouldLaunchSingleTop()) 1 else 0
        result = 31 * result + if (shouldRestoreState()) 1 else 0
        result = 31 * result + popUpToRoute.hashCode()
        result = 31 * result + if (isPopUpToInclusive()) 1 else 0
        result = 31 * result + if (shouldPopUpToSaveState()) 1 else 0
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(this::class.simpleName)
        sb.append("(")
        if (singleTop) {
            sb.append("launchSingleTop ")
        }
        if (restoreState) {
            sb.append("restoreState ")
        }
        if (popUpToRoute != null)
            if (popUpToRoute != null) {
                sb.append("popUpTo(")
                sb.append(popUpToRoute)
                if (popUpToInclusive) {
                    sb.append(" inclusive")
                }
                if (popUpToSaveState) {
                    sb.append(" saveState")
                }
                sb.append(")")
            }
        return sb.toString()
    }

    /**
     * Builder for constructing new instances of NavOptions.
     */
    public actual class Builder {
        private var singleTop = false
        private var restoreState = false
        private var popUpToRoute: String? = null
        private var popUpToInclusive = false
        private var popUpToSaveState = false

        /**
         * Launch a navigation target as single-top if you are making a lateral navigation
         * between instances of the same target (e.g. detail pages about similar data items)
         * that should not preserve history.
         *
         * @param singleTop true to launch as single-top
         */
        public actual fun setLaunchSingleTop(singleTop: Boolean): Builder {
            this.singleTop = singleTop
            return this
        }

        /**
         * Whether this navigation action should restore any state previously saved
         * by [setPopUpTo] or the `popUpToSaveState` attribute. If no state was
         * previously saved with the destination ID being navigated to, this has no effect.
         */
        public actual fun setRestoreState(restoreState: Boolean): Builder {
            this.restoreState = restoreState
            return this
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param route route for destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         * current destination and [route] should be saved for later restoration via
         * [setRestoreState] or the `restoreState` attribute using the same ID
         * as [popUpToRoute] (note: this matching ID is true whether [inclusive] is true or
         * false).
         * @return this Builder
         *
         * @see NavOptions.isPopUpToInclusive
         */
        @JvmOverloads
        public actual fun setPopUpTo(
            route: String?,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRoute = route
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        /**
         * @return a constructed NavOptions
         */
        public actual fun build(): NavOptions {
            return NavOptions(
                singleTop, restoreState,
                popUpToRoute, popUpToInclusive, popUpToSaveState
            )
        }
    }
}
