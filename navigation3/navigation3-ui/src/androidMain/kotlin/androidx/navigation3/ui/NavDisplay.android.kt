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

@file:JvmName("NavDisplayKt")
@file:JvmMultifileClass

package androidx.navigation3.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

internal const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 700

public actual fun <T : Any> defaultTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
        fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
    )
}

public actual fun <T : Any> defaultPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
        fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
    )
}

public actual fun <T : Any> defaultPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    {
        ContentTransform(
            fadeIn(
                spring(
                    dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                    stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                )
            ),
            scaleOut(targetScale = 0.7f),
        )
    }
