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
internal val Print: ImageVector
    get() {
        if (_print != null) {
            return _print!!
        }
        _print =
            ImageVector.Builder(
                    name = "print",
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
                        moveTo(16f, 8f)
                        verticalLineTo(5f)
                        horizontalLineTo(8f)
                        verticalLineTo(8f)
                        horizontalLineTo(6f)
                        verticalLineTo(3f)
                        horizontalLineTo(18f)
                        verticalLineTo(8f)
                        horizontalLineTo(16f)
                        close()
                        moveTo(4f, 10f)
                        quadToRelative(0f, 0f, 0.29f, 0f)
                        reflectiveQuadTo(5f, 10f)
                        horizontalLineTo(19f)
                        quadToRelative(0.43f, 0f, 0.71f, 0f)
                        reflectiveQuadTo(20f, 10f)
                        horizontalLineTo(18f)
                        horizontalLineTo(6f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(14f, 2.5f)
                        quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                        quadTo(19f, 11.93f, 19f, 11.5f)
                        reflectiveQuadTo(18.71f, 10.79f)
                        reflectiveQuadTo(18f, 10.5f)
                        reflectiveQuadToRelative(-0.71f, 0.29f)
                        reflectiveQuadTo(17f, 11.5f)
                        reflectiveQuadToRelative(0.29f, 0.71f)
                        reflectiveQuadTo(18f, 12.5f)
                        close()
                        moveTo(16f, 19f)
                        verticalLineTo(15f)
                        horizontalLineTo(8f)
                        verticalLineToRelative(4f)
                        horizontalLineToRelative(8f)
                        close()
                        moveToRelative(2f, 2f)
                        horizontalLineTo(6f)
                        verticalLineTo(17f)
                        horizontalLineTo(2f)
                        verticalLineTo(11f)
                        quadTo(2f, 9.73f, 2.88f, 8.86f)
                        reflectiveQuadTo(5f, 8f)
                        horizontalLineTo(19f)
                        quadToRelative(1.28f, 0f, 2.14f, 0.86f)
                        quadTo(22f, 9.73f, 22f, 11f)
                        verticalLineToRelative(6f)
                        horizontalLineTo(18f)
                        verticalLineToRelative(4f)
                        close()
                        moveToRelative(2f, -6f)
                        verticalLineTo(11f)
                        quadToRelative(0f, -0.43f, -0.29f, -0.71f)
                        reflectiveQuadTo(19f, 10f)
                        horizontalLineTo(5f)
                        quadTo(4.58f, 10f, 4.29f, 10.29f)
                        reflectiveQuadTo(4f, 11f)
                        verticalLineToRelative(4f)
                        horizontalLineTo(6f)
                        verticalLineTo(13f)
                        horizontalLineTo(18f)
                        verticalLineToRelative(2f)
                        horizontalLineToRelative(2f)
                        close()
                    }
                }
                .build()
        return _print!!
    }

private var _print: ImageVector? = null
