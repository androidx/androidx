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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderRenderer
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Sampled
fun BorderRendererSample() {
    // Note: you may need to implement semantics here, depending on your use case.
    class SimpleBorderNode(var width: Dp, var brush: Brush, var shape: Shape) :
        Modifier.Node(), DrawModifierNode {
        // The GraphicsLayer is only created if the BorderRenderer needs one, and is released when
        // the node is detached.
        private var graphicsLayer: GraphicsLayer? = null

        // Keep the same BorderRenderer across draws so it can reuse its drawing resources.
        private val borderRenderer = BorderRenderer {
            graphicsLayer
                ?: requireGraphicsContext().createGraphicsLayer().also { graphicsLayer = it }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            borderRenderer.drawBorder(
                drawScope = this,
                width = width,
                brush = brush,
                outline = shape.createOutline(size, layoutDirection, this),
            )
        }

        override fun onDetach() {
            graphicsLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
            graphicsLayer = null
        }
    }
}

@Sampled
fun BorderRendererMultipleBordersSample() {
    // Note: you may need to implement semantics here, depending on your use case.
    class DoubleBorderNode : Modifier.Node(), DrawModifierNode {
        private val borderWidth = 2.dp
        // 2.dp between the inner edge of the outer border and the outer edge of the inner border
        private val innerBorderInset = borderWidth + 2.dp
        private val outerBrush = SolidColor(Color.Black)
        private val innerBrush = Brush.linearGradient(listOf(Color.Magenta, Color.Cyan))
        private val shape = CutCornerShape(8.dp)

        // Each border needs its own GraphicsLayer, as both may be used in the same draw.
        private var outerGraphicsLayer: GraphicsLayer? = null
        private var innerGraphicsLayer: GraphicsLayer? = null

        // Use a separate BorderRenderer for each border, so that each one can reuse its own
        // drawing resources across draws.
        private val outerBorderRenderer = BorderRenderer {
            outerGraphicsLayer
                ?: requireGraphicsContext().createGraphicsLayer().also { outerGraphicsLayer = it }
        }
        private val innerBorderRenderer = BorderRenderer {
            innerGraphicsLayer
                ?: requireGraphicsContext().createGraphicsLayer().also { innerGraphicsLayer = it }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            val outerOutline = shape.createOutline(size, layoutDirection, this)
            // Draw the outer border at the edge
            outerBorderRenderer.drawBorder(
                drawScope = this,
                width = borderWidth,
                brush = outerBrush,
                outline = outerOutline,
            )
            val inset = innerBorderInset.toPx()
            // Draw the inner border inset from the outer border
            inset(inset) {
                // The size in this scope is already inset, so this outline will be smaller
                val innerOutline = shape.createOutline(size, layoutDirection, this)
                innerBorderRenderer.drawBorder(
                    drawScope = this,
                    width = borderWidth,
                    brush = innerBrush,
                    outline = innerOutline,
                )
            }
        }

        override fun onDetach() {
            outerGraphicsLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
            innerGraphicsLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
            outerGraphicsLayer = null
            innerGraphicsLayer = null
        }
    }
}
