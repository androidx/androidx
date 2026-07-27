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
public val ChevronForwardIcon: ImageVector
    get() {
        if (_chevron_forward != null) {
            return _chevron_forward!!
        }
        _chevron_forward =
            ImageVector.Builder(
                    name = "chevron_forward",
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
                        moveTo(12.6f, 12f)
                        lineTo(8.7f, 8.1f)
                        quadTo(8.43f, 7.82f, 8.43f, 7.4f)
                        reflectiveQuadTo(8.7f, 6.7f)
                        reflectiveQuadTo(9.4f, 6.43f)
                        reflectiveQuadTo(10.1f, 6.7f)
                        lineToRelative(4.6f, 4.6f)
                        quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
                        reflectiveQuadTo(14.98f, 12f)
                        reflectiveQuadToRelative(-0.06f, 0.38f)
                        reflectiveQuadTo(14.7f, 12.7f)
                        lineToRelative(-4.6f, 4.6f)
                        quadTo(9.83f, 17.58f, 9.4f, 17.58f)
                        reflectiveQuadTo(8.7f, 17.3f)
                        quadTo(8.43f, 17.02f, 8.43f, 16.6f)
                        reflectiveQuadTo(8.7f, 15.9f)
                        lineTo(12.6f, 12f)
                        close()
                    }
                }
                .build()
        return _chevron_forward!!
    }

private var _chevron_forward: ImageVector? = null
