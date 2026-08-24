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
internal val Folder: ImageVector
    get() {
        if (_folder != null) {
            return _folder!!
        }
        _folder =
            ImageVector.Builder(
                    name = "folder",
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
                        moveTo(4f, 20f)
                        quadTo(3.18f, 20f, 2.59f, 19.41f)
                        reflectiveQuadTo(2f, 18f)
                        verticalLineTo(6f)
                        quadTo(2f, 5.18f, 2.59f, 4.59f)
                        reflectiveQuadTo(4f, 4f)
                        horizontalLineToRelative(6f)
                        lineToRelative(2f, 2f)
                        horizontalLineToRelative(8f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        quadTo(22f, 7.18f, 22f, 8f)
                        verticalLineTo(18f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(20f, 20f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(4f, 18f)
                        horizontalLineTo(20f)
                        verticalLineTo(8f)
                        horizontalLineTo(11.18f)
                        lineToRelative(-2f, -2f)
                        horizontalLineTo(4f)
                        verticalLineTo(18f)
                        close()
                        moveToRelative(0f, 0f)
                        verticalLineTo(6f)
                        verticalLineTo(8f)
                        verticalLineTo(18f)
                        close()
                    }
                }
                .build()
        return _folder!!
    }

private var _folder: ImageVector? = null
