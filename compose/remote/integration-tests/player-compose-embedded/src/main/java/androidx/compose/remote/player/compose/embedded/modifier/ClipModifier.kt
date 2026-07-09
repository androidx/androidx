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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.player.compose.embedded.readDataReflection
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Composable
internal fun Modifier.clipRect(op: ClipRectModifierOperation): Modifier {
    return this.clip(RectangleShape)
}

@Composable
internal fun Modifier.roundedClipRect(op: RoundedClipRectModifierOperation): Modifier {
    val data = op.readDataReflection()
    val topStartPx = rememberRemoteFloatAsState(data.x1Value)
    val topEndPx = rememberRemoteFloatAsState(data.y1Value)
    val bottomEndPx = rememberRemoteFloatAsState(data.y2Value)
    val bottomStartPx = rememberRemoteFloatAsState(data.x2Value)

    return this.clip(
        RemoteRoundedClipShape(
            topStart = topStartPx,
            topEnd = topEndPx,
            bottomEnd = bottomEndPx,
            bottomStart = bottomStartPx,
        )
    )
}

internal data class RemoteRoundedClipShape(
    val topStart: State<Float>,
    val topEnd: State<Float>,
    val bottomEnd: State<Float>,
    val bottomStart: State<Float>,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val minDimension = size.minDimension
        val fallback = minDimension / 2f
        val topStartRadius = topStart.value.componentRelativeRadius(fallback, minDimension)
        val topEndRadius = topEnd.value.componentRelativeRadius(fallback, minDimension)
        val bottomEndRadius = bottomEnd.value.componentRelativeRadius(fallback, minDimension)
        val bottomStartRadius = bottomStart.value.componentRelativeRadius(fallback, minDimension)

        return Outline.Rounded(
            RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                topLeft = CornerRadius(topStartRadius),
                topRight = CornerRadius(topEndRadius),
                bottomRight = CornerRadius(bottomEndRadius),
                bottomLeft = CornerRadius(bottomStartRadius),
            )
        )
    }
}

internal fun Float.componentRelativeRadius(fallback: Float, minDimension: Float): Float =
    when {
        !isFinite() -> fallback
        // Percent corner sizes can arrive as 0..1 fractions before the component-size expression
        // has settled into pixels.
        this > 0f && this <= 1f -> this * minDimension
        else -> this
    }
