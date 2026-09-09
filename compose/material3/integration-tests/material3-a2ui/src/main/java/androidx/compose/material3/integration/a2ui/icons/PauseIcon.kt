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
public val PauseIcon: ImageVector
    get() {
        if (_pause != null) {
            return _pause!!
        }
        _pause =
            ImageVector.Builder(
                    name = "pause",
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
                        moveTo(15f, 19f)
                        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                        reflectiveQuadTo(13f, 17f)
                        verticalLineTo(7f)
                        quadTo(13f, 6.18f, 13.59f, 5.59f)
                        reflectiveQuadTo(15f, 5f)
                        horizontalLineToRelative(2f)
                        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
                        quadTo(19f, 6.18f, 19f, 7f)
                        verticalLineTo(17f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(17f, 19f)
                        horizontalLineTo(15f)
                        close()
                        moveTo(7f, 19f)
                        quadTo(6.18f, 19f, 5.59f, 18.41f)
                        reflectiveQuadTo(5f, 17f)
                        verticalLineTo(7f)
                        quadTo(5f, 6.18f, 5.59f, 5.59f)
                        reflectiveQuadTo(7f, 5f)
                        horizontalLineTo(9f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        quadTo(11f, 6.18f, 11f, 7f)
                        verticalLineTo(17f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(9f, 19f)
                        horizontalLineTo(7f)
                        close()
                        moveToRelative(8f, -2f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(7f)
                        horizontalLineTo(15f)
                        verticalLineTo(17f)
                        close()
                        moveTo(7f, 17f)
                        horizontalLineTo(9f)
                        verticalLineTo(7f)
                        horizontalLineTo(7f)
                        verticalLineTo(17f)
                        close()
                        moveTo(7f, 7f)
                        verticalLineTo(17f)
                        verticalLineTo(7f)
                        close()
                        moveToRelative(8f, 0f)
                        verticalLineTo(17f)
                        verticalLineTo(7f)
                        close()
                    }
                }
                .build()
        return _pause!!
    }

private var _pause: ImageVector? = null
