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

package androidx.compose.material3.integration.a2ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val GalleryIcon: ImageVector
    get() {
        if (_gallery != null) {
            return _gallery!!
        }
        _gallery =
            ImageVector.Builder(
                    name = "gallery",
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
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(11f, 21f)
                        horizontalLineTo(5f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineToRelative(6f)
                        verticalLineTo(21f)
                        close()
                        moveTo(9f, 19f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(19f)
                        horizontalLineTo(9f)
                        close()
                        moveToRelative(4f, -8f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(6f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(21f, 5f)
                        verticalLineToRelative(6f)
                        horizontalLineTo(13f)
                        close()
                        moveTo(15f, 9f)
                        horizontalLineToRelative(4f)
                        verticalLineTo(5f)
                        horizontalLineTo(15f)
                        verticalLineTo(9f)
                        close()
                        moveTo(13f, 21f)
                        verticalLineTo(13f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(6f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        horizontalLineTo(13f)
                        close()
                        moveToRelative(2f, -2f)
                        horizontalLineToRelative(4f)
                        verticalLineTo(15f)
                        horizontalLineTo(15f)
                        verticalLineToRelative(4f)
                        close()
                    }
                }
                .build()
        return _gallery!!
    }

private var _gallery: ImageVector? = null
