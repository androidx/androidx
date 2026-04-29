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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.navigation.NavBackStackEntry
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT

private const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 500
private val IosTransitionEasing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)

@OptIn(ExperimentalAnimationApi::class)
public actual object DefaultNavTransitions {
    public actual val enterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
                DEFAULT_TRANSITION_DURATION_MILLISECOND,
                easing = IosTransitionEasing
            ),
        )
    }

    public actual val exitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            targetOffset = { it / 4 },
            animationSpec = tween(
                DEFAULT_TRANSITION_DURATION_MILLISECOND,
                easing = IosTransitionEasing
            ),
        ) + veilOut(
            animationSpec = tween(
                DEFAULT_TRANSITION_DURATION_MILLISECOND,
                easing = IosTransitionEasing
            ),
        )
    }

    public actual val predictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition =
        { edge ->
            val towards = if (edge == EDGE_LEFT) {
                AnimatedContentTransitionScope.SlideDirection.Right
            } else {
                AnimatedContentTransitionScope.SlideDirection.Left
            }
            slideIntoContainer(
                towards = towards,
                initialOffset = { it / 4 },
                animationSpec = tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                    easing = LinearEasing
                ),
            ) + unveilIn(
                animationSpec = tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                    easing = LinearEasing
                ),
            )
        }

    public actual val predictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition =
        { edge ->
            val towards = if (edge == EDGE_LEFT) {
                AnimatedContentTransitionScope.SlideDirection.Right
            } else {
                AnimatedContentTransitionScope.SlideDirection.Left
            }
            slideOutOfContainer(
                towards = towards,
                animationSpec = tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                    easing = LinearEasing
                ),
            )
        }


    public actual val sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? =
        null

    public actual fun popEnterTransition(
        enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            initialOffset = { it / 4 },
            animationSpec = tween(
                DEFAULT_TRANSITION_DURATION_MILLISECOND,
                easing = IosTransitionEasing
            ),
        ) + unveilIn(
            animationSpec = tween(
                DEFAULT_TRANSITION_DURATION_MILLISECOND,
                easing = IosTransitionEasing
            )
        )
    }

    public actual fun popExitTransition(
        exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    ): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(
                DEFAULT_TRANSITION_DURATION_MILLISECOND,
                easing = IosTransitionEasing
            ),
        )
    }
}