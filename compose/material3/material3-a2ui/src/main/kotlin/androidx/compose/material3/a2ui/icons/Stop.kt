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
internal val Stop: ImageVector
    get() {
        if (_stop != null) {
            return _stop!!
        }
        _stop =
            ImageVector.Builder(
                    name = "stop",
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
                        moveTo(8f, 8f)
                        verticalLineToRelative(8f)
                        verticalLineTo(8f)
                        close()
                        moveTo(6f, 18f)
                        verticalLineTo(6f)
                        horizontalLineTo(18f)
                        verticalLineTo(18f)
                        horizontalLineTo(6f)
                        close()
                        moveTo(8f, 16f)
                        horizontalLineToRelative(8f)
                        verticalLineTo(8f)
                        horizontalLineTo(8f)
                        verticalLineToRelative(8f)
                        close()
                    }
                }
                .build()
        return _stop!!
    }

private var _stop: ImageVector? = null
