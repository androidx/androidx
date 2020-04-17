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
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.paint
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.ImageAsset
import androidx.ui.core.ContentScale
import androidx.ui.graphics.isUnset
import androidx.ui.graphics.painter.ColorPainter
import androidx.ui.graphics.painter.ImagePainter
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.VectorPainter
import androidx.ui.layout.preferredSize

/**
 * A composable that lays out and draws a given [ImageAsset]. This will attempt to
 * size the composable according to the [ImageAsset]'s given width and height. However, an
 * optional [Modifier] parameter can be provided to adjust sizing or draw additional content (ex.
 * background). Any unspecified dimension will leverage the [ImageAsset]'s size as a minimum
 * constraint.
 *
 * The following sample shows basic usage of an Image composable to position and draw an
 * [ImageAsset] on screen
 * @sample androidx.ui.foundation.samples.ImageSample
 *
 * For use cases that require drawing a rectangular subset of the [ImageAsset] consumers can use
 * overload that consumes a [Painter] parameter shown in this sample
 * @sample androidx.ui.foundation.samples.ImagePainterSubsectionSample
 *
 * @param asset The [ImageAsset] to draw.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [ImageAsset] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [ImageAsset].
 * @param alpha Optional opacity to be applied to the [ImageAsset] when it is rendered onscreen
 * @param colorFilter Optional ColorFilter to apply for the [ImageAsset] when it is rendered
 * onscreen
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Image(
    asset: ImageAsset,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val imagePainter = remember(asset) { ImagePainter(asset) }
    Image(
        painter = imagePainter,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}

/**
 * A composable that lays out and draws a given [VectorAsset]. This will attempt to
 * size the composable according to the [VectorAsset]'s given width and height. However, an
 * optional [Modifier] parameter can be provided to adjust sizing or draw additional content (ex.
 * background). Any unspecified dimension will leverage the [VectorAsset]'s size as a minimum
 * constraint.
 *
 * @sample androidx.ui.foundation.samples.ImageVectorAssetSample
 *
 * @param asset The [VectorAsset] to draw.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [VectorAsset] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [VectorAsset].
 * @param alpha Optional opacity to be applied to the [VectorAsset] when it is rendered onscreen
 * @param colorFilter Optional ColorFilter to apply for the [VectorAsset] when it is rendered
 * onscreen
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Image(
    asset: VectorAsset,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = Image(
    painter = VectorPainter(asset),
    modifier = modifier,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

/**
 * Creates a composable that lays out and draws a given [Painter]. This will attempt to size
 * the composable according to the [Painter]'s intrinsic size. However, an optional [Modifier]
 * parameter can be provided to adjust sizing or draw additional content (ex. background)
 *
 * **NOTE** a Painter might not have an intrinsic size, so if no LayoutModifier is provided
 * as part of the Modifier chain this might size the [Image] composable to a width and height
 * of zero and will not draw any content. This can happen for Painter implementations that
 * always attempt to fill the bounds like [ColorPainter]
 *
 * @sample androidx.ui.foundation.samples.ImagePainterSample
 *
 * @param painter to draw
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [Painter] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [Painter].
 * @param alpha Optional opacity to be applied to the [Painter] when it is rendered onscreen
 * the default renders the [Painter] completely opaque
 * @param colorFilter Optional colorFilter to apply for the [Painter] when it is rendered onscreen
 */
@Composable
fun Image(
    painter: Painter,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    Box(
        modifier.clipToBounds().paint(
            painter,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter
        )
    )
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
    tint: Color = Color.Unset
) {
    with(DensityAmbient.current) {
        Box(
            Modifier.preferredSize(image.width.toDp(), image.height.toDp())
                .clipToBounds()
                .paint(
                    ImagePainter(image),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (tint.isUnset) null else {
                        ColorFilter(tint, BlendMode.srcIn)
                    }
                )
        )
    }
}