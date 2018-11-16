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

import android.support.annotation.AnimRes
import android.support.annotation.AnimatorRes
import android.support.annotation.IdRes

@DslMarker
annotation class NavOptionsDsl

/**
 * Construct a new [NavOptions]
 */
fun navOptions(block: NavOptionsBuilder.() -> Unit): NavOptions =
        NavOptionsBuilder().apply(block).build()

/**
 * DSL for constructing a new [NavOptions]
 */
@NavOptionsDsl
class NavOptionsBuilder {
    private val builder = NavOptions.Builder()

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     *
     * This functions similarly to how [android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP]
     * works with activites.
     */
    var launchSingleTop = false

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until this destination is found.
     */
    @IdRes
    var popUpTo: Int = 0
        set(value) {
            field = value
            inclusive = false
        }
    private var inclusive = false

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until this destination is found.
     */
    fun popUpTo(@IdRes id: Int, block: PopUpToBuilder.() -> Unit) {
        popUpTo = id
        inclusive = PopUpToBuilder().apply(block).inclusive
    }

    /**
     * Sets any custom Animation or Animator resources that should be used.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    fun anim(block: AnimBuilder.() -> Unit) {
        AnimBuilder().apply(block).run {
            this@NavOptionsBuilder.builder.setEnterAnim(enter)
                    .setExitAnim(exit)
                    .setPopEnterAnim(popEnter)
                    .setPopExitAnim(popExit)
        }
    }

    internal fun build() = builder.apply {
        setLaunchSingleTop(launchSingleTop)
        setPopUpTo(popUpTo, inclusive)
    }.build()
}

/**
 * DSL for customizing [NavOptionsBuilder.popUpTo] operations.
 */
@NavOptionsDsl
class PopUpToBuilder {
    /**
     * Whether the `popUpTo` destination should be popped from the back stack.
     */
    var inclusive: Boolean = false
}

/**
 * DSL for setting custom Animation or Animator resources on a [NavOptionsBuilder]
 */
@NavOptionsDsl
class AnimBuilder {
    /**
     * The custom Animation or Animator resource for the enter animation.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    var enter = -1

    /**
     * The custom Animation or Animator resource for the exit animation.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    var exit = -1

    /**
     * The custom Animation or Animator resource for the enter animation
     * when popping off the back stack.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    var popEnter = -1

    /**
     * The custom Animation or Animator resource for the exit animation
     * when popping off the back stack.
     *
     * Note: Animator resources are not supported for navigating to a new Activity
     */
    @AnimRes
    @AnimatorRes
    var popExit = -1
}
