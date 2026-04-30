/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("DefaultNavTransitions")

package androidx.navigation.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.navigation.NavBackStackEntry
import kotlin.jvm.JvmName

/** Default nav transitions to be used by [NavHost]. */
public expect object DefaultNavTransitions {

    /** Default [enterTransition] for forward navigation to be used by [NavHost]. */
    public val enterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition

    /** Default [exitTransition] for forward navigation to be used by [NavHost]. */
    public val exitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

    /** Default [popEnterTransition] for pop navigation to be used by [NavHost]. */
    public fun popEnterTransition(
        enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition

    /** Default [popExitTransition] for pop navigation to be used by [NavHost]. */
    public fun popExitTransition(
        exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

    /**
     * Default [predictivePopEnterTransition] for predictive pop navigation to be used by [NavHost].
     */
    public val predictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(swipeEdge: Int) -> EnterTransition

    /**
     * Default [predictivePopExitTransition] for predictive pop navigation to be used by [NavHost].
     */
    public val predictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(swipeEdge: Int) -> ExitTransition

    /** Default [sizeTransform] to be used by [NavHost]. */
    public val sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)?
}
