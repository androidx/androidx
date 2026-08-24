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
internal val FastForward: ImageVector
    get() {
        if (_fast_forward != null) {
            return _fast_forward!!
        }
        _fast_forward =
            ImageVector.Builder(
                    name = "fast_forward",
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
                        moveTo(2.5f, 18f)
                        verticalLineTo(6f)
                        lineToRelative(9f, 6f)
                        lineToRelative(-9f, 6f)
                        close()
                        moveToRelative(10f, 0f)
                        verticalLineTo(6f)
                        lineToRelative(9f, 6f)
                        lineToRelative(-9f, 6f)
                        close()
                        moveToRelative(-8f, -6f)
                        close()
                        moveToRelative(10f, 0f)
                        close()
                        moveToRelative(-10f, 2.25f)
                        lineTo(7.9f, 12f)
                        lineTo(4.5f, 9.75f)
                        verticalLineToRelative(4.5f)
                        close()
                        moveToRelative(10f, 0f)
                        lineTo(17.9f, 12f)
                        lineTo(14.5f, 9.75f)
                        verticalLineToRelative(4.5f)
                        close()
                    }
                }
                .build()
        return _fast_forward!!
    }

private var _fast_forward: ImageVector? = null
