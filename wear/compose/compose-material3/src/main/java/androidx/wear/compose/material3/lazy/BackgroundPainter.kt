/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress

internal class ScalingMorphingBackgroundPainter(
    private val behavior: TransformingLazyColumnScrollTransformBehavior,
    private val shape: Shape,
    private val border: BorderStroke?,
    private val backgroundPainter: Painter,
    private val progress: DrawScope.() -> TransformingLazyColumnItemScrollProgress
) : Painter() {
    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        with(behavior) {
            progress()
                .takeIf { it != TransformingLazyColumnItemScrollProgress.Unspecified }
                ?.let {
                    val contentWidth =
                        (1f - 2 * (1f - it.backgroundXOffsetFraction)) * size.width * it.scale
                    val xOffset = (size.width - contentWidth) / 2f

                    translate(xOffset, 0f) {
                        val placementHeight = it.placementHeight(size.height)
                        val shapeOutline =
                            shape.createOutline(
                                Size(contentWidth, placementHeight),
                                layoutDirection,
                                this@onDraw
                            )

                        // TODO: b/376693576 - cache the path.
                        clipPath(Path().apply { addOutline(shapeOutline) }) {
                            if (border != null) {
                                drawOutline(
                                    outline = shapeOutline,
                                    brush = border.brush,
                                    alpha = it.backgroundAlpha,
                                    style = Stroke(border.width.toPx())
                                )
                            }
                            with(backgroundPainter) { draw(Size(contentWidth, placementHeight)) }
                        }
                    }
                }
        }
    }
}

internal class BackgroundPainter(
    private val transformState: State<TransformationState?>,
    private val shape: Shape,
    private val border: BorderStroke?,
    private val backgroundPainter: Painter
) : Painter() {
    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        transformState.value?.let {
            val contentWidth =
                (1f - 2 * (1f - it.backgroundXOffsetFraction)) * size.width * it.scale
            val xOffset = (size.width - contentWidth) / 2f

            translate(xOffset, 0f) {
                val placementHeight = it.morphedHeight * it.scale // Save as placement height ?
                val shapeOutline =
                    shape.createOutline(
                        Size(contentWidth, placementHeight),
                        layoutDirection,
                        this@onDraw
                    )

                // TODO: b/376693576 - cache the path.
                clipPath(Path().apply { addOutline(shapeOutline) }) {
                    if (border != null) {
                        drawOutline(
                            outline = shapeOutline,
                            brush = border.brush,
                            alpha = it.containerAlpha,
                            style = Stroke(border.width.toPx())
                        )
                    }
                    with(backgroundPainter) {
                        draw(Size(contentWidth, placementHeight), alpha = it.containerAlpha)
                    }
                }
            }
        }
    }
}
