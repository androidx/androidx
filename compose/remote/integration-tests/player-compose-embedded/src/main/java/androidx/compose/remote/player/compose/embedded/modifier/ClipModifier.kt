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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
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

internal data class ClipCorner(val value: State<Float>, val literal: Boolean = true)

@Composable
internal fun Modifier.roundedClipRect(op: RoundedClipRectModifierOperation): Modifier {
    val behavior = LocalCoreDocument.current.densityBehavior
    val data = op.readDataReflection()

    return this.clip(
        RemoteRoundedClipShape(
            topStart = ClipCorner(rememberRemoteFloatAsState(data.x1Value), !data.x1.isNaN()),
            topEnd = ClipCorner(rememberRemoteFloatAsState(data.y1Value), !data.y1.isNaN()),
            bottomEnd = ClipCorner(rememberRemoteFloatAsState(data.y2Value), !data.y2.isNaN()),
            bottomStart = ClipCorner(rememberRemoteFloatAsState(data.x2Value), !data.x2.isNaN()),
            densityBehavior = behavior,
        )
    )
}

internal data class RemoteRoundedClipShape(
    val topStart: ClipCorner,
    val topEnd: ClipCorner,
    val bottomEnd: ClipCorner,
    val bottomStart: ClipCorner,
    val densityBehavior: Int = CoreDocument.DENSITY_BEHAVIOR_DP,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val minDimension = size.minDimension
        val fallback = minDimension / 2f
        val topStartRadius =
            topStart.resolve(minDimension, fallback, density.density, densityBehavior)
        val topEndRadius = topEnd.resolve(minDimension, fallback, density.density, densityBehavior)
        val bottomEndRadius =
            bottomEnd.resolve(minDimension, fallback, density.density, densityBehavior)
        val bottomStartRadius =
            bottomStart.resolve(minDimension, fallback, density.density, densityBehavior)

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

internal fun ClipCorner.resolve(
    minDimension: Float,
    fallback: Float,
    density: Float,
    densityBehavior: Int,
): Float {
    val v = value.value
    return when {
        !v.isFinite() -> fallback
        literal -> if (densityBehavior == CoreDocument.DENSITY_BEHAVIOR_DP) v * density else v
        else -> v
    }
}
