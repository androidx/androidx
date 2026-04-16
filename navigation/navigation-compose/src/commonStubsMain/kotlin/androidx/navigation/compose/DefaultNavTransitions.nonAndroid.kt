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
import androidx.navigation.compose.internal.implementedInJetBrainsFork
import kotlin.jvm.JvmName

/** Default nav transitions to be used by [NavHost]. */
public actual object DefaultNavTransitions {

    /** Default [enterTransition] for forward navigation to be used by [NavHost]. */
    public actual val enterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = implementedInJetBrainsFork()

    /** Default [exitTransition] for forward navigation to be used by [NavHost]. */
    public actual val exitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = implementedInJetBrainsFork()

    /** Default [popEnterTransition] for pop navigation to be used by [NavHost]. */
    public actual fun popEnterTransition(
        enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        implementedInJetBrainsFork()

    /** Default [popExitTransition] for pop navigation to be used by [NavHost]. */
    public actual fun popExitTransition(
        exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        implementedInJetBrainsFork()

    /**
     * Default [predictivePopEnterTransition] for predictive pop navigation to be used by [NavHost].
     */
    public actual val predictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(swipeEdge: Int) -> EnterTransition
        get() = implementedInJetBrainsFork()

    /**
     * Default [predictivePopExitTransition] for predictive pop navigation to be used by [NavHost].
     */
    public actual val predictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(swipeEdge: Int) -> ExitTransition
        get() = implementedInJetBrainsFork()

    /** Default [sizeTransform] to be used by [NavHost]. */
    public actual val sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)?
        get() = implementedInJetBrainsFork()
}
