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

import androidx.annotation.RestrictTo
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
import kotlin.math.min

@Composable
internal fun Modifier.clipRect(op: ClipRectModifierOperation): Modifier {
    return this.clip(RectangleShape)
}

@Composable
internal fun Modifier.roundedClipRect(
    op: RoundedClipRectModifierOperation,
    hoistBeforeDraw: Boolean = false,
): Modifier {
    val behavior = LocalCoreDocument.current.densityBehavior
    val data = op.readDataReflection()

    val shape =
        RemoteRoundedClipShape(
            topStart = rememberRemoteFloatAsState(data.x1Value),
            topEnd = rememberRemoteFloatAsState(data.y1Value),
            bottomEnd = rememberRemoteFloatAsState(data.y2Value),
            bottomStart = rememberRemoteFloatAsState(data.x2Value),
            densityBehavior = behavior,
        )
    // When draw content has already been processed in the modifier list (hoistBeforeDraw == true),
    // hoist the clip modifier before the draw node so content is clipped. If no draw node was
    // processed yet, preserve the existing modifier order so preceding padding is not bypassed.
    return if (hoistBeforeDraw) {
        Modifier.clip(shape).then(this)
    } else {
        this.clip(shape)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RemoteRoundedClipShape(
    val topStart: State<Float>,
    val topEnd: State<Float>,
    val bottomEnd: State<Float>,
    val bottomStart: State<Float>,
    val densityBehavior: Int = CoreDocument.DENSITY_BEHAVIOR_DP,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val fallback = size.minDimension / 2f
        val topStartRadius = topStart.resolveRadius(fallback, density.density, densityBehavior)
        val topEndRadius = topEnd.resolveRadius(fallback, density.density, densityBehavior)
        val bottomEndRadius = bottomEnd.resolveRadius(fallback, density.density, densityBehavior)
        val bottomStartRadius =
            bottomStart.resolveRadius(fallback, density.density, densityBehavior)

        val radiusScale =
            roundedRectRadiusScale(
                size,
                topStartRadius,
                topEndRadius,
                bottomEndRadius,
                bottomStartRadius,
            )

        return Outline.Rounded(
            RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                topLeft = CornerRadius(topStartRadius * radiusScale),
                topRight = CornerRadius(topEndRadius * radiusScale),
                bottomRight = CornerRadius(bottomEndRadius * radiusScale),
                bottomLeft = CornerRadius(bottomStartRadius * radiusScale),
            )
        )
    }
}

/** Matches the radius normalization performed by Android's Path.addRoundRect in remote-core. */
private fun roundedRectRadiusScale(
    size: Size,
    topStart: Float,
    topEnd: Float,
    bottomEnd: Float,
    bottomStart: Float,
): Float {
    fun scaleFor(limit: Float, first: Float, second: Float): Float {
        val sum = first + second
        return if (sum > limit && sum != 0f) limit / sum else 1f
    }
    return min(
        min(scaleFor(size.width, topStart, topEnd), scaleFor(size.width, bottomStart, bottomEnd)),
        min(scaleFor(size.height, topStart, bottomStart), scaleFor(size.height, topEnd, bottomEnd)),
    )
}

internal fun State<Float>.resolveRadius(
    fallback: Float,
    density: Float,
    densityBehavior: Int,
): Float {
    val v = value
    return when {
        !v.isFinite() -> fallback
        // Under DENSITY_BEHAVIOR_DP, both literal and variable-backed corner radii represent DP
        // values and must be scaled by density to match remote-core behavior.
        densityBehavior == CoreDocument.DENSITY_BEHAVIOR_DP -> v * density
        else -> v
    }
}
