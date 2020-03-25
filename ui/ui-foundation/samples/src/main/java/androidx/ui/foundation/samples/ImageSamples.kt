/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.foundation.Image
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.painter.Painter
import androidx.ui.layout.preferredSize
import androidx.ui.res.loadVectorResource
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.px

@Sampled
@Composable
fun ImageSample() {
    val imageAsset = createTestImage()
    // Lays out and draws an image sized to the dimensions of the ImageAsset
    Image(asset = imageAsset)
}

@Sampled
@Composable
fun ImageVectorAssetSample() {
    val vectorAsset = loadVectorResource(R.drawable.ic_sample_vector)
    vectorAsset.resource.resource?.let {
        Image(
            asset = it,
            modifier = Modifier.preferredSize(200.dp, 200.dp),
            scaleFit = ScaleFit.FillMinDimension,
            colorFilter = ColorFilter.tint(Color.Cyan)
        )
    }
}

@Sampled
@Composable
fun ImagePainterSample() {
    val customPainter = remember {
        object : Painter() {

            val paint = Paint().apply { this.color = Color.Cyan }

            override val intrinsicSize: PxSize
                get() = PxSize(100.px, 100.px)

            override fun onDraw(canvas: Canvas, bounds: PxSize) {
                canvas.drawRect(
                    Rect.fromLTWH(0.0f, 0.0f, bounds.width.value, bounds.height.value),
                    paint
                )
            }
        }
    }

    Image(painter = customPainter, modifier = Modifier.preferredSize(100.dp, 100.dp))
}

/**
 * Helper method to create an ImageAsset with some content in it
 */
private fun createTestImage(): ImageAsset {
    val imageAsset = ImageAsset(100, 100)
    Canvas(imageAsset).drawCircle(
        Offset(50.0f, 50.0f), 50.0f,
        Paint().apply { this.color = Color.Cyan }
    )
    return imageAsset
}