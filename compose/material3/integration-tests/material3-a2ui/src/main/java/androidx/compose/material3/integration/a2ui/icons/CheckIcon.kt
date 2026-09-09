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
public val CheckIcon: ImageVector
    get() {
        if (_check != null) {
            return _check!!
        }
        _check =
            ImageVector.Builder(
                    name = "check",
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
                        moveTo(9.55f, 15.15f)
                        lineTo(18.03f, 6.68f)
                        quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
                        reflectiveQuadToRelative(0.7f, 0.3f)
                        quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
                        reflectiveQuadTo(19.43f, 8.1f)
                        lineToRelative(-9.18f, 9.2f)
                        quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
                        reflectiveQuadTo(8.85f, 17.3f)
                        lineTo(4.55f, 13f)
                        quadTo(4.25f, 12.7f, 4.26f, 12.29f)
                        reflectiveQuadTo(4.58f, 11.58f)
                        reflectiveQuadToRelative(0.71f, -0.3f)
                        reflectiveQuadTo(6f, 11.58f)
                        lineToRelative(3.55f, 3.58f)
                        close()
                    }
                }
                .build()
        return _check!!
    }

private var _check: ImageVector? = null
