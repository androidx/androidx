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
internal val Phone: ImageVector
    get() {
        if (_phone != null) {
            return _phone!!
        }
        _phone =
            ImageVector.Builder(
                    name = "phone",
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
                        moveTo(19.95f, 21f)
                        quadToRelative(-3.13f, 0f, -6.18f, -1.36f)
                        reflectiveQuadTo(8.23f, 15.78f)
                        quadTo(5.73f, 13.27f, 4.36f, 10.23f)
                        reflectiveQuadTo(3f, 4.05f)
                        quadTo(3f, 3.6f, 3.3f, 3.3f)
                        reflectiveQuadTo(4.05f, 3f)
                        horizontalLineTo(8.1f)
                        quadTo(8.45f, 3f, 8.73f, 3.24f)
                        reflectiveQuadTo(9.05f, 3.8f)
                        lineTo(9.7f, 7.3f)
                        quadTo(9.75f, 7.7f, 9.68f, 7.97f)
                        reflectiveQuadTo(9.4f, 8.45f)
                        lineTo(6.98f, 10.9f)
                        quadToRelative(0.5f, 0.93f, 1.19f, 1.79f)
                        reflectiveQuadToRelative(1.51f, 1.66f)
                        quadToRelative(0.78f, 0.78f, 1.63f, 1.44f)
                        reflectiveQuadTo(13.1f, 17f)
                        lineToRelative(2.35f, -2.35f)
                        quadToRelative(0.22f, -0.23f, 0.59f, -0.34f)
                        reflectiveQuadToRelative(0.71f, -0.06f)
                        lineToRelative(3.45f, 0.7f)
                        quadToRelative(0.35f, 0.1f, 0.57f, 0.36f)
                        reflectiveQuadTo(21f, 15.9f)
                        verticalLineToRelative(4.05f)
                        quadToRelative(0f, 0.45f, -0.3f, 0.75f)
                        reflectiveQuadTo(19.95f, 21f)
                        close()
                        moveTo(6.03f, 9f)
                        lineTo(7.68f, 7.35f)
                        lineTo(7.25f, 5f)
                        horizontalLineTo(5.03f)
                        quadTo(5.15f, 6.02f, 5.38f, 7.02f)
                        reflectiveQuadTo(6.03f, 9f)
                        close()
                        moveToRelative(8.95f, 8.95f)
                        quadToRelative(0.97f, 0.43f, 1.99f, 0.68f)
                        reflectiveQuadTo(19f, 18.95f)
                        verticalLineToRelative(-2.2f)
                        lineTo(16.65f, 16.27f)
                        lineToRelative(-1.68f, 1.68f)
                        close()
                        moveTo(6.03f, 9f)
                        close()
                        moveToRelative(8.95f, 8.95f)
                        close()
                    }
                }
                .build()
        return _phone!!
    }

private var _phone: ImageVector? = null
