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

package androidx.ui.painting

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.painting.alignment.Alignment
import kotlin.coroutines.experimental.buildIterator
import kotlin.math.ceil
import kotlin.math.floor

// / Paints an image into the given rectangle on the canvas.
// /
// / The arguments have the following meanings:
// /
// /  * `canvas`: The canvas onto which the image will be painted.
// /
// /  * `rect`: The region of the canvas into which the image will be painted.
// /    The image might not fill the entire rectangle (e.g., depending on the
// /    `fit`). If `rect` is empty, nothing is painted.
// /
// /  * `image`: The image to paint onto the canvas.
// /
// /  * `colorFilter`: If non-null, the color filter to apply when painting the
// /    image.
// /
// /  * `fit`: How the image should be inscribed into `rect`. If null, the
// /    default behavior depends on `centerSlice`. If `centerSlice` is also null,
// /    the default behavior is [BoxFit.scaleDown]. If `centerSlice` is
// /    non-null, the default behavior is [BoxFit.fill]. See [BoxFit] for
// /    details.
// /
// /  * `alignment`: How the destination rectangle defined by applying `fit` is
// /    aligned within `rect`. For example, if `fit` is [BoxFit.contain] and
// /    `alignment` is [Alignment.bottomRight], the image will be as large
// /    as possible within `rect` and placed with its bottom right corner at the
// /    bottom right corner of `rect`. Defaults to [Alignment.center].
// /
// /  * `centerSlice`: The image is drawn in nine portions described by splitting
// /    the image by drawing two horizontal lines and two vertical lines, where
// /    `centerSlice` describes the rectangle formed by the four points where
// /    these four lines intersect each other. (This forms a 3-by-3 grid
// /    of regions, the center region being described by `centerSlice`.)
// /    The four regions in the corners are drawn, without scaling, in the four
// /    corners of the destination rectangle defined by applying `fit`. The
// /    remaining five regions are drawn by stretching them to fit such that they
// /    exactly cover the destination rectangle while maintaining their relative
// /    positions.
// /
// /  * `repeat`: If the image does not fill `rect`, whether and how the image
// /    should be repeated to fill `rect`. By default, the image is not repeated.
// /    See [ImageRepeat] for details.
// /
// /  * `flipHorizontally`: Whether to flip the image horizontally. This is
// /    occasionally used with images in right-to-left environments, for images
// /    that were designed for left-to-right locales (or vice versa). Be careful,
// /    when using this, to not flip images with integral shadows, text, or other
// /    effects that will look incorrect when flipped.
// /
// / The `canvas`, `rect`, `image`, `alignment`, `repeat`, and `flipHorizontally`
// / arguments must not be null.
// /
// / See also:
// /
// /  * [paintBorder], which paints a border around a rectangle on a canvas.
// /  * [DecorationImage], which holds a configuration for calling this function.
// /  * [BoxDecoration], which uses this function to paint a [DecorationImage].
fun paintImage(
    canvas: Canvas,
    rect: Rect,
    image: Image,
    colorFilter: ColorFilter? = null,
    fit: BoxFit? = null,
    alignment: Alignment = Alignment.center,
    centerSlice: Rect? = null,
    repeat: ImageRepeat = ImageRepeat.noRepeat,
    flipHorizontally: Boolean = false
) {
    assert(canvas != null)
    assert(image != null)
    assert(alignment != null)
    assert(repeat != null)
    assert(flipHorizontally != null)
    if (rect.isEmpty())
        return
    var outputSize = rect.getSize()
    var inputSize = Size(image.width.toDouble(), image.height.toDouble())
    val sliceBorder: Offset?
    if (centerSlice != null) {
        sliceBorder = Offset(
                centerSlice.left + inputSize.width - centerSlice.right,
        centerSlice.top + inputSize.height - centerSlice.bottom
        )
        outputSize -= sliceBorder
        inputSize -= sliceBorder
    } else {
        sliceBorder = null
    }
    val resolvedFit = fit ?: if (centerSlice == null) BoxFit.scaleDown else BoxFit.fill
    assert(centerSlice == null || (resolvedFit != BoxFit.none && resolvedFit != BoxFit.cover))
    val fittedSizes: FittedSizes = applyBoxFit(resolvedFit, inputSize, outputSize)
    val sourceSize: Size = fittedSizes.source
    var destinationSize: Size = fittedSizes.destination
    if (sliceBorder != null) {
        outputSize += sliceBorder
        destinationSize += sliceBorder
        // We don't have the ability to draw a subset of the image at the same time
        // as we apply a nine-patch stretch.
        assert(sourceSize == inputSize, { "centerSlice was used with a BoxFit that does" +
                " not guarantee that the image is fully visible." })
    }
    val resolvedRepeat: ImageRepeat
    if (repeat != ImageRepeat.noRepeat && destinationSize == outputSize) {
        // There's no need to repeat the image because we're exactly filling the
        // output rect with the image.
        resolvedRepeat = ImageRepeat.noRepeat
    } else {
        resolvedRepeat = repeat
    }
    val paint = Paint().apply { isAntiAlias = false }
    if (colorFilter != null)
        paint.colorFilter = colorFilter
    if (sourceSize != destinationSize) {
        // Use the "low" quality setting to scale the image, which corresponds to
        // bilinear interpolation, rather than the default "none" which corresponds
        // to nearest-neighbor.
        paint.filterQuality = FilterQuality.low
    }
    val halfWidthDelta = (outputSize.width - destinationSize.width) / 2.0
    val halfHeightDelta = (outputSize.height - destinationSize.height) / 2.0
    val dx = halfWidthDelta + (if (flipHorizontally) -alignment.x else alignment.x) * halfWidthDelta
    val dy = halfHeightDelta + alignment.y * halfHeightDelta
    val destinationPosition = rect.getTopLeft().translate(dx, dy)
    val destinationRect = destinationPosition.and(destinationSize)
    val needSave = resolvedRepeat != ImageRepeat.noRepeat || flipHorizontally
    if (needSave)
        canvas.save()
    if (resolvedRepeat != ImageRepeat.noRepeat)
        canvas.clipRect(rect)
    if (flipHorizontally) {
        val dx = -(rect.left + rect.width / 2.0)
        canvas.translate(-dx, 0.0)
        canvas.scale(-1.0, 1.0)
        canvas.translate(dx, 0.0)
    }
    if (centerSlice == null) {
        val sourceRect = alignment.inscribe(fittedSizes.source, Offset.zero.and(inputSize))
        for (tileRect in _generateImageTileRects(rect, destinationRect, resolvedRepeat)) {
            canvas.drawImageRect(image, sourceRect, tileRect, paint)
        }
    } else {
        for (tileRect in _generateImageTileRects(rect, destinationRect, resolvedRepeat)) {
            canvas.drawImageNine(image, centerSlice, tileRect, paint)
        }
    }
    if (needSave) {
        canvas.restore()
    }
}

private fun _generateImageTileRects(
    outputRect: Rect,
    fundamentalRect: Rect,
    repeat: ImageRepeat
): Iterator<Rect> = buildIterator {
    if (repeat == ImageRepeat.noRepeat) {
        yield(fundamentalRect)
    } else {
        var startX = 0
        var startY = 0
        var stopX = 0
        var stopY = 0
        var strideX = fundamentalRect.width
        var strideY = fundamentalRect.height

        if (repeat == ImageRepeat.repeat || repeat == ImageRepeat.repeatX) {
            startX = floor((outputRect.left - fundamentalRect.left) / strideX).toInt()
            stopX = ceil((outputRect.right - fundamentalRect.right) / strideX).toInt()
        }

        if (repeat == ImageRepeat.repeat || repeat == ImageRepeat.repeatY) {
            startY = floor((outputRect.top - fundamentalRect.top) / strideY).toInt()
            stopY = ceil((outputRect.bottom - fundamentalRect.bottom) / strideY).toInt()
        }

        for (i in startX..stopX) {
            for (j in startY..stopY) {
                yield(fundamentalRect.shift(Offset(i * strideX, j * strideY)))
            }
        }
    }
}