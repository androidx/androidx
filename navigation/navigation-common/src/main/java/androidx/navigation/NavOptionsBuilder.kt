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

import androidx.annotation.AnimRes
import androidx.annotation.AnimatorRes
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

@DslMarker public annotation class NavOptionsDsl

/** Construct a new [NavOptions] */
public fun navOptions(optionsBuilder: NavOptionsBuilder.() -> Unit): NavOptions =
    NavOptionsBuilder().apply(optionsBuilder).build()

/** DSL for constructing a new [NavOptions] */
@NavOptionsDsl
public class NavOptionsBuilder {
    private val builder = NavOptions.Builder()

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most one
     * copy of a given destination on the top of the back stack).
     *
     * This functions similarly to how [android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP] works with
     * activities.
     */
    public var launchSingleTop: Boolean = false

    /**
     * Whether this navigation action should restore any state previously saved by
     * [PopUpToBuilder.saveState] or the `popUpToSaveState` attribute. If no state was previously
     * saved with the destination ID being navigated to, this has no effect.
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var restoreState: Boolean = false

    /** Returns the current destination that the builder will pop up to. */
    @IdRes
    public var popUpToId: Int = -1
        internal set(value) {
            field = value
            inclusive = false
        }

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    @Deprecated("Use the popUpToId property.")
    public var popUpTo: Int
        get() = popUpToId
        @Deprecated("Use the popUpTo function and passing in the id.")
        set(value) {
            popUpTo(value)
        }

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    public var popUpToRoute: String? = null
        private set(value) {
            if (value != null) {
                require(value.isNotBlank()) { "Cannot pop up to an empty route" }
                field = value
                inclusive = false
            }
        }

    private var inclusive = false
    private var saveState = false

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    @get:Suppress("GetterOnBuilder")
    public var popUpToRouteClass: KClass<*>? = null
        private set(value) {
            if (value != null) {
                field = value
                inclusive = false
            }
        }

    /**
     * The destination to pop up to before navigating. All non-matching destinations from the back
     * stack up until this destination will also be popped.
     */
    @get:Suppress("GetterOnBuilder")
    public var popUpToRouteObject: Any? = null
        private set(value) {
            if (value != null) {
                field = value
                inclusive = false
            }
        }

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations from
     * the back stack until this destination is found.
     */
    public fun popUpTo(@IdRes id: Int, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {
        popUpToId = id
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination
     * routes from the back stack until the destination with a matching route is found.
     *
     * @param route route for the destination
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    public fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {
        popUpToRoute = route
        popUpToId = -1
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

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
    ) {
        popUpTo(T::class, popUpToBuilder)
    }

    // this restricted public is needed so that the public reified [popUpTo] can call
    // private popUpToRouteClass setter
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : Any> popUpTo(klass: KClass<T>, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        popUpToRouteClass = klass
        popUpToId = -1
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination
     * routes from the back stack until the destination with a matching route is found.
     *
     * @param route route from a Object for the destination
     * @param popUpToBuilder builder used to construct a popUpTo operation
     */
    // align with other popUpTo overloads where this is suppressed in baseline lint ignore
    @Suppress("BuilderSetStyle", "MissingJvmstatic")
    public fun <T : Any> popUpTo(route: T, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {
        popUpToRouteObject = route
        popUpToId = -1
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    /**
     * Sets any custom Animation or Animator resources that should be used.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    public fun anim(animBuilder: AnimBuilder.() -> Unit) {
        AnimBuilder().apply(animBuilder).run {
            this@NavOptionsBuilder.builder
                .setEnterAnim(enter)
                .setExitAnim(exit)
                .setPopEnterAnim(popEnter)
                .setPopExitAnim(popExit)
        }
    }

    internal fun build() =
        builder
            .apply {
                setLaunchSingleTop(launchSingleTop)
                setRestoreState(restoreState)
                if (popUpToRoute != null) {
                    setPopUpTo(popUpToRoute, inclusive, saveState)
                } else if (popUpToRouteClass != null) {
                    setPopUpTo(popUpToRouteClass!!, inclusive, saveState)
                } else if (popUpToRouteObject != null) {
                    setPopUpTo(popUpToRouteObject!!, inclusive, saveState)
                } else {
                    setPopUpTo(popUpToId, inclusive, saveState)
                }
            }
            .build()
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
     * [NavOptionsBuilder.popUpTo] ID (note: this matching ID is true whether [inclusive] is true or
     * false).
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var saveState: Boolean = false
}

/** DSL for setting custom Animation or Animator resources on a [NavOptionsBuilder] */
@NavOptionsDsl
public class AnimBuilder {
    /**
     * The custom Animation or Animator resource for the enter animation.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes @AnimatorRes public var enter: Int = -1

    /**
     * The custom Animation or Animator resource for the exit animation.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes @AnimatorRes public var exit: Int = -1

    /**
     * The custom Animation or Animator resource for the enter animation when popping off the back
     * stack.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes @AnimatorRes public var popEnter: Int = -1

    /**
     * The custom Animation or Animator resource for the exit animation when popping off the back
     * stack.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes @AnimatorRes public var popExit: Int = -1
}
