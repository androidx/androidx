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

package androidx.navigation

import kotlin.reflect.KClass

@DslMarker public annotation class NavOptionsDsl

/** Construct a new [NavOptions] */
public fun navOptions(optionsBuilder: NavOptionsBuilder.() -> Unit): NavOptions =
    NavOptionsBuilder().apply(optionsBuilder).build()

/** DSL for constructing a new [NavOptions] */
@NavOptionsDsl
public expect class NavOptionsBuilder() {

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most one
     * copy of a given destination on the top of the back stack).
     *
     * This functions similarly to how [android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP] works with
     * activities.
     */
    public var launchSingleTop: Boolean

    /**
     * Whether this navigation action should restore any state previously saved by
     * [PopUpToBuilder.saveState] or the `popUpToSaveState` attribute. If no state was previously
     * saved with the destination ID being navigated to, this has no effect.
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var restoreState: Boolean

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    public var popUpToRoute: String?
        private set

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    @get:Suppress("GetterOnBuilder")
    public var popUpToRouteClass: KClass<*>?
        private set

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    @get:Suppress("GetterOnBuilder")
    public var popUpToRouteObject: Any?
        private set

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination
     * routes from the back stack until the destination with a matching route is found.
     *
     * @param route route for the destination
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    public fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit = {})

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination
     * routes from the back stack until the destination with a matching route is found.
     *
     * @param T route from a [KClass] for the destination
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    // align with other popUpTo overloads where this is suppressed in baseline lint ignore
    @Suppress("BuilderSetStyle")
    public inline fun <reified T : Any> popUpTo(
        noinline popUpToBuilder: PopUpToBuilder.() -> Unit = {}
    )

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination
     * routes from the back stack until the destination with a matching route is found.
     *
     * @param route the [KClass] of the destination [T]
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    public fun <T : Any> popUpTo(route: KClass<T>, popUpToBuilder: PopUpToBuilder.() -> Unit)

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination
     * routes from the back stack until the destination with a matching route is found.
     *
     * @param route route from a Object for the destination
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    // align with other popUpTo overloads where this is suppressed in baseline lint ignore
    @Suppress("BuilderSetStyle", "MissingJvmstatic")
    public fun <T : Any> popUpTo(route: T, popUpToBuilder: PopUpToBuilder.() -> Unit = {})

    internal fun build(): NavOptions
}

/** DSL for customizing [NavOptionsBuilder.popUpTo] operations. */
@NavOptionsDsl
public class PopUpToBuilder {
    /** Whether the `popUpTo` destination should be popped from the back stack. */
    public var inclusive: Boolean = false

    /**
     * Whether the back stack and the state of all destinations between the current destination and
     * the [NavOptionsBuilder.popUpTo] ID should be saved for later restoration via
     * [NavOptionsBuilder.restoreState] or the `restoreState` attribute using the same
     * [NavOptionsBuilder.popUpTo] ID (note: this matching ID is true if [inclusive] is true. If
     * [inclusive] is false, this matching ID is the id of the last destination that is popped).
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var saveState: Boolean = false
}
