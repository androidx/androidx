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

package androidx.tv.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo

@Composable
internal fun Modifier.tvSurfaceGlow(
    shape: Shape,
    glow: Glow,
): Modifier {
    val color = surfaceColorAtElevation(color = glow.elevationColor, elevation = glow.elevation)
    val glowBlurRadiusPx = with(LocalDensity.current) { glow.elevation.toPx() }

    return then(
        SurfaceGlowElement(
            shape = shape,
            glowBlurRadiusPx = glowBlurRadiusPx,
            color = color,
            inspectorInfo =
                debugInspectorInfo {
                    name = "tvSurfaceGlow"
                    properties["shape"] = shape
                    properties["glow"] = glow
                }
        )
    )
}

private class SurfaceGlowElement(
    private val shape: Shape,
    private val glowBlurRadiusPx: Float,
    private val color: Color,
    private val inspectorInfo: InspectorInfo.() -> Unit
) : ModifierNodeElement<SurfaceGlowNode>() {
    override fun create(): SurfaceGlowNode {
        return SurfaceGlowNode(
            shape = shape,
            glowBlurRadiusPx = glowBlurRadiusPx,
            color = color,
        )
    }

    override fun update(node: SurfaceGlowNode) {
        node.reactToUpdates(
            newShape = shape,
            newGlowBlurRadiusPx = glowBlurRadiusPx,
            newColor = color
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + glowBlurRadiusPx.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherTyped = other as? SurfaceGlowElement ?: return false
        return shape == otherTyped.shape &&
            glowBlurRadiusPx == otherTyped.glowBlurRadiusPx &&
            color == otherTyped.color
    }
}

private class SurfaceGlowNode(
    private var shape: Shape,
    private var glowBlurRadiusPx: Float,
    private var color: Color,
) : DrawModifierNode, Modifier.Node() {
    private var paint: Paint? = null
    private var frameworkPaint: NativePaint? = null

    // This value is lazily allocated
    private var shapeOutlineCache: SurfaceShapeOutlineCache? = null

    fun reactToUpdates(
        newShape: Shape,
        newGlowBlurRadiusPx: Float,
        newColor: Color,
    ) {
        shape = newShape
        glowBlurRadiusPx = newGlowBlurRadiusPx
        color = newColor

        if (paint == null) {
            initializePaint()
        }
        setShadowLayer()
    }

    override fun ContentDrawScope.draw() {
        drawIntoCanvas { canvas ->
            if (paint == null) {
                initializePaint()
                setShadowLayer()
            }

            if (shapeOutlineCache == null) {
                shapeOutlineCache =
                    SurfaceShapeOutlineCache(
                        shape = shape,
                        size = size,
                        layoutDirection = layoutDirection,
                        density = this
                    )
            }

            when (
                val shapeOutline =
                    shapeOutlineCache!!.updatedOutline(
                        shape = shape,
                        size = size,
                        layoutDirection = layoutDirection,
                        density = this
                    )
            ) {
                is Outline.Rectangle -> canvas.drawRect(shapeOutline.rect, paint!!)
                is Outline.Rounded -> {
                    val shapeCornerRadiusX = shapeOutline.roundRect.topLeftCornerRadius.x
                    val shapeCornerRadiusY = shapeOutline.roundRect.topLeftCornerRadius.y

                    canvas.drawRoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        radiusX = shapeCornerRadiusX,
                        radiusY = shapeCornerRadiusY,
                        paint = paint!!
                    )
                }
                is Outline.Generic -> canvas.drawPath(shapeOutline.path, paint!!)
            }
        }
        drawContent()
    }

    private fun initializePaint() {
        paint = Paint()
        frameworkPaint = paint!!.asFrameworkPaint()
    }

    private fun setShadowLayer() {
        val transparentColor = color.copy(alpha = 0f).toArgb()
        val shadowColor = color.toArgb()

        frameworkPaint!!.color = transparentColor

        frameworkPaint!!.setShadowLayer(
            /* radius= */ glowBlurRadiusPx,
            /* dx= */ 0f,
            /* dy= */ 0f,
            /* shadowColor= */ shadowColor
        )
    }
}
