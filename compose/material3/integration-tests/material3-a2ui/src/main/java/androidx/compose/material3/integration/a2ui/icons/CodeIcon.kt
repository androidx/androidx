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
public val CodeIcon: ImageVector
    get() {
        if (_code_blocks != null) {
            return _code_blocks!!
        }
        _code_blocks =
            ImageVector.Builder(
                    name = "code_blocks",
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
                        moveTo(8.83f, 12f)
                        lineTo(10.3f, 10.52f)
                        quadToRelative(0.3f, -0.3f, 0.3f, -0.7f)
                        reflectiveQuadTo(10.3f, 9.13f)
                        quadTo(10f, 8.82f, 9.59f, 8.82f)
                        reflectiveQuadTo(8.88f, 9.13f)
                        lineTo(6.7f, 11.3f)
                        quadTo(6.55f, 11.45f, 6.49f, 11.63f)
                        reflectiveQuadTo(6.43f, 12f)
                        reflectiveQuadToRelative(0.06f, 0.38f)
                        reflectiveQuadTo(6.7f, 12.7f)
                        lineToRelative(2.17f, 2.18f)
                        quadToRelative(0.3f, 0.3f, 0.71f, 0.3f)
                        reflectiveQuadToRelative(0.71f, -0.3f)
                        reflectiveQuadToRelative(0.3f, -0.7f)
                        reflectiveQuadToRelative(-0.3f, -0.7f)
                        lineTo(8.83f, 12f)
                        close()
                        moveToRelative(6.35f, 0f)
                        lineTo(13.7f, 13.48f)
                        quadToRelative(-0.3f, 0.3f, -0.3f, 0.7f)
                        reflectiveQuadToRelative(0.3f, 0.7f)
                        reflectiveQuadToRelative(0.71f, 0.3f)
                        reflectiveQuadToRelative(0.71f, -0.3f)
                        lineTo(17.3f, 12.7f)
                        quadToRelative(0.15f, -0.15f, 0.21f, -0.32f)
                        reflectiveQuadTo(17.58f, 12f)
                        reflectiveQuadTo(17.51f, 11.63f)
                        reflectiveQuadTo(17.3f, 11.3f)
                        lineTo(15.13f, 9.13f)
                        quadTo(14.98f, 8.98f, 14.79f, 8.9f)
                        reflectiveQuadTo(14.41f, 8.82f)
                        reflectiveQuadTo(14.04f, 8.9f)
                        quadTo(13.85f, 8.98f, 13.7f, 9.13f)
                        quadToRelative(-0.3f, 0.3f, -0.3f, 0.7f)
                        reflectiveQuadToRelative(0.3f, 0.7f)
                        lineTo(15.18f, 12f)
                        close()
                        moveTo(5f, 21f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineTo(19f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(21f, 5f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(5f, 19f)
                        horizontalLineTo(19f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(19f)
                        close()
                        moveTo(5f, 5f)
                        verticalLineTo(19f)
                        verticalLineTo(5f)
                        close()
                    }
                }
                .build()
        return _code_blocks!!
    }

private var _code_blocks: ImageVector? = null
