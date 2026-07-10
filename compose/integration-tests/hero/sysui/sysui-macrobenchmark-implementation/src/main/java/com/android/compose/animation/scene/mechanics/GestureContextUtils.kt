/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.animation.scene.mechanics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.mechanics.GestureContext
import com.android.mechanics.spec.InputDirection

/**
 * Provides a [GestureContext] retrieving gesture details from the current [SceneTransitionLayout]
 * transition, using default values as a fallback if the transition's own gesture context isn't
 * available.
 */
fun ContentScope.gestureContextOrDefault(
    defaultDragOffset: Float = 0f,
    defaultDirection: InputDirection = InputDirection.Max,
): GestureContext {
    return object : GestureContext {
        override val direction: InputDirection
            get() = gestureContext?.direction ?: defaultDirection

        override val dragOffset: Float
            get() = gestureContext?.dragOffset ?: defaultDragOffset

        private val gestureContext
            get() = layoutState.currentTransition?.gestureContext
    }
}

@Composable
fun ContentScope.rememberGestureContext(
    defaultDragOffset: Float = 0f,
    defaultDirection: InputDirection = InputDirection.Max,
): GestureContext {
    return remember(defaultDragOffset, defaultDirection) {
        gestureContextOrDefault(defaultDragOffset, defaultDirection)
    }
}
