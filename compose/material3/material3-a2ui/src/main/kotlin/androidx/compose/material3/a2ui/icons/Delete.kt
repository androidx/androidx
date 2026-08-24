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
internal val Delete: ImageVector
    get() {
        if (_delete != null) {
            return _delete!!
        }
        _delete =
            ImageVector.Builder(
                    name = "delete",
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
                        moveTo(7f, 21f)
                        quadTo(6.18f, 21f, 5.59f, 20.41f)
                        reflectiveQuadTo(5f, 19f)
                        verticalLineTo(6f)
                        horizontalLineTo(4f)
                        verticalLineTo(4f)
                        horizontalLineTo(9f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(6f)
                        verticalLineTo(4f)
                        horizontalLineToRelative(5f)
                        verticalLineTo(6f)
                        horizontalLineTo(19f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(17f, 21f)
                        horizontalLineTo(7f)
                        close()
                        moveTo(17f, 6f)
                        horizontalLineTo(7f)
                        verticalLineTo(19f)
                        horizontalLineTo(17f)
                        verticalLineTo(6f)
                        close()
                        moveTo(9f, 17f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(8f)
                        horizontalLineTo(9f)
                        verticalLineToRelative(9f)
                        close()
                        moveToRelative(4f, 0f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(8f)
                        horizontalLineTo(13f)
                        verticalLineToRelative(9f)
                        close()
                        moveTo(7f, 6f)
                        verticalLineTo(19f)
                        verticalLineTo(6f)
                        close()
                    }
                }
                .build()
        return _delete!!
    }

private var _delete: ImageVector? = null
