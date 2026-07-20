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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material3.R
import androidx.wear.compose.material3.internal.LocalWristOrientation
import androidx.wear.compose.material3.internal.isLeftWrist

@Composable
internal fun GestureIndicatorImage(
    painter: Painter,
    size: DpSize,
    tint: Color,
    scaleX: () -> Float = { 1.0f },
    scaleY: () -> Float = { 1.0f },
) {
    // animatedVectorPainter hardcodes autoMirror = true, which reacts to
    // LocalLayoutDirection. To gain manual control over mirroring, we force
    // LayoutDirection.Ltr and apply a horizontal scale based on the wrist orientation.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val wristOrientation = LocalWristOrientation.current
        Image(
            painter = painter,
            contentDescription = null,
            modifier =
                Modifier.size(size).graphicsLayer {
                    // Mirror the image only when worn on the right hand
                    this.scaleX = if (wristOrientation.isLeftWrist()) scaleX() else -scaleX()
                    this.scaleY = scaleY()
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
internal fun GestureAction.animatedImageVector(): AnimatedImageVector {
    val resourceId =
        when (this) {
            GestureAction.Primary -> R.drawable.wear_one_handed_gesture_primary_indicator_animation
            else -> R.drawable.wear_one_handed_gesture_dismiss_indicator_animation
        }
    return AnimatedImageVector.animatedVectorResource(resourceId)
}

internal val EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT =
    spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 350f)

internal val EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)

internal val EXPRESSIVE_DEFAULT_EFFECTS_SPRING_COLOR =
    spring<Color>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)

internal const val INDICATOR_ANIMATION_START_DELAY_MILLIS = 450L
internal const val POST_INDICATOR_ANIMATION_DELAY_MILLIS = 200L
