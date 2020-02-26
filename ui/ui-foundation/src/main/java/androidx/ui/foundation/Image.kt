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
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DrawModifier
import androidx.ui.core.asModifier
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.Image
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.painter.ImagePainter
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize
import androidx.ui.unit.toRect

/**
 * Fits an image into the container with sizes equals to the image size, while maintaining
 * the image aspect ratio.
 * The image will be clipped if the aspect ratios of the image and the parent don't match.
 *
 * This component has the same behavior as ImageView.ScaleType.CENTER_CROP currently.
 *
 * @param image The image to draw.
 * @param tint The tint color to apply for the image.
 */
// TODO(Andrey) Temporary. Should be replaced with our proper Image component when it available
// TODO(Andrey, Matvei, Nader): Support other scale types b/141741141
@Composable
fun SimpleImage(
    image: Image,
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