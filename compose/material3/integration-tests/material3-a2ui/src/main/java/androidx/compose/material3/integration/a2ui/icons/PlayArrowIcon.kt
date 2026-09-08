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
public val PlayArrowIcon: ImageVector
    get() {
        if (_play_arrow != null) {
            return _play_arrow!!
        }
        _play_arrow =
            ImageVector.Builder(
                    name = "play_arrow",
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
                        moveTo(8f, 17.18f)
                        verticalLineTo(6.82f)
                        quadTo(8f, 6.4f, 8.3f, 6.11f)
                        quadTo(8.6f, 5.82f, 9f, 5.82f)
                        quadToRelative(0.13f, 0f, 0.26f, 0.04f)
                        reflectiveQuadTo(9.53f, 5.97f)
                        lineToRelative(8.15f, 5.18f)
                        quadToRelative(0.23f, 0.15f, 0.34f, 0.38f)
                        quadToRelative(0.11f, 0.23f, 0.11f, 0.48f)
                        reflectiveQuadToRelative(-0.11f, 0.47f)
                        reflectiveQuadToRelative(-0.34f, 0.38f)
                        lineTo(9.53f, 18.02f)
                        quadTo(9.4f, 18.1f, 9.26f, 18.14f)
                        quadTo(9.13f, 18.18f, 9f, 18.18f)
                        quadToRelative(-0.4f, 0f, -0.7f, -0.29f)
                        reflectiveQuadTo(8f, 17.18f)
                        close()
                        moveTo(10f, 12f)
                        close()
                        moveToRelative(0f, 3.35f)
                        lineTo(15.25f, 12f)
                        lineTo(10f, 8.65f)
                        verticalLineToRelative(6.7f)
                        close()
                    }
                }
                .build()
        return _play_arrow!!
    }

private var _play_arrow: ImageVector? = null
