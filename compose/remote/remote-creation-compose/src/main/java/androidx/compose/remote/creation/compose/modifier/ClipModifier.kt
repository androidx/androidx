/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.layout.RemoteFloatContext
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.modifiers.ClipModifier as CoreClipModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RectShape as CoreRectShape
import androidx.compose.remote.creation.modifiers.RoundedRectShape as CoreRoundedRectShape
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.modifiers.UnsupportedModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClipModifier(public val shape: RemoteShape = RemoteRectangleShape) :
    RemoteModifier.Element {
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        val coreShape =
            when (shape) {
                RemoteRectangleShape -> CoreRectShape(0f, 0f, 0f, 0f)
                RemoteCircleShape -> {
                    val context = RemoteFloatContext(this)
                    val remoteSize = RemoteSize(context.componentWidth(), context.componentHeight())
                    val radius = min(remoteSize.width, remoteSize.height).div(2f)
                    CoreRoundedRectShape(
                        radius.floatId,
                        radius.floatId,
                        radius.floatId,
                        radius.floatId,
                    )
                }
                is RemoteRoundedCornerShape -> {
                    val context = RemoteFloatContext(this)
                    val remoteSize = RemoteSize(context.componentWidth(), context.componentHeight())
                    CoreRoundedRectShape(
                        shape.topStart.toPx(remoteSize, remoteDensity).floatId,
                        shape.topEnd.toPx(remoteSize, remoteDensity).floatId,
                        shape.bottomStart.toPx(remoteSize, remoteDensity).floatId,
                        shape.bottomEnd.toPx(remoteSize, remoteDensity).floatId,
                    )
                }
                else -> CoreRectShape(0f, 0f, 0f, 0f)
            }

        return CoreClipModifier(coreShape)
    }
}

public fun RemoteModifier.clip(shape: RemoteShape = RemoteRectangleShape): RemoteModifier =
    then(ClipModifier(shape))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
private class LegacyClipModifier(
    public val shape: Shape,
    public val size: DpSize,
    public val density: Density,
) : RemoteModifier.Element {
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        val remoteShape = remoteShape()

        return if (remoteShape == null) {
            UnsupportedModifier("ClipModifier($shape size: $size)")
        } else {
            androidx.compose.remote.creation.modifiers.ClipModifier(remoteShape)
        }
    }

    private fun remoteShape(): RoundedRectShape? {
        val pxSize = with(density) { size.toSize() }

        return when (shape) {
            CircleShape -> {
                // TODO how to avoid needing the size here
                if (pxSize.isUnspecified) return null

                val cornerSize = pxSize.minDimension
                RoundedRectShape(cornerSize, cornerSize, cornerSize, cornerSize)
            }
            is RoundedCornerShape -> {
                RoundedRectShape(
                    shape.topStart.toPx(pxSize, density),
                    shape.topEnd.toPx(pxSize, density),
                    shape.bottomStart.toPx(pxSize, density),
                    shape.bottomEnd.toPx(pxSize, density),
                )
            }
            else -> {
                System.err.println("Unhandled clip shape $shape size: $size")
                null
            }
        }
    }
}

// @Deprecated
// To be replaced by form above completely once API approved.
@Composable
public fun RemoteModifier.clip(shape: Shape, size: DpSize = DpSize.Unspecified): RemoteModifier =
    then(LegacyClipModifier(shape, size, LocalDensity.current))
