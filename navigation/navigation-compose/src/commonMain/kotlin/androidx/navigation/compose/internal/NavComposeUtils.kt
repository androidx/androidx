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

package androidx.navigation.compose.internal

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.Flow

internal expect class BackEventCompat {
    val touchX: Float
    val touchY: Float
    val progress: Float
    val swipeEdge: Int
}

@Composable
internal expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
)

internal expect fun randomUUID(): String

/**
 * Class WeakReference encapsulates weak reference to an object, which could be used to either
 * retrieve a strong reference to an object, or return null, if object was already destroyed by the
 * memory manager.
 */
internal expect class WeakReference<T : Any>(reference: T) {
    fun get(): T?

    fun clear()
}

internal expect object DefaultNavTransitions {
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
    val predictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition
    val predictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition
    val sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)?
}

internal object StandardDefaultNavTransitions {
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(700))
    }
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(700))
    }
    val predictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition =
        {
            fadeIn(
                spring(
                    dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                    stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                )
            )
        }
    val predictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition =
        {
            scaleOut(targetScale = 0.7f) // reflects material3 motionScheme.defaultEffectsSpec()
        }
    val sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? =
        null
}
