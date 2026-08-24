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
internal val Notifications: ImageVector
    get() {
        if (_notifications != null) {
            return _notifications!!
        }
        _notifications =
            ImageVector.Builder(
                    name = "notifications",
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
                        moveTo(4f, 19f)
                        verticalLineTo(17f)
                        horizontalLineTo(6f)
                        verticalLineTo(10f)
                        quadTo(6f, 7.93f, 7.25f, 6.31f)
                        reflectiveQuadTo(10.5f, 4.2f)
                        verticalLineTo(3.5f)
                        quadToRelative(0f, -0.63f, 0.44f, -1.06f)
                        reflectiveQuadTo(12f, 2f)
                        reflectiveQuadToRelative(1.06f, 0.44f)
                        reflectiveQuadTo(13.5f, 3.5f)
                        verticalLineTo(4.2f)
                        quadToRelative(2f, 0.5f, 3.25f, 2.11f)
                        reflectiveQuadTo(18f, 10f)
                        verticalLineToRelative(7f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(8f, -7.5f)
                        close()
                        moveTo(12f, 22f)
                        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                        reflectiveQuadTo(10f, 20f)
                        horizontalLineToRelative(4f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(12f, 22f)
                        close()
                        moveTo(8f, 17f)
                        horizontalLineToRelative(8f)
                        verticalLineTo(10f)
                        quadTo(16f, 8.35f, 14.83f, 7.18f)
                        reflectiveQuadTo(12f, 6f)
                        reflectiveQuadTo(9.18f, 7.18f)
                        reflectiveQuadTo(8f, 10f)
                        verticalLineToRelative(7f)
                        close()
                    }
                }
                .build()
        return _notifications!!
    }

private var _notifications: ImageVector? = null
