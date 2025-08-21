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
package androidx.compose.remote.frontend.modifier

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.modifiers.UnsupportedModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize

class ClipModifier(val shape: Shape, val size: DpSize, val density: Density) :
    RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
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

    @Composable override fun Modifier.toComposeUi(): Modifier = clip(shape)
}

@Composable
fun RemoteModifier.clip(shape: Shape, size: DpSize = DpSize.Unspecified): RemoteModifier =
    then(ClipModifier(shape, size, LocalDensity.current))
