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
internal val VisibilityOff: ImageVector
    get() {
        if (_visibility_off != null) {
            return _visibility_off!!
        }
        _visibility_off =
            ImageVector.Builder(
                    name = "visibility_off",
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
                        moveTo(16.1f, 13.3f)
                        lineTo(14.65f, 11.85f)
                        quadToRelative(0.22f, -1.18f, -0.67f, -2.2f)
                        quadTo(13.08f, 8.63f, 11.65f, 8.85f)
                        lineTo(10.2f, 7.4f)
                        quadTo(10.63f, 7.2f, 11.06f, 7.1f)
                        reflectiveQuadTo(12f, 7f)
                        quadToRelative(1.88f, 0f, 3.19f, 1.31f)
                        reflectiveQuadTo(16.5f, 11.5f)
                        quadToRelative(0f, 0.5f, -0.1f, 0.94f)
                        reflectiveQuadTo(16.1f, 13.3f)
                        close()
                        moveToRelative(3.2f, 3.15f)
                        lineToRelative(-1.45f, -1.4f)
                        quadToRelative(0.95f, -0.72f, 1.69f, -1.59f)
                        reflectiveQuadTo(20.8f, 11.5f)
                        quadTo(19.55f, 8.98f, 17.21f, 7.49f)
                        reflectiveQuadTo(12f, 6f)
                        quadTo(11.28f, 6f, 10.58f, 6.1f)
                        reflectiveQuadTo(9.2f, 6.4f)
                        lineTo(7.65f, 4.85f)
                        quadTo(8.68f, 4.42f, 9.75f, 4.21f)
                        reflectiveQuadTo(12f, 4f)
                        quadToRelative(3.78f, 0f, 6.73f, 2.09f)
                        reflectiveQuadTo(23f, 11.5f)
                        quadToRelative(-0.57f, 1.47f, -1.51f, 2.74f)
                        reflectiveQuadTo(19.3f, 16.45f)
                        close()
                        moveToRelative(0.5f, 6.15f)
                        lineTo(15.6f, 18.45f)
                        quadToRelative(-0.88f, 0.28f, -1.76f, 0.41f)
                        reflectiveQuadTo(12f, 19f)
                        quadTo(8.23f, 19f, 5.28f, 16.91f)
                        reflectiveQuadTo(1f, 11.5f)
                        quadTo(1.53f, 10.17f, 2.33f, 9.04f)
                        reflectiveQuadTo(4.15f, 7f)
                        lineTo(1.4f, 4.2f)
                        lineTo(2.8f, 2.8f)
                        lineTo(21.2f, 21.2f)
                        lineToRelative(-1.4f, 1.4f)
                        close()
                        moveTo(5.55f, 8.4f)
                        quadTo(4.83f, 9.05f, 4.23f, 9.82f)
                        reflectiveQuadTo(3.2f, 11.5f)
                        quadToRelative(1.25f, 2.52f, 3.59f, 4.01f)
                        reflectiveQuadTo(12f, 17f)
                        quadToRelative(0.5f, 0f, 0.98f, -0.06f)
                        reflectiveQuadTo(13.95f, 16.8f)
                        lineToRelative(-0.9f, -0.95f)
                        quadToRelative(-0.28f, 0.07f, -0.53f, 0.11f)
                        reflectiveQuadTo(12f, 16f)
                        quadTo(10.13f, 16f, 8.81f, 14.69f)
                        reflectiveQuadTo(7.5f, 11.5f)
                        quadToRelative(0f, -0.28f, 0.04f, -0.53f)
                        reflectiveQuadTo(7.65f, 10.45f)
                        lineTo(5.55f, 8.4f)
                        close()
                        moveToRelative(7.98f, 2.33f)
                        close()
                        moveTo(9.75f, 12.6f)
                        close()
                    }
                }
                .build()
        return _visibility_off!!
    }

private var _visibility_off: ImageVector? = null
