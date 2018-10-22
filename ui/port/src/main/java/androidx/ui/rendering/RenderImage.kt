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

package androidx.ui.rendering

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.painting.BlendMode
import androidx.ui.painting.BoxFit
import androidx.ui.painting.Color
import androidx.ui.painting.ColorFilter
import androidx.ui.painting.Image
import androidx.ui.painting.ImageRepeat
import androidx.ui.painting.alignment.Alignment
import androidx.ui.painting.alignment.AlignmentGeometry
import androidx.ui.painting.paintImage
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.engine.text.TextDirection

/**
 * An image in the render tree.
 *
 * The render image attempts to find a size for itself that fits in the given
 * constraints and preserves the image's intrinsic aspect ratio.
 *
 * The image is painted using [paintImage], which describes the meanings of the
 * various fields on this class in more detail.
 *
 * Ctor comment:
 * Creates a render box that displays an image.
 *
 * The [scale], [alignment], [repeat], and [matchTextDirection] arguments
 * must not be null. The [textDirection] argument must not be null if
 * [alignment] will need resolving or if [matchTextDirection] is true.
 */
class RenderImage(
    image: Image? = null,
    width: Double? = null,
    height: Double? = null,
    scale: Double = 1.0,
    color: Color? = null,
    colorBlendMode: BlendMode? = null,
    fit: BoxFit? = null,
    alignment: AlignmentGeometry = Alignment.center,
    repeat: ImageRepeat = ImageRepeat.noRepeat,
    centerSlice: Rect? = null,
    matchTextDirection: Boolean = false,
    textDirection: TextDirection? = null
) : RenderBox() {

    private var _image = image
    private var _width = width
    private var _height = height
    private var _scale = scale
    private var _color = color
    private var _colorBlendMode = colorBlendMode
    private var _fit = fit
    private var _alignment = alignment
    private var _repeat = repeat
    private var _centerSlice = centerSlice
    private var _matchTextDirection = matchTextDirection
    private var _textDirection = textDirection

    init {
        _updateColorFilter()
    }

    var _resolvedAlignment: Alignment? = null
    var _flipHorizontally: Boolean? = null

    private fun _resolve() {
        if (_resolvedAlignment != null)
            return
        _resolvedAlignment = alignment.resolve(textDirection)
        _flipHorizontally = matchTextDirection && textDirection == TextDirection.RTL
    }

    private fun _markNeedResolution() {
        _resolvedAlignment = null
        _flipHorizontally = null
        markNeedsPaint()
    }

    /** The image to display. */
    var image: Image?
        get() = _image
        set(value) = run {
            if (value == _image)
                return
            _image = value
            markNeedsPaint()
            if (_width == null || _height == null)
                markNeedsLayout()
        }

    /**
     * If non-null, requires the image to have this width.
     *
     * If null, the image will pick a size that best preserves its intrinsic
     * aspect ratio.
     */
    var width: Double?
        get() = _width
        set(value) = run {
            if (value == _width)
                return
            _width = value
            markNeedsLayout()
        }

    /**
     * If non-null, require the image to have this height.
     *
     * If null, the image will pick a size that best preserves its intrinsic
     * aspect ratio.
     */
    var height: Double?
        get() = _height
        set(value) = run {
            if (value == _height)
                return
            _height = value
            markNeedsLayout()
        }

    /**
     * Specifies the image's scale.
     *
     * Used when determining the best display size for the image.
     */
    var scale: Double
        get() = _scale
        set(value) = run {
            if (value == _scale)
                return
            _scale = value
            markNeedsLayout()
        }

    var _colorFilter: ColorFilter? = null

    private fun _updateColorFilter() {
        if (_color == null)
            _colorFilter = null
        else
            _colorFilter = ColorFilter(_color!!, _colorBlendMode ?: BlendMode.srcIn)
    }

    /** If non-null, this color is blended with each image pixel using [colorBlendMode]. */
    var color: Color?
        get() = _color
        set(value) = run {
            if (value == _color)
                return
            _color = value
            _updateColorFilter()
            markNeedsLayout()
        }

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
    var colorBlendMode: BlendMode?
        get() = _colorBlendMode
        set(value) = run {
            if (value == _colorBlendMode)
                return
            _colorBlendMode = value
            _updateColorFilter()
            markNeedsLayout()
        }

    /**
     * How to inscribe the image into the space allocated during layout.
     *
     * The default varies based on the other fields. See the discussion at
     * [paintImage].
     */
    var fit: BoxFit?
        get() = _fit
        set(value) = run {
            if (value == _fit)
                return
            _fit = value
            markNeedsLayout()
        }

    /**
     * How to align the image within its bounds.
     *
     * If this is set to a text-direction-dependent value, [textDirection] must
     * not be null.
     */
    var alignment: AlignmentGeometry
        get() = _alignment
        set(value) = run {
            if (value == _alignment)
                return
            _alignment = value
            _markNeedResolution()
        }

    /** How to repeat this image if it doesn't fill its layout bounds. */
    var repeat: ImageRepeat
        get() = _repeat
        set(value) = run {
            if (value == _repeat)
                return
            _repeat = value
            markNeedsPaint()
        }

    /**
     * The center slice for a nine-patch image.
     *
     * The region of the image inside the center slice will be stretched both
     * horizontally and vertically to fit the image into its destination. The
     * region of the image above and below the center slice will be stretched
     * only horizontally and the region of the image to the left and right of
     * the center slice will be stretched only vertically.
     */
    var centerSlice: Rect?
        get() = _centerSlice
        set(value) = run {
            if (value == _centerSlice)
                return
            _centerSlice = value
            markNeedsPaint()
        }

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
     * If this is set to true, [textDirection] must not be null.
     */
    var matchTextDirection: Boolean
        get() = _matchTextDirection
        set(value) = run {
            if (value == _matchTextDirection)
                return
            _matchTextDirection = value
            _markNeedResolution()
        }

    /**
     * The text direction with which to resolve [alignment].
     *
     * This may be changed to null, but only after the [alignment] and
     * [matchTextDirection] properties have been changed to values that do not
     * depend on the direction.
     */
    var textDirection: TextDirection?
        get() = _textDirection
        set(value) = run {
            if (_textDirection == value)
                return
            _textDirection = value
            _markNeedResolution()
        }

    /**
     * Find a size for the render image within the given constraints.
     *
     *  - The dimensions of the RenderImage must fit within the constraints.
     *  - The aspect ratio of the RenderImage matches the intrinsic aspect
     *    ratio of the image.
     *  - The RenderImage's dimension are maximal subject to being smaller than
     *    the intrinsic size of the image.
     */
    fun _sizeForConstraints(constraints: BoxConstraints): Size {
        // Folds the given |width| and |height| into |constraints| so they can all
        // be treated uniformly.
        val cons = BoxConstraints.tightFor(
                width = _width,
                height = _height
        ).enforce(constraints)

        if (_image == null)
            return cons.smallest

        return cons.constrainSizeAndAttemptToPreserveAspectRatio(Size(
                _image!!.width.toDouble() / _scale,
                _image!!.height.toDouble() / _scale
        ))
    }

    override fun computeMinIntrinsicWidth(height: Double): Double {
        assert(height >= 0.0)
        if (_width == null && _height == null)
            return 0.0
        return _sizeForConstraints(BoxConstraints.tightForFinite(height = height)).width
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        assert(height >= 0.0)
        return _sizeForConstraints(BoxConstraints.tightForFinite(height = height)).width
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        assert(width >= 0.0)
        if (_width == null && _height == null)
            return 0.0
        return _sizeForConstraints(BoxConstraints.tightForFinite(width = width)).height
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        assert(width >= 0.0)
        return _sizeForConstraints(BoxConstraints.tightForFinite(width = width)).height
    }

    override fun hitTestSelf(position: Offset): Boolean = true

    override fun performLayout() {
        // TODO(Migration/Filip): Forced non null!
        size = _sizeForConstraints(constraints!!)
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        if (_image == null)
            return
        _resolve()
        assert(_resolvedAlignment != null)
        assert(_flipHorizontally != null)

        paintImage(
            canvas = context.canvas,
            rect = offset.and(size),
            image = _image!!,
            colorFilter = _colorFilter,
            fit = _fit,
            alignment = _resolvedAlignment!!,
            centerSlice = _centerSlice,
            repeat = _repeat,
            flipHorizontally = _flipHorizontally!!)
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
        properties.add(EnumProperty("textDirection", textDirection, defaultValue = null))
    }
}