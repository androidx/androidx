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

package androidx.navigation.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.internal.NonAndroidDefaultNavTransitions
import androidx.navigation.compose.internal.NonAndroidDefaultNavTransitions.enterTransition
import androidx.navigation.compose.internal.NonAndroidDefaultNavTransitions.exitTransition
import androidx.navigation.compose.internal.NonAndroidDefaultNavTransitions.predictivePopEnterTransition
import androidx.navigation.compose.internal.NonAndroidDefaultNavTransitions.predictivePopExitTransition
import androidx.navigation.compose.internal.NonAndroidDefaultNavTransitions.sizeTransform

public actual object DefaultNavTransitions {
    public actual val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = NonAndroidDefaultNavTransitions.enterTransition

    public actual val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = NonAndroidDefaultNavTransitions.exitTransition

    public actual val predictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition
        get() = NonAndroidDefaultNavTransitions.predictivePopEnterTransition

    public actual val predictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition
        get() = NonAndroidDefaultNavTransitions.predictivePopExitTransition


    public actual val sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)?
        get() = NonAndroidDefaultNavTransitions.sizeTransform

    public actual fun popEnterTransition(enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        NonAndroidDefaultNavTransitions.popEnterTransition(enterTransition)

    public actual fun popExitTransition(exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        NonAndroidDefaultNavTransitions.popExitTransition(exitTransition)
}