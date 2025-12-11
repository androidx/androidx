/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation3.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEvent.SwipeEdge

private const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 500
private val IosTransitionEasing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)

public actual fun <T : Any> defaultTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND, easing = IosTransitionEasing),
        ),
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            targetOffset = { it / 4 },
            animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND, easing = IosTransitionEasing),
        ),
    )
}

public actual fun <T : Any> defaultPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            initialOffset = { it / 4 },
            animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND, easing = IosTransitionEasing),
        ),
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND, easing = IosTransitionEasing),
        ),
    )
}

public actual fun <T : Any> defaultPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(@SwipeEdge Int) -> ContentTransform = { edge ->
    val towards = if (edge == EDGE_LEFT) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
    ContentTransform(
        slideIntoContainer(
            towards = towards,
            initialOffset = { it / 4 },
            animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND, easing = LinearEasing),
        ),
        slideOutOfContainer(
            towards = towards,
            animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND, easing = LinearEasing),
        ),
    )
}
