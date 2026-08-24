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
internal val Lock: ImageVector
    get() {
        if (_lock != null) {
            return _lock!!
        }
        _lock =
            ImageVector.Builder(
                    name = "lock",
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
                        moveTo(6f, 22f)
                        quadTo(5.18f, 22f, 4.59f, 21.41f)
                        reflectiveQuadTo(4f, 20f)
                        verticalLineTo(10f)
                        quadTo(4f, 9.17f, 4.59f, 8.59f)
                        reflectiveQuadTo(6f, 8f)
                        horizontalLineTo(7f)
                        verticalLineTo(6f)
                        quadTo(7f, 3.92f, 8.46f, 2.46f)
                        reflectiveQuadTo(12f, 1f)
                        reflectiveQuadToRelative(3.54f, 1.46f)
                        reflectiveQuadTo(17f, 6f)
                        verticalLineTo(8f)
                        horizontalLineToRelative(1f)
                        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(20f, 10f)
                        verticalLineTo(20f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(18f, 22f)
                        horizontalLineTo(6f)
                        close()
                        moveTo(6f, 20f)
                        horizontalLineTo(18f)
                        verticalLineTo(10f)
                        horizontalLineTo(6f)
                        verticalLineTo(20f)
                        close()
                        moveToRelative(7.41f, -3.59f)
                        quadTo(14f, 15.83f, 14f, 15f)
                        reflectiveQuadTo(13.41f, 13.59f)
                        reflectiveQuadTo(12f, 13f)
                        reflectiveQuadToRelative(-1.41f, 0.59f)
                        quadTo(10f, 14.18f, 10f, 15f)
                        reflectiveQuadToRelative(0.59f, 1.41f)
                        reflectiveQuadTo(12f, 17f)
                        reflectiveQuadToRelative(1.41f, -0.59f)
                        close()
                        moveTo(9f, 8f)
                        horizontalLineToRelative(6f)
                        verticalLineTo(6f)
                        quadTo(15f, 4.75f, 14.13f, 3.88f)
                        reflectiveQuadTo(12f, 3f)
                        reflectiveQuadTo(9.88f, 3.88f)
                        reflectiveQuadTo(9f, 6f)
                        verticalLineTo(8f)
                        close()
                        moveTo(6f, 20f)
                        verticalLineTo(10f)
                        verticalLineTo(20f)
                        close()
                    }
                }
                .build()
        return _lock!!
    }

private var _lock: ImageVector? = null
