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
internal val Home: ImageVector
    get() {
        if (_home != null) {
            return _home!!
        }
        _home =
            ImageVector.Builder(
                    name = "home",
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
                        moveTo(6f, 19f)
                        horizontalLineTo(9f)
                        verticalLineTo(13f)
                        horizontalLineToRelative(6f)
                        verticalLineToRelative(6f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(10f)
                        lineTo(12f, 5.5f)
                        lineTo(6f, 10f)
                        verticalLineToRelative(9f)
                        close()
                        moveTo(4f, 21f)
                        verticalLineTo(9f)
                        lineTo(12f, 3f)
                        lineToRelative(8f, 6f)
                        verticalLineTo(21f)
                        horizontalLineTo(13f)
                        verticalLineTo(15f)
                        horizontalLineTo(11f)
                        verticalLineToRelative(6f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(8f, -8.75f)
                        close()
                    }
                }
                .build()
        return _home!!
    }

private var _home: ImageVector? = null
