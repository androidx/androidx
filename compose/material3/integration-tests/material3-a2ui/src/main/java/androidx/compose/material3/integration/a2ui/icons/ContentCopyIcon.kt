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
public val ContentCopyIcon: ImageVector
    get() {
        if (_content_copy != null) {
            return _content_copy!!
        }
        _content_copy =
            ImageVector.Builder(
                    name = "content_copy",
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
                        moveTo(9f, 18f)
                        quadTo(8.18f, 18f, 7.59f, 17.41f)
                        reflectiveQuadTo(7f, 16f)
                        verticalLineTo(4f)
                        quadTo(7f, 3.17f, 7.59f, 2.59f)
                        reflectiveQuadTo(9f, 2f)
                        horizontalLineToRelative(9f)
                        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(20f, 4f)
                        verticalLineTo(16f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(18f, 18f)
                        horizontalLineTo(9f)
                        close()
                        moveTo(9f, 16f)
                        horizontalLineToRelative(9f)
                        verticalLineTo(4f)
                        horizontalLineTo(9f)
                        verticalLineTo(16f)
                        close()
                        moveTo(5f, 22f)
                        quadTo(4.18f, 22f, 3.59f, 21.41f)
                        reflectiveQuadTo(3f, 20f)
                        verticalLineTo(7f)
                        quadTo(3f, 6.57f, 3.29f, 6.29f)
                        reflectiveQuadTo(4f, 6f)
                        reflectiveQuadTo(4.71f, 6.29f)
                        reflectiveQuadTo(5f, 7f)
                        verticalLineTo(20f)
                        horizontalLineTo(15f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(16f, 21f)
                        reflectiveQuadToRelative(-0.29f, 0.71f)
                        reflectiveQuadTo(15f, 22f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(9f, 16f)
                        verticalLineTo(4f)
                        verticalLineTo(16f)
                        close()
                    }
                }
                .build()
        return _content_copy!!
    }

private var _content_copy: ImageVector? = null
