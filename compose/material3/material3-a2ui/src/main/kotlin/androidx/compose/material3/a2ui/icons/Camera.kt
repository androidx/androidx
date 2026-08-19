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

package androidx.compose.material3.a2ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val Camera: ImageVector
    get() {
        if (_camera != null) {
            return _camera!!
        }
        _camera =
            ImageVector.Builder(
                    name = "camera",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(11.4f, 9f)
                        horizontalLineToRelative(8f)
                        quadTo(18.73f, 7.27f, 17.34f, 6.04f)
                        reflectiveQuadTo(14.15f, 4.3f)
                        lineTo(11.4f, 9f)
                        close()
                        moveTo(9.1f, 11f)
                        lineToRelative(4f, -6.9f)
                        quadTo(12.83f, 4.05f, 12.55f, 4.02f)
                        reflectiveQuadTo(12f, 4f)
                        quadTo(10.35f, 4f, 8.93f, 4.63f)
                        reflectiveQuadTo(6.4f, 6.3f)
                        lineTo(9.1f, 11f)
                        close()
                        moveTo(4.25f, 14f)
                        horizontalLineTo(9.7f)
                        lineTo(5.7f, 7.1f)
                        quadTo(4.9f, 8.13f, 4.45f, 9.36f)
                        reflectiveQuadTo(4f, 12f)
                        quadToRelative(0f, 0.52f, 0.06f, 1.01f)
                        reflectiveQuadTo(4.25f, 14f)
                        close()
                        moveToRelative(5.6f, 5.7f)
                        lineTo(12.55f, 15f)
                        horizontalLineTo(4.6f)
                        quadToRelative(0.67f, 1.73f, 2.06f, 2.96f)
                        reflectiveQuadTo(9.85f, 19.7f)
                        close()
                        moveTo(12f, 20f)
                        quadToRelative(1.65f, 0f, 3.08f, -0.63f)
                        reflectiveQuadTo(17.6f, 17.7f)
                        lineTo(14.9f, 13f)
                        lineToRelative(-4f, 6.9f)
                        quadToRelative(0.28f, 0.05f, 0.54f, 0.08f)
                        reflectiveQuadTo(12f, 20f)
                        close()
                        moveToRelative(6.3f, -3.1f)
                        quadToRelative(0.8f, -1.02f, 1.25f, -2.26f)
                        reflectiveQuadTo(20f, 12f)
                        quadToRelative(0f, -0.53f, -0.06f, -1.01f)
                        reflectiveQuadTo(19.75f, 10f)
                        horizontalLineTo(14.3f)
                        lineToRelative(4f, 6.9f)
                        close()
                        moveTo(12f, 12f)
                        close()
                        moveToRelative(0f, 10f)
                        quadTo(9.95f, 22f, 8.13f, 21.21f)
                        quadTo(6.3f, 20.43f, 4.94f, 19.06f)
                        quadTo(3.58f, 17.7f, 2.79f, 15.88f)
                        reflectiveQuadTo(2f, 12f)
                        quadTo(2f, 9.92f, 2.79f, 8.11f)
                        reflectiveQuadTo(4.94f, 4.94f)
                        reflectiveQuadTo(8.13f, 2.79f)
                        reflectiveQuadTo(12f, 2f)
                        quadToRelative(2.08f, 0f, 3.89f, 0.79f)
                        reflectiveQuadToRelative(3.17f, 2.15f)
                        reflectiveQuadToRelative(2.15f, 3.17f)
                        reflectiveQuadTo(22f, 12f)
                        quadToRelative(0f, 2.05f, -0.79f, 3.88f)
                        reflectiveQuadToRelative(-2.15f, 3.19f)
                        reflectiveQuadToRelative(-3.17f, 2.15f)
                        reflectiveQuadTo(12f, 22f)
                        close()
                    }
                }
                .build()
        return _camera!!
    }

private var _camera: ImageVector? = null
