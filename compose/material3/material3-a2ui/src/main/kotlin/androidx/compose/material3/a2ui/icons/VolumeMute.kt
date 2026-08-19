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
internal val VolumeMute: ImageVector
    get() {
        if (_volume_mute != null) {
            return _volume_mute!!
        }
        _volume_mute =
            ImageVector.Builder(
                    name = "volume_mute",
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
                        moveTo(7f, 15f)
                        verticalLineTo(9f)
                        horizontalLineToRelative(4f)
                        lineTo(16f, 4f)
                        verticalLineTo(20f)
                        lineTo(11f, 15f)
                        horizontalLineTo(7f)
                        close()
                        moveTo(9f, 13f)
                        horizontalLineToRelative(2.85f)
                        lineTo(14f, 15.15f)
                        verticalLineTo(8.85f)
                        lineTo(11.85f, 11f)
                        horizontalLineTo(9f)
                        verticalLineToRelative(2f)
                        close()
                        moveToRelative(2.5f, -1f)
                        close()
                    }
                }
                .build()
        return _volume_mute!!
    }

private var _volume_mute: ImageVector? = null
