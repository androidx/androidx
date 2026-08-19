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
internal val Edit: ImageVector
    get() {
        if (_edit != null) {
            return _edit!!
        }
        _edit =
            ImageVector.Builder(
                    name = "edit",
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
                        moveTo(5f, 19f)
                        horizontalLineTo(6.43f)
                        lineTo(16.2f, 9.23f)
                        lineTo(14.78f, 7.8f)
                        lineTo(5f, 17.58f)
                        verticalLineTo(19f)
                        close()
                        moveTo(3f, 21f)
                        verticalLineTo(16.75f)
                        lineTo(16.2f, 3.57f)
                        quadTo(16.5f, 3.3f, 16.86f, 3.15f)
                        reflectiveQuadTo(17.63f, 3f)
                        quadToRelative(0.4f, 0f, 0.78f, 0.15f)
                        reflectiveQuadTo(19.05f, 3.6f)
                        lineTo(20.43f, 5f)
                        quadToRelative(0.3f, 0.27f, 0.44f, 0.65f)
                        reflectiveQuadTo(21f, 6.4f)
                        quadToRelative(0f, 0.4f, -0.14f, 0.76f)
                        reflectiveQuadTo(20.43f, 7.82f)
                        lineTo(7.25f, 21f)
                        horizontalLineTo(3f)
                        close()
                        moveTo(19f, 6.4f)
                        lineTo(17.6f, 5f)
                        lineTo(19f, 6.4f)
                        close()
                        moveTo(15.48f, 8.52f)
                        lineTo(14.78f, 7.8f)
                        lineTo(16.2f, 9.23f)
                        lineTo(15.48f, 8.52f)
                        close()
                    }
                }
                .build()
        return _edit!!
    }

private var _edit: ImageVector? = null
