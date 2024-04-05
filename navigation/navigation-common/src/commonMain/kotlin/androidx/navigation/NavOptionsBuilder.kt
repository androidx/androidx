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

@DslMarker
public annotation class NavOptionsDsl

/**
 * Construct a new [NavOptions]
 */
public fun navOptions(optionsBuilder: NavOptionsBuilder.() -> Unit): NavOptions =
    NavOptionsBuilder().apply(optionsBuilder).build()

/**
 * DSL for constructing a new [NavOptions]
 */
@NavOptionsDsl
public expect class NavOptionsBuilder() {

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     */
    public var launchSingleTop: Boolean

    /**
     * Whether this navigation action should restore any state previously saved
     * by [PopUpToBuilder.saveState] or the `popUpToSaveState` attribute. If no state was
     * previously saved with the destination ID being navigated to, this has no effect.
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var restoreState: Boolean

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until this destination is found.
     */
    public var popUpToRoute: String?
        private set

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination routes
     * from the back stack until the destination with a matching route is found.
     *
     * @param route route for the destination
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    public fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit = {})

    internal fun build(): NavOptions
}

/**
 * DSL for customizing [NavOptionsBuilder.popUpTo] operations.
 */
@NavOptionsDsl
public class PopUpToBuilder {
    /**
     * Whether the `popUpTo` destination should be popped from the back stack.
     */
    public var inclusive: Boolean = false

    /**
     * Whether the back stack and the state of all destinations between the
     * current destination and the [NavOptionsBuilder.popUpTo] ID should be saved for later
     * restoration via [NavOptionsBuilder.restoreState] or the `restoreState` attribute using
     * the same [NavOptionsBuilder.popUpTo] ID (note: this matching ID is true whether
     * [inclusive] is true or false).
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var saveState: Boolean = false
}
