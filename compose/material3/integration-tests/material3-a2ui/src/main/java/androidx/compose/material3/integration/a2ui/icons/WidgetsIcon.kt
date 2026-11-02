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
internal val WidgetsIcon: ImageVector
    get() {
        if (_widgets != null) {
            return _widgets!!
        }
        _widgets =
            ImageVector.Builder(
                    name = "widgets",
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
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(16.65f, 13f)
                        lineTo(11f, 7.35f)
                        lineTo(16.65f, 1.7f)
                        lineTo(22.3f, 7.35f)
                        lineTo(16.65f, 13f)
                        close()
                        moveTo(3f, 11f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(8f)
                        horizontalLineTo(3f)
                        close()
                        moveTo(13f, 21f)
                        verticalLineTo(13f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(8f)
                        horizontalLineTo(13f)
                        close()
                        moveTo(3f, 21f)
                        verticalLineTo(13f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(8f)
                        horizontalLineTo(3f)
                        close()
                        moveTo(5f, 9f)
                        horizontalLineTo(9f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(9f)
                        close()
                        moveToRelative(11.68f, 1.2f)
                        lineTo(19.5f, 7.38f)
                        lineTo(16.68f, 4.55f)
                        lineTo(13.85f, 7.38f)
                        lineToRelative(2.82f, 2.82f)
                        close()
                        moveTo(15f, 19f)
                        horizontalLineToRelative(4f)
                        verticalLineTo(15f)
                        horizontalLineTo(15f)
                        verticalLineToRelative(4f)
                        close()
                        moveTo(5f, 19f)
                        horizontalLineTo(9f)
                        verticalLineTo(15f)
                        horizontalLineTo(5f)
                        verticalLineToRelative(4f)
                        close()
                    }
                }
                .build()
        return _widgets!!
    }

private var _widgets: ImageVector? = null
