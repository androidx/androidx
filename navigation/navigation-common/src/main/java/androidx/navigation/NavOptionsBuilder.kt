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
public class NavOptionsBuilder {
    private val builder = NavOptions.Builder()

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     *
     * This functions similarly to how [android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP]
     * works with activites.
     */
    public var launchSingleTop: Boolean = false

    /**
     * Whether this navigation action should restore any state previously saved
     * by [PopUpToBuilder.saveState] or the `popUpToSaveState` attribute. If no state was
     * previously saved with the destination ID being navigated to, this has no effect.
     */
    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public var restoreState: Boolean = false

    /**
     * Returns the current destination that the builder will pop up to.
     */
    @IdRes
    public var popUpToId: Int = -1
        internal set(value) {
            field = value
            inclusive = false
        }

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until this destination is found.
     */
    @Deprecated("Use the popUpToId property.")
    public var popUpTo: Int
        get() = popUpToId
        @Deprecated("Use the popUpTo function and passing in the id.")
        set(value) {
            popUpTo(value)
        }

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until this destination is found.
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
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until this destination is found.
     */
    public fun popUpTo(@IdRes id: Int, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {
        popUpToId = id
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destination routes
     * from the back stack until the destination with a matching route is found.
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
     * Sets any custom Animation or Animator resources that should be used.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    public fun anim(animBuilder: AnimBuilder.() -> Unit) {
        AnimBuilder().apply(animBuilder).run {
            this@NavOptionsBuilder.builder.setEnterAnim(enter)
                .setExitAnim(exit)
                .setPopEnterAnim(popEnter)
                .setPopExitAnim(popExit)
        }
    }

    internal fun build() = builder.apply {
        setLaunchSingleTop(launchSingleTop)
        setRestoreState(restoreState)
        if (popUpToRoute != null) {
            setPopUpTo(popUpToRoute, inclusive, saveState)
        } else {
            setPopUpTo(popUpToId, inclusive, saveState)
        }
    }.build()
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

/**
 * DSL for setting custom Animation or Animator resources on a [NavOptionsBuilder]
 */
@NavOptionsDsl
public class AnimBuilder {
    /**
     * The custom Animation or Animator resource for the enter animation.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    public var enter: Int = -1

    /**
     * The custom Animation or Animator resource for the exit animation.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    public var exit: Int = -1

    /**
     * The custom Animation or Animator resource for the enter animation
     * when popping off the back stack.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    public var popEnter: Int = -1

    /**
     * The custom Animation or Animator resource for the exit animation
     * when popping off the back stack.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    public var popExit: Int = -1
}
