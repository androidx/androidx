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
internal val MoreVert: ImageVector
    get() {
        if (_more_vert != null) {
            return _more_vert!!
        }
        _more_vert =
            ImageVector.Builder(
                    name = "more_vert",
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
                        moveTo(12f, 20f)
                        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                        reflectiveQuadTo(10f, 18f)
                        reflectiveQuadToRelative(0.59f, -1.41f)
                        reflectiveQuadTo(12f, 16f)
                        reflectiveQuadToRelative(1.41f, 0.59f)
                        quadTo(14f, 17.18f, 14f, 18f)
                        reflectiveQuadToRelative(-0.59f, 1.41f)
                        reflectiveQuadTo(12f, 20f)
                        close()
                        moveToRelative(0f, -6f)
                        quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                        reflectiveQuadTo(10f, 12f)
                        reflectiveQuadToRelative(0.59f, -1.41f)
                        reflectiveQuadTo(12f, 10f)
                        reflectiveQuadToRelative(1.41f, 0.59f)
                        quadTo(14f, 11.18f, 14f, 12f)
                        reflectiveQuadToRelative(-0.59f, 1.41f)
                        reflectiveQuadTo(12f, 14f)
                        close()
                        moveTo(12f, 8f)
                        quadTo(11.18f, 8f, 10.59f, 7.41f)
                        reflectiveQuadTo(10f, 6f)
                        reflectiveQuadTo(10.59f, 4.59f)
                        reflectiveQuadTo(12f, 4f)
                        reflectiveQuadToRelative(1.41f, 0.59f)
                        quadTo(14f, 5.18f, 14f, 6f)
                        reflectiveQuadTo(13.41f, 7.41f)
                        reflectiveQuadTo(12f, 8f)
                        close()
                    }
                }
                .build()
        return _more_vert!!
    }

private var _more_vert: ImageVector? = null
