/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec.Companion.NoOpTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.TransformedContainerPainterScope

/**
 * Object to be used to apply different transformation to the content and the container (i.e. the
 * background) of the composable.
 *
 * This interface allows you to customize the appearance of a surface by modifying the container
 * painter and applying visual transformations to the content. In this context, a surface is a
 * container composable that displays content (which could use other Composables such as Icon, Text
 * or Button) as well as a background typically drawn using a painter. This is useful for creating
 * custom effects like scaling, rotation, or applying shaders.
 *
 * Example usage with the [Button]:
 *
 * @sample androidx.wear.compose.material3.samples.SurfaceTransformationButtonSample
 *
 * Example usage with the [Card]:
 *
 * @sample androidx.wear.compose.material3.samples.SurfaceTransformationCardSample
 *
 * Example of adding support in a custom component:
 *
 * @sample androidx.wear.compose.material3.samples.SurfaceTransformationOnCustomComponent
 */
public interface SurfaceTransformation {
    /**
     * Returns a new painter to be used instead of [painter] which should react on a transformation.
     *
     * This allows the transformation to modify the container painter based on properties like the
     * shape or border. For example, a transformation might apply a gradient that follows the shape
     * of the surface.
     *
     * @param painter The original painter.
     * @param shape The shape of the content to be used for clipping.
     * @param border The border to be applied to the container.
     */
    public fun createContainerPainter(
        painter: Painter,
        shape: Shape,
        border: BorderStroke? = null,
    ): Painter

    /**
     * Visual transformations to be applied to the container of the item.
     *
     * This function is called within a [GraphicsLayerScope], allowing you to use properties like
     * `scaleX`, `scaleY`, `rotationZ`, `alpha`, and others to transform the content.
     */
    public fun GraphicsLayerScope.applyContainerTransformation()

    /**
     * Visual transformations to be applied to the content of the item.
     *
     * This function is called within a [GraphicsLayerScope], allowing you to use properties like
     * `scaleX`, `scaleY`, `rotationZ`, `alpha`, and others to transform the content.
     */
    public fun GraphicsLayerScope.applyContentTransformation()
}

internal val NoOpSurfaceTransformation: SurfaceTransformation =
    object : SurfaceTransformation {
        override fun createContainerPainter(
            painter: Painter,
            shape: Shape,
            border: BorderStroke?,
        ): Painter =
            object : Painter() {
                override val intrinsicSize: Size
                    get() = Size.Unspecified

                private var lastUsedSize = Size.Unspecified
                private val cachedPath: Path = Path()

                override fun DrawScope.onDraw() {
                    if (size != lastUsedSize) {
                        cachedPath.reset()
                        cachedPath.addOutline(
                            shape.createOutline(size, layoutDirection, this@onDraw)
                        )
                        lastUsedSize = size
                    }

                    clipPath(path = cachedPath) {
                        if (border != null) {
                            drawOutline(
                                outline = shape.createOutline(size, layoutDirection, this@onDraw),
                                brush = border.brush,
                                style = Stroke(border.width.toPx()),
                            )
                        }
                        with(painter) { draw(size) }
                    }
                }
            }

        override fun GraphicsLayerScope.applyContentTransformation() {}

        override fun GraphicsLayerScope.applyContainerTransformation() {}
    }

/**
 * Exposes [androidx.wear.compose.material3.lazy.TransformationSpec] as [SurfaceTransformation] to
 * be used with Material components.
 *
 * @param spec [TransformationSpec] to be used.
 */
@Stable
public fun TransformingLazyColumnItemScope.SurfaceTransformation(
    spec: TransformationSpec
): SurfaceTransformation =
    if (spec == NoOpTransformationSpec) {
        NoOpSurfaceTransformation
    } else {
        SurfaceTransformationImpl(spec, this)
    }

private class SurfaceTransformationImpl(
    private val spec: TransformationSpec,
    private val scope: TransformingLazyColumnItemScope,
) : SurfaceTransformation, TransformedContainerPainterScope {
    override val DrawScope.scrollProgress: TransformingLazyColumnItemScrollProgress
        get() = with(scope) { scrollProgress }

    override val DrawScope.itemHeight: Float
        get() = this@itemHeight.size.height

    override fun createContainerPainter(
        painter: Painter,
        shape: Shape,
        border: BorderStroke?,
    ): Painter = with(spec) { createTransformedContainerPainter(painter, shape, border) }

    override fun GraphicsLayerScope.applyContainerTransformation() {
        with(scope) { with(spec) { applyContainerTransformation(scrollProgress) } }
    }

    override fun GraphicsLayerScope.applyContentTransformation() {
        with(scope) { with(spec) { applyContentTransformation(scrollProgress) } }
    }

    override fun hashCode(): Int = 31 * spec.hashCode() + scope.hashCode()

    override fun equals(other: Any?): Boolean {
        val otherSurfaceTransformation = other as? SurfaceTransformationImpl ?: return false
        return spec === otherSurfaceTransformation.spec &&
            scope === otherSurfaceTransformation.scope
    }
}
