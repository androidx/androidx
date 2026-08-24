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
internal val AttachFile: ImageVector
    get() {
        if (_attach_file != null) {
            return _attach_file!!
        }
        _attach_file =
            ImageVector.Builder(
                    name = "attach_file",
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
                        moveTo(18f, 15.75f)
                        quadToRelative(0f, 2.6f, -1.82f, 4.43f)
                        reflectiveQuadTo(11.75f, 22f)
                        reflectiveQuadTo(7.33f, 20.18f)
                        reflectiveQuadTo(5.5f, 15.75f)
                        verticalLineTo(6.5f)
                        quadTo(5.5f, 4.63f, 6.81f, 3.31f)
                        reflectiveQuadTo(10f, 2f)
                        reflectiveQuadToRelative(3.19f, 1.31f)
                        reflectiveQuadTo(14.5f, 6.5f)
                        verticalLineToRelative(8.75f)
                        quadToRelative(0f, 1.15f, -0.8f, 1.95f)
                        reflectiveQuadTo(11.75f, 18f)
                        reflectiveQuadTo(9.8f, 17.2f)
                        reflectiveQuadTo(9f, 15.25f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(9.25f)
                        quadToRelative(0f, 0.32f, 0.21f, 0.54f)
                        reflectiveQuadTo(11.75f, 16f)
                        reflectiveQuadToRelative(0.54f, -0.21f)
                        reflectiveQuadTo(12.5f, 15.25f)
                        verticalLineTo(6.5f)
                        quadTo(12.48f, 5.45f, 11.76f, 4.72f)
                        reflectiveQuadTo(10f, 4f)
                        reflectiveQuadTo(8.23f, 4.72f)
                        reflectiveQuadTo(7.5f, 6.5f)
                        verticalLineToRelative(9.25f)
                        quadToRelative(-0.02f, 1.77f, 1.22f, 3.01f)
                        quadTo(9.98f, 20f, 11.75f, 20f)
                        quadToRelative(1.75f, 0f, 2.98f, -1.24f)
                        reflectiveQuadTo(16f, 15.75f)
                        verticalLineTo(6f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(9.75f)
                        close()
                    }
                }
                .build()
        return _attach_file!!
    }

private var _attach_file: ImageVector? = null
