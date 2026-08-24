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
internal val Visibility: ImageVector
    get() {
        if (_visibility != null) {
            return _visibility!!
        }
        _visibility =
            ImageVector.Builder(
                    name = "visibility",
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
                        moveTo(15.19f, 14.69f)
                        quadTo(16.5f, 13.38f, 16.5f, 11.5f)
                        reflectiveQuadTo(15.19f, 8.31f)
                        reflectiveQuadTo(12f, 7f)
                        reflectiveQuadTo(8.81f, 8.31f)
                        reflectiveQuadTo(7.5f, 11.5f)
                        reflectiveQuadToRelative(1.31f, 3.19f)
                        reflectiveQuadTo(12f, 16f)
                        reflectiveQuadToRelative(3.19f, -1.31f)
                        close()
                        moveToRelative(-5.1f, -1.28f)
                        quadTo(9.3f, 12.63f, 9.3f, 11.5f)
                        reflectiveQuadTo(10.09f, 9.59f)
                        reflectiveQuadTo(12f, 8.8f)
                        reflectiveQuadToRelative(1.91f, 0.79f)
                        quadToRelative(0.79f, 0.79f, 0.79f, 1.91f)
                        reflectiveQuadToRelative(-0.79f, 1.91f)
                        reflectiveQuadTo(12f, 14.2f)
                        reflectiveQuadTo(10.09f, 13.41f)
                        close()
                        moveTo(5.35f, 16.96f)
                        quadTo(2.35f, 14.93f, 1f, 11.5f)
                        quadTo(2.35f, 8.07f, 5.35f, 6.04f)
                        reflectiveQuadTo(12f, 4f)
                        reflectiveQuadToRelative(6.65f, 2.04f)
                        reflectiveQuadTo(23f, 11.5f)
                        quadToRelative(-1.35f, 3.42f, -4.35f, 5.46f)
                        reflectiveQuadTo(12f, 19f)
                        reflectiveQuadTo(5.35f, 16.96f)
                        close()
                        moveTo(12f, 11.5f)
                        close()
                        moveToRelative(5.19f, 4.01f)
                        quadTo(19.55f, 14.02f, 20.8f, 11.5f)
                        quadTo(19.55f, 8.98f, 17.19f, 7.49f)
                        reflectiveQuadTo(12f, 6f)
                        quadTo(9.18f, 6f, 6.81f, 7.49f)
                        reflectiveQuadTo(3.2f, 11.5f)
                        quadToRelative(1.25f, 2.52f, 3.61f, 4.01f)
                        reflectiveQuadTo(12f, 17f)
                        reflectiveQuadToRelative(5.19f, -1.49f)
                        close()
                    }
                }
                .build()
        return _visibility!!
    }

private var _visibility: ImageVector? = null
