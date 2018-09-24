/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.basic

import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.painting.BlendMode
import androidx.ui.painting.BoxFit
import androidx.ui.painting.Color
import androidx.ui.painting.Image
import androidx.ui.painting.ImageRepeat
import androidx.ui.painting.alignment.Alignment
import androidx.ui.painting.alignment.AlignmentGeometry
import androidx.ui.rendering.RenderImage
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.debugCheckHasDirectionality
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.LeafRenderObjectWidget

/**
 * A widget that displays a [dart:ui.Image] directly.
 *
 * The image is painted using [paintImage], which describes the meanings of the
 * various fields on this class in more detail.
 *
 * This widget is rarely used directly. Instead, consider using [Image].
 *
 * Ctor comment:
 * Creates a widget that displays an image.
 *
 * The [scale], [alignment], [repeat], and [matchTextDirection] arguments must
 * not be null.
 */
class RawImage(
    key: Key,
    /** The image to display. */
    val image: Image,
    /**
     * If non-null, require the image to have this width.
     *
     * If null, the image will pick a size that best preserves its intrinsic
     * aspect ratio.
     */
    val width: Double? = null,
    /**
     * If non-null, require the image to have this height.
     *
     * If null, the image will pick a size that best preserves its intrinsic
     * aspect ratio.
     */
    val height: Double? = null,
    /**
     * Specifies the image's scale.
     *
     * Used when determining the best display size for the image.
     */
    val scale: Double = 1.0,
    /** If non-null, this color is blended with each image pixel using [colorBlendMode]. */
    val color: Color? = null,
    /**
     * Used to combine [color] with this image.
     *
     * The default is [BlendMode.srcIn]. In terms of the blend mode, [color] is
     * the source and this image is the destination.
     *
     * See also:
     *
     *  * [BlendMode], which includes an illustration of the effect of each blend mode.
     */
    val colorBlendMode: BlendMode? = null,
    /**
     * How to inscribe the image into the space allocated during layout.
     *
     * The default varies based on the other fields. See the discussion at
     * [paintImage].
     */
    val fit: BoxFit? = null,
    /**
     * How to align the image within its bounds.
     *
     * The alignment aligns the given position in the image to the given position
     * in the layout bounds. For example, an [Alignment] alignment of (-1.0,
     * -1.0) aligns the image to the top-left corner of its layout bounds, while a
     * [Alignment] alignment of (1.0, 1.0) aligns the bottom right of the
     * image with the bottom right corner of its layout bounds. Similarly, an
     * alignment of (0.0, 1.0) aligns the bottom middle of the image with the
     * middle of the bottom edge of its layout bounds.
     *
     * To display a subpart of an image, consider using a [CustomPainter] and
     * [Canvas.drawImageRect].
     *
     * If the [alignment] is [TextDirection]-dependent (i.e. if it is a
     * [AlignmentDirectional]), then an ambient [Directionality] widget
     * must be in scope.
     *
     * Defaults to [Alignment.center].
     *
     * See also:
     *
     *  * [Alignment], a class with convenient constants typically used to
     *    specify an [AlignmentGeometry].
     *  * [AlignmentDirectional], like [Alignment] for specifying alignments
     *    relative to text direction.
     */
    val alignment: AlignmentGeometry = Alignment.center,
    /** How to paint any portions of the layout bounds not covered by the image. */
    val repeat: ImageRepeat = ImageRepeat.noRepeat,
    /**
     * The center slice for a nine-patch image.
     *
     * The region of the image inside the center slice will be stretched both
     * horizontally and vertically to fit the image into its destination. The
     * region of the image above and below the center slice will be stretched
     * only horizontally and the region of the image to the left and right of
     * the center slice will be stretched only vertically.
     */
    val centerSlice: Rect? = null,
    /**
     * Whether to paint the image in the direction of the [TextDirection].
     *
     * If this is true, then in [TextDirection.ltr] contexts, the image will be
     * drawn with its origin in the top left (the "normal" painting direction for
     * images); and in [TextDirection.rtl] contexts, the image will be drawn with
     * a scaling factor of -1 in the horizontal direction so that the origin is
     * in the top right.
     *
     * This is occasionally used with images in right-to-left environments, for
     * images that were designed for left-to-right locales. Be careful, when
     * using this, to not flip images with integral shadows, text, or other
     * effects that will look incorrect when flipped.
     *
     * If this is true, there must be an ambient [Directionality] widget in
     * scope.
     */
    val matchTextDirection: Boolean = false
) : LeafRenderObjectWidget(key) {

    init {
        assert(scale != null)
        assert(alignment != null)
        assert(repeat != null)
        assert(matchTextDirection != null)
    }

    override fun createRenderObject(context: BuildContext): RenderImage {
        assert((!matchTextDirection && alignment is Alignment) ||
                debugCheckHasDirectionality(context))
        return RenderImage(
                image = image,
                width = width,
                height = height,
                scale = scale,
                color = color,
                colorBlendMode = colorBlendMode,
                fit = fit,
                alignment = alignment,
                repeat = repeat,
                centerSlice = centerSlice,
                matchTextDirection = matchTextDirection,
                textDirection = if (matchTextDirection || alignment !is Alignment)
                    Directionality.of(context) else null
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) {
        (renderObject as RenderImage).let {
            it.image = image
            it.width = width
            it.height = height
            it.scale = scale
            it.color = color
            it.colorBlendMode = colorBlendMode
            it.alignment = alignment
            it.fit = fit
            it.repeat = repeat
            it.centerSlice = centerSlice
            it.matchTextDirection = matchTextDirection
            it.textDirection = if (matchTextDirection || alignment !is Alignment)
                Directionality.of(context) else null
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("image", image))
        properties.add(DoubleProperty.create("width", width, defaultValue = null))
        properties.add(DoubleProperty.create("height", height, defaultValue = null))
        properties.add(DoubleProperty.create("scale", scale, defaultValue = 1.0))
        properties.add(DiagnosticsProperty.create("color", color, defaultValue = null))
        properties.add(EnumProperty("colorBlendMode", colorBlendMode, defaultValue = null))
        properties.add(EnumProperty("fit", fit, defaultValue = null))
        properties.add(DiagnosticsProperty.create("alignment", alignment, defaultValue = null))
        properties.add(EnumProperty("repeat", repeat, defaultValue = ImageRepeat.noRepeat))
        properties.add(DiagnosticsProperty.create("centerSlice", centerSlice, defaultValue = null))
        properties.add(FlagProperty(
                "matchTextDirection",
                value = matchTextDirection,
                ifTrue = "match text direction"
        ))
    }
}