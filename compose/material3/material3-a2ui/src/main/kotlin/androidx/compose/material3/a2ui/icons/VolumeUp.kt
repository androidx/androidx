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
internal val VolumeUp: ImageVector
    get() {
        if (_volume_up != null) {
            return _volume_up!!
        }
        _volume_up =
            ImageVector.Builder(
                    name = "volume_up",
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
                        moveTo(14f, 20.73f)
                        verticalLineTo(18.68f)
                        quadToRelative(2.25f, -0.65f, 3.63f, -2.5f)
                        reflectiveQuadTo(19f, 11.98f)
                        reflectiveQuadTo(17.63f, 7.77f)
                        quadTo(16.25f, 5.93f, 14f, 5.27f)
                        verticalLineTo(3.22f)
                        quadToRelative(3.1f, 0.7f, 5.05f, 3.14f)
                        reflectiveQuadTo(21f, 11.98f)
                        reflectiveQuadToRelative(-1.95f, 5.61f)
                        reflectiveQuadTo(14f, 20.73f)
                        close()
                        moveTo(3f, 15f)
                        verticalLineTo(9f)
                        horizontalLineTo(7f)
                        lineTo(12f, 4f)
                        verticalLineTo(20f)
                        lineTo(7f, 15f)
                        horizontalLineTo(3f)
                        close()
                        moveToRelative(11f, 1f)
                        verticalLineTo(7.95f)
                        quadTo(15.18f, 8.5f, 15.84f, 9.6f)
                        reflectiveQuadTo(16.5f, 12f)
                        quadToRelative(0f, 1.27f, -0.66f, 2.36f)
                        reflectiveQuadTo(14f, 16f)
                        close()
                        moveTo(10f, 8.85f)
                        lineTo(7.85f, 11f)
                        horizontalLineTo(5f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(7.85f)
                        lineTo(10f, 15.15f)
                        verticalLineTo(8.85f)
                        close()
                        moveTo(7.5f, 12f)
                        close()
                    }
                }
                .build()
        return _volume_up!!
    }

private var _volume_up: ImageVector? = null
