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
import kotlin.reflect.KClass

@NavOptionsDsl
public actual class NavOptionsBuilder actual constructor() {
    private val builder = NavOptions.Builder()

    public actual var launchSingleTop: Boolean = false

    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public actual var restoreState: Boolean = false

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

    public actual var popUpToRoute: String? = null
        private set(value) {
            if (value != null) {
                require(value.isNotBlank()) { "Cannot pop up to an empty route" }
                field = value
                inclusive = false
            }
        }

    private var inclusive = false
    private var saveState = false

    @get:Suppress("GetterOnBuilder")
    public actual var popUpToRouteClass: KClass<*>? = null
        private set(value) {
            if (value != null) {
                field = value
                inclusive = false
            }
        }

    @get:Suppress("GetterOnBuilder")
    public actual var popUpToRouteObject: Any? = null
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

    public actual fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        popUpToRoute = route
        popUpToId = -1
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    // align with other popUpTo overloads where this is suppressed in baseline lint ignore
    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> popUpTo(
        noinline popUpToBuilder: PopUpToBuilder.() -> Unit
    ) {
        popUpTo(T::class, popUpToBuilder)
    }

    @Suppress("BuilderSetStyle")
    public actual fun <T : Any> popUpTo(
        route: KClass<T>,
        popUpToBuilder: PopUpToBuilder.() -> Unit,
    ) {
        popUpToRouteClass = route
        popUpToId = -1
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    // align with other popUpTo overloads where this is suppressed in baseline lint ignore
    @Suppress("BuilderSetStyle", "MissingJvmstatic")
    public actual fun <T : Any> popUpTo(route: T, popUpToBuilder: PopUpToBuilder.() -> Unit) {
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

    internal actual fun build() =
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
