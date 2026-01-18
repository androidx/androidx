/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.demos

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.samples.placeholderImagePainter

/** Icons taken from material-icons-core */
internal object Icons {
    val FavoriteIcon: ImageVector =
        ImageVector.Builder(
                name = "Favorite",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
            .apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(12.0f, 21.35f)
                    lineToRelative(-1.45f, -1.32f)
                    curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
                    curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
                    curveToRelative(1.74f, 0.0f, 3.41f, 0.81f, 4.5f, 2.09f)
                    curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
                    curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
                    curveToRelative(0.0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                    lineTo(12.0f, 21.35f)
                    close()
                }
            }
            .build()

    val SendIcon: ImageVector =
        ImageVector.Builder(
                name = "Send",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
            .apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(3f, 20f)
                    lineTo(3f, 4f)
                    lineTo(22f, 12f)
                    lineTo(3f, 20f)
                    close()
                    moveTo(5f, 17f)
                    lineTo(16.85f, 12f)
                    lineTo(5f, 7f)
                    lineTo(5f, 10.5f)
                    lineTo(11f, 12f)
                    lineTo(5f, 13.5f)
                    lineTo(5f, 17f)
                    close()
                }
            }
            .build()

    val CalendarIcon: ImageVector =
        ImageVector.Builder(
                name = "Calendar",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
            .apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(9.0f, 16.5f)
                    quadTo(7.95f, 16.5f, 7.225f, 15.775f)
                    quadTo(6.5f, 15.05f, 6.5f, 14.0f)
                    quadTo(6.5f, 12.95f, 7.225f, 12.225f)
                    quadTo(7.95f, 11.5f, 9.0f, 11.5f)
                    quadTo(10.05f, 11.5f, 10.775f, 12.225f)
                    quadTo(11.5f, 12.95f, 11.5f, 14.0f)
                    quadTo(11.5f, 15.05f, 10.775f, 15.775f)
                    quadTo(10.05f, 16.5f, 9.0f, 16.5f)
                    close()
                    moveTo(5.0f, 22.0f)
                    quadTo(4.175f, 22.0f, 3.5875f, 21.4125f)
                    quadTo(3.0f, 20.825f, 3.0f, 20.0f)
                    lineTo(3.0f, 6.0f)
                    quadTo(3.0f, 5.175f, 3.5875f, 4.5875f)
                    quadTo(4.175f, 4.0f, 5.0f, 4.0f)
                    lineTo(6.0f, 4.0f)
                    lineTo(6.0f, 2.0f)
                    lineTo(8.0f, 2.0f)
                    lineTo(8.0f, 4.0f)
                    lineTo(16.0f, 4.0f)
                    lineTo(16.0f, 2.0f)
                    lineTo(18.0f, 2.0f)
                    lineTo(18.0f, 4.0f)
                    lineTo(19.0f, 4.0f)
                    quadTo(19.825f, 4.0f, 20.4125f, 4.5875f)
                    quadTo(21.0f, 5.175f, 21.0f, 6.0f)
                    lineTo(21.0f, 20.0f)
                    quadTo(21.0f, 20.825f, 20.4125f, 21.4125f)
                    quadTo(19.825f, 22.0f, 19.0f, 22.0f)
                    lineTo(5.0f, 22.0f)
                    close()
                    moveTo(5.0f, 20.0f)
                    lineTo(19.0f, 20.0f)
                    lineTo(19.0f, 10.0f)
                    lineTo(5.0f, 10.0f)
                    close()
                    moveTo(5.0f, 8.0f)
                    lineTo(19.0f, 8.0f)
                    lineTo(19.0f, 6.0f)
                    lineTo(5.0f, 6.0f)
                    close()
                }
            }
            .build()
}

/**
 * Placeholder image with a large intrinsic size, to simulate a real life use case of loading a
 * bitmap
 */
internal val SampleImage = placeholderImagePainter(Size(1000f, 1000f))
