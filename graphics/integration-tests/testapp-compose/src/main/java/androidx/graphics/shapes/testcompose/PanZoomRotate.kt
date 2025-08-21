/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pinch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class PanZoomRotateBoxState(
    var zoom: MutableState<Float> = mutableFloatStateOf(START_ZOOM),
    var offset: MutableState<Offset> = mutableStateOf(START_OFFSET),
    var angle: MutableState<Float> = mutableFloatStateOf(START_ANGLE),
) {
    fun reset() {
        zoom.value = START_ZOOM
        offset.value = START_OFFSET
        angle.value = START_ANGLE
    }

    fun hasChanged() =
        zoom.value != START_ZOOM || offset.value != START_OFFSET || angle.value != START_ANGLE

    fun mapOut(p: Offset): Offset {
        return ((p.rotate(angle.value.toRadians()) - offset.value) * zoom.value)
    }
}

// Wrap the content in a box that adds a gesture detector and a transform layer.
// This lets the content be scaled/rotated/panned.
// Small note that AFAIK, the center of the gestures on the emulator are at the center of the
// screen, so for this to work on emulator the center of the screen needs to be inside this
// component.
@Composable
fun PanZoomRotateBox(
    modifier: Modifier = Modifier,
    state: PanZoomRotateBoxState = remember { PanZoomRotateBoxState() },
    allowRotation: Boolean = true,
    allowZoom: Boolean = true,
    allowPan: Boolean = true,
    showInteraction: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    with(state) {
        Box(modifier = modifier) {
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                                val actualRotation = if (allowRotation) gestureRotate else 0f
                                val oldScale = zoom.value
                                val newScale = zoom.value * if (allowZoom) gestureZoom else 1f

                                // For natural zooming and rotating, the centroid of the gesture
                                // should
                                // be the fixed point where zooming and rotating occurs.
                                // We compute where the centroid was (in the pre-transformed
                                // coordinate
                                // space), and then compute where it will be after this delta.
                                // We then compute what the new offset should be to keep the
                                // centroid
                                // visually stationary for rotating and zooming, and also apply the
                                // pan.
                                offset.value =
                                    (offset.value + centroid / oldScale).rotate(
                                        actualRotation.toRadians()
                                    ) -
                                        (centroid / newScale +
                                            (if (allowPan) pan else Offset.Zero) / oldScale)
                                zoom.value = newScale
                                angle.value += actualRotation
                            }
                        )
                    }
                    .graphicsLayer {
                        translationX = -offset.value.x * zoom.value
                        translationY = -offset.value.y * zoom.value
                        scaleX = zoom.value
                        scaleY = zoom.value
                        rotationZ = angle.value
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
                content = content,
            )
            if (showInteraction && hasChanged()) {
                Button(onClick = { state.reset() }) {
                    Text("Reset View", textAlign = TextAlign.Center)
                }
            } else if (showInteraction) {
                Icon(Icons.Default.Pinch, "Zoom in", Modifier.alpha(0.2f))
            }
        }
    }
}

internal fun Float.toRadians() = this * PI.toFloat() / 180f

private fun Offset.rotate90() = Offset(-y, x)

internal fun directionVector(angleRadians: Float) = Offset(cos(angleRadians), sin(angleRadians))

private fun Offset.rotate(angleRadians: Float): Offset {
    val vec = directionVector(angleRadians)
    return vec * x + vec.rotate90() * y
}

private const val START_ZOOM = 1f
private const val START_ANGLE = 0f
private val START_OFFSET = Offset.Zero
