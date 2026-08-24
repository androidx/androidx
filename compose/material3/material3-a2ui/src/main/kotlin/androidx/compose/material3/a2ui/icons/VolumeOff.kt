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
internal val VolumeOff: ImageVector
    get() {
        if (_volume_off != null) {
            return _volume_off!!
        }
        _volume_off =
            ImageVector.Builder(
                    name = "volume_off",
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
                        moveTo(19.8f, 22.6f)
                        lineTo(16.78f, 19.58f)
                        quadToRelative(-0.63f, 0.4f, -1.33f, 0.69f)
                        reflectiveQuadTo(14f, 20.73f)
                        verticalLineTo(18.68f)
                        quadToRelative(0.35f, -0.13f, 0.69f, -0.25f)
                        quadToRelative(0.34f, -0.13f, 0.64f, -0.3f)
                        lineTo(12f, 14.8f)
                        verticalLineTo(20f)
                        lineTo(7f, 15f)
                        horizontalLineTo(3f)
                        verticalLineTo(9f)
                        horizontalLineTo(6.2f)
                        lineTo(1.4f, 4.2f)
                        lineTo(2.8f, 2.8f)
                        lineTo(21.2f, 21.2f)
                        lineToRelative(-1.4f, 1.4f)
                        close()
                        moveTo(19.6f, 16.8f)
                        lineTo(18.15f, 15.35f)
                        quadToRelative(0.42f, -0.78f, 0.64f, -1.63f)
                        reflectiveQuadTo(19f, 11.98f)
                        quadTo(19f, 9.63f, 17.63f, 7.77f)
                        quadTo(16.25f, 5.93f, 14f, 5.27f)
                        verticalLineTo(3.22f)
                        quadToRelative(3.1f, 0.7f, 5.05f, 3.14f)
                        reflectiveQuadTo(21f, 11.98f)
                        quadToRelative(0f, 1.32f, -0.36f, 2.55f)
                        reflectiveQuadTo(19.6f, 16.8f)
                        close()
                        moveTo(16.25f, 13.45f)
                        lineTo(14f, 11.2f)
                        verticalLineTo(7.95f)
                        quadTo(15.18f, 8.5f, 15.84f, 9.6f)
                        reflectiveQuadTo(16.5f, 12f)
                        quadToRelative(0f, 0.38f, -0.06f, 0.74f)
                        reflectiveQuadToRelative(-0.19f, 0.71f)
                        close()
                        moveTo(12f, 9.2f)
                        lineTo(9.4f, 6.6f)
                        lineTo(12f, 4f)
                        verticalLineTo(9.2f)
                        close()
                        moveToRelative(-2f, 5.95f)
                        verticalLineTo(12.8f)
                        lineTo(8.2f, 11f)
                        horizontalLineTo(5f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(7.85f)
                        lineTo(10f, 15.15f)
                        close()
                        moveTo(9.1f, 11.9f)
                        close()
                    }
                }
                .build()
        return _volume_off!!
    }

private var _volume_off: ImageVector? = null
