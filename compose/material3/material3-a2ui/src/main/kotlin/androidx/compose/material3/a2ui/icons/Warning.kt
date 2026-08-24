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
internal val Warning: ImageVector
    get() {
        if (_warning != null) {
            return _warning!!
        }
        _warning =
            ImageVector.Builder(
                    name = "warning",
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
                        moveTo(1f, 21f)
                        lineTo(12f, 2f)
                        lineTo(23f, 21f)
                        horizontalLineTo(1f)
                        close()
                        moveTo(4.45f, 19f)
                        horizontalLineToRelative(15.1f)
                        lineTo(12f, 6f)
                        lineTo(4.45f, 19f)
                        close()
                        moveToRelative(8.26f, -1.29f)
                        quadTo(13f, 17.43f, 13f, 17f)
                        reflectiveQuadTo(12.71f, 16.29f)
                        reflectiveQuadTo(12f, 16f)
                        reflectiveQuadToRelative(-0.71f, 0.29f)
                        reflectiveQuadTo(11f, 17f)
                        reflectiveQuadToRelative(0.29f, 0.71f)
                        reflectiveQuadTo(12f, 18f)
                        reflectiveQuadToRelative(0.71f, -0.29f)
                        close()
                        moveTo(11f, 15f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(10f)
                        horizontalLineTo(11f)
                        verticalLineToRelative(5f)
                        close()
                        moveToRelative(1f, -2.5f)
                        close()
                    }
                }
                .build()
        return _warning!!
    }

private var _warning: ImageVector? = null
