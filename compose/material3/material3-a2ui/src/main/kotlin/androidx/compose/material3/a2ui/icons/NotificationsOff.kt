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
internal val NotificationsOff: ImageVector
    get() {
        if (_notifications_off != null) {
            return _notifications_off!!
        }
        _notifications_off =
            ImageVector.Builder(
                    name = "notifications_off",
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
                        quadTo(6f, 9.17f, 6.21f, 8.38f)
                        quadTo(6.43f, 7.57f, 6.85f, 6.85f)
                        lineToRelative(1.5f, 1.5f)
                        quadTo(8.18f, 8.75f, 8.09f, 9.16f)
                        reflectiveQuadTo(8f, 10f)
                        verticalLineToRelative(7f)
                        horizontalLineToRelative(6.2f)
                        lineTo(1.4f, 4.2f)
                        lineTo(2.8f, 2.8f)
                        lineTo(21.2f, 21.2f)
                        lineToRelative(-1.4f, 1.4f)
                        lineTo(16.15f, 19f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(18f, 15.15f)
                        lineToRelative(-2f, -2f)
                        verticalLineTo(10f)
                        quadTo(16f, 8.35f, 14.83f, 7.18f)
                        reflectiveQuadTo(12f, 6f)
                        quadTo(11.35f, 6f, 10.75f, 6.2f)
                        reflectiveQuadTo(9.65f, 6.8f)
                        lineTo(8.2f, 5.35f)
                        quadTo(8.7f, 4.95f, 9.28f, 4.65f)
                        reflectiveQuadTo(10.5f, 4.2f)
                        verticalLineTo(3.5f)
                        quadToRelative(0f, -0.63f, 0.44f, -1.06f)
                        reflectiveQuadTo(12f, 2f)
                        reflectiveQuadToRelative(1.06f, 0.44f)
                        reflectiveQuadTo(13.5f, 3.5f)
                        verticalLineTo(4.2f)
                        quadToRelative(2f, 0.5f, 3.25f, 2.11f)
                        reflectiveQuadTo(18f, 10f)
                        verticalLineToRelative(5.15f)
                        close()
                        moveTo(11.1f, 13.9f)
                        close()
                        moveTo(12f, 22f)
                        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                        reflectiveQuadTo(10f, 20f)
                        horizontalLineToRelative(4f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(12f, 22f)
                        close()
                        moveTo(12.83f, 9.98f)
                        close()
                    }
                }
                .build()
        return _notifications_off!!
    }

private var _notifications_off: ImageVector? = null
