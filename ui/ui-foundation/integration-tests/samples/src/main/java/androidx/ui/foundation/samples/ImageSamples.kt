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
import androidx.ui.foundation.Image
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.painter.ColorPainter
import androidx.ui.unit.dp

@Sampled
@Composable
fun ImageSample() {
    val imageAsset = createTestImage()
    // Lays out and draws an image sized to the dimensions of the ImageAsset
    Image(image = imageAsset)
}

@Sampled
@Composable
fun ImageSamplePainterMinSize() {
    // Lays out 20 dp x 20 dp composable and draws the area with the given color
    Image(
        painter = ColorPainter(Color.Red),
        minWidth = 20.dp,
        minHeight = 20.dp
    )
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