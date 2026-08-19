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
internal val Help: ImageVector
    get() {
        if (_help != null) {
            return _help!!
        }
        _help =
            ImageVector.Builder(
                    name = "help",
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
                        moveTo(12.84f, 17.64f)
                        quadTo(13.2f, 17.27f, 13.2f, 16.75f)
                        reflectiveQuadTo(12.84f, 15.86f)
                        reflectiveQuadTo(11.95f, 15.5f)
                        reflectiveQuadToRelative(-0.89f, 0.36f)
                        quadTo(10.7f, 16.23f, 10.7f, 16.75f)
                        reflectiveQuadToRelative(0.36f, 0.89f)
                        reflectiveQuadTo(11.95f, 18f)
                        reflectiveQuadToRelative(0.89f, -0.36f)
                        close()
                        moveTo(11.05f, 14.15f)
                        horizontalLineTo(12.9f)
                        quadToRelative(0f, -0.82f, 0.19f, -1.3f)
                        reflectiveQuadToRelative(1.06f, -1.3f)
                        quadTo(14.8f, 10.9f, 15.18f, 10.31f)
                        reflectiveQuadTo(15.55f, 8.9f)
                        quadToRelative(0f, -1.4f, -1.03f, -2.15f)
                        reflectiveQuadTo(12.1f, 6f)
                        quadTo(10.68f, 6f, 9.79f, 6.75f)
                        reflectiveQuadTo(8.55f, 8.55f)
                        lineTo(10.2f, 9.2f)
                        quadTo(10.33f, 8.75f, 10.76f, 8.23f)
                        reflectiveQuadTo(12.1f, 7.7f)
                        quadToRelative(0.8f, 0f, 1.2f, 0.44f)
                        reflectiveQuadTo(13.7f, 9.1f)
                        quadToRelative(0f, 0.5f, -0.3f, 0.94f)
                        reflectiveQuadToRelative(-0.75f, 0.81f)
                        quadToRelative(-1.1f, 0.97f, -1.35f, 1.47f)
                        reflectiveQuadToRelative(-0.25f, 1.82f)
                        close()
                        moveTo(12f, 22f)
                        quadTo(9.93f, 22f, 8.1f, 21.21f)
                        quadTo(6.28f, 20.43f, 4.93f, 19.08f)
                        quadTo(3.58f, 17.73f, 2.79f, 15.9f)
                        reflectiveQuadTo(2f, 12f)
                        quadTo(2f, 9.92f, 2.79f, 8.1f)
                        quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                        quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                        quadTo(9.93f, 2f, 12f, 2f)
                        reflectiveQuadToRelative(3.9f, 0.79f)
                        reflectiveQuadToRelative(3.17f, 2.14f)
                        quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                        quadTo(22f, 9.92f, 22f, 12f)
                        reflectiveQuadToRelative(-0.79f, 3.9f)
                        reflectiveQuadToRelative(-2.14f, 3.17f)
                        quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
                        reflectiveQuadTo(12f, 22f)
                        close()
                        moveToRelative(0f, -2f)
                        quadToRelative(3.35f, 0f, 5.68f, -2.32f)
                        reflectiveQuadTo(20f, 12f)
                        reflectiveQuadTo(17.68f, 6.32f)
                        reflectiveQuadTo(12f, 4f)
                        reflectiveQuadTo(6.33f, 6.32f)
                        reflectiveQuadTo(4f, 12f)
                        reflectiveQuadToRelative(2.33f, 5.68f)
                        reflectiveQuadTo(12f, 20f)
                        close()
                        moveToRelative(0f, -8f)
                        close()
                    }
                }
                .build()
        return _help!!
    }

private var _help: ImageVector? = null
