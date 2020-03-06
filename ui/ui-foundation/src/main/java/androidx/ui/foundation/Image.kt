/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DrawModifier
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutDirectionAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.asModifier
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.painter.ImagePainter
import androidx.ui.graphics.painter.Painter
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.toRect

/**
 * A composable that lays out and draws a given [ImageAsset]. This will attempt to
 * size the composable according to the [ImageAsset]'s given width and height. However, an
 * optional [Modifier] parameter can be provided to adjust sizing or draw additional content (ex.
 * background). Any unspecified dimension will leverage the [ImageAsset]'s size as a minimum
 * constraint.
 *
 * @sample androidx.ui.foundation.samples.ImageSample
 * @sample androidx.ui.foundation.samples.ImageSamplePainterMinSize
 *
 * @param image The [ImageAsset] to draw.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [ImageAsset] in the given
 * bounds defined by the width and height.
 * @param scaleFit Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [ImageAsset].
 * @param alpha Optional opacity to be applied to the [ImageAsset] when it is rendered onscreen
 * @param colorFilter Optional ColorFilter to apply for the [ImageAsset] when it is rendered
 * onscreen
 * @param layoutDirection Optional parameter indicating the content should be mirrored for right to
 * left languages.
 */
@Composable
fun Image(
    image: ImageAsset,
    modifier: Modifier = Modifier.None,
    alignment: Alignment = Alignment.Center,
    scaleFit: ScaleFit = ScaleFit.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    layoutDirection: LayoutDirection = LayoutDirectionAmbient.current
) {
    val imagePainter = remember(image) { ImagePainter(image) }
    // Min width/height are intentionally not provided in this call as they are consumed
    // from the ImagePainter directly
    Image(
        painter = imagePainter,
        modifier = modifier,
        alignment = alignment,
        scaleFit = scaleFit,
        alpha = alpha,
        colorFilter = colorFilter,
        layoutDirection = layoutDirection
    )
}

/**
 * Creates a composable that lays out and draws a given [Painter]. This will attempt to size
 * the composable according to the [Painter]'s intrinsic size. However, an optional [Modifier]
 * parameter can be provided to adjust sizing or draw additional content (ex. background)
 *
 * @param painter to draw
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param minWidth Minimum width used to size this Image composable. Useful for situations where
 * the given Painter has no intrinsic size (ex. ColorPainter)
 * @param minHeight Minimum height used to size this Image composable. Useful for situations where
 * the given Painter has no intrinsic size (ex. ColorPainter)
 * @param alignment Optional alignment parameter used to place the [Painter] in the given
 * bounds defined by the width and height.
 * @param scaleFit Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [Painter].
 * @param alpha Optional opacity to be applied to the [Painter] when it is rendered onscreen
 * the default renders the [Painter] completely opaque
 * @param colorFilter Optional colorFilter to apply for the [Painter] when it is rendered onscreen
 * @param layoutDirection Optional parameter indicating the content should be mirrored for right to
 * left languages. The default value is extracted from [LayoutDirectionAmbient]
 */
@Composable
fun Image(
    painter: Painter,
    modifier: Modifier = Modifier.None,
    minWidth: Dp = Dp.Unspecified,
    minHeight: Dp = Dp.Unspecified,
    alignment: Alignment = Alignment.Center,
    scaleFit: ScaleFit = ScaleFit.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    layoutDirection: LayoutDirection = LayoutDirectionAmbient.current
) {
    val painterModifier = painter.asModifier(
        alignment = alignment,
        scaleFit = scaleFit,
        alpha = alpha,
        colorFilter = colorFilter,
        rtl = layoutDirection == LayoutDirection.Rtl
    )
    // Enforce a minimum size if specified. This is used if the underlying painter does not
    // have a minimum size, or it is smaller than the given minimum size
    val hasSpecifiedMinWidth = minWidth != Dp.Unspecified
    val hasSpecifiedMinHeight = minHeight != Dp.Unspecified
    val hasSpecifiedMinSize = hasSpecifiedMinWidth && hasSpecifiedMinHeight
    val minSizeModifier = when {
        hasSpecifiedMinSize -> LayoutSize.Min(minWidth, minHeight)
        hasSpecifiedMinWidth -> LayoutSize.Min(minWidth, 0.dp)
        hasSpecifiedMinHeight -> LayoutSize.Min(0.dp, minHeight)
        else -> Modifier.None
    }

    Box(modifier + minSizeModifier + ClipModifier + painterModifier)
}

/**
 * Fits an image into the container with sizes equals to the image size, while maintaining
 * the image aspect ratio.
 * The image will be clipped if the aspect ratios of the image and the parent don't match.
 *
 * This component has the same behavior as ImageView.ScaleType.CENTER_CROP currently.
 *
 * @param image The image to draw.
 * @param tint The tint color to apply for the image.
 *
 * @deprecated use [Image] instead
 */
// TODO(Andrey) Temporary. Should be replaced with our proper Image component when it available
// TODO(Andrey, Matvei, Nader): Support other scale types b/141741141
@Deprecated("SimpleImage has limited functionality and was a placeholder API until" +
        "the preferred API for laying out and drawing an ImageAsset was finalized.",
    ReplaceWith("Image(image)")
)
@Composable
fun SimpleImage(
    image: ImageAsset,
    tint: Color? = null
) {
    with(DensityAmbient.current) {
        val imageModifier = ImagePainter(image).asModifier(
            scaleFit = ScaleFit.FillMaxDimension,
            colorFilter = tint?.let { ColorFilter(it, BlendMode.srcIn) }
        )
        Box(LayoutSize(image.width.toDp(), image.height.toDp()) + ClipModifier + imageModifier)
    }
}

// TODO(mount, malkov) : remove when RepaintBoundary is a modifier: b/149982905
// This is class and not val because if b/149985596
private object ClipModifier : DrawModifier {
    override fun draw(density: Density, drawContent: () -> Unit, canvas: Canvas, size: PxSize) {
        canvas.save()
        canvas.clipRect(size.toRect())
        drawContent()
        canvas.restore()
    }
}