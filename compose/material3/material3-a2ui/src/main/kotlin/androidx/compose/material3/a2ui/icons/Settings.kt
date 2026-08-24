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
internal val Settings: ImageVector
    get() {
        if (_settings != null) {
            return _settings!!
        }
        _settings =
            ImageVector.Builder(
                    name = "settings",
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
                        moveTo(9.25f, 22f)
                        lineTo(8.85f, 18.8f)
                        quadTo(8.53f, 18.68f, 8.24f, 18.5f)
                        reflectiveQuadTo(7.68f, 18.13f)
                        lineTo(4.7f, 19.38f)
                        lineTo(1.95f, 14.63f)
                        lineTo(4.53f, 12.68f)
                        quadTo(4.5f, 12.5f, 4.5f, 12.34f)
                        quadToRelative(0f, -0.16f, 0f, -0.34f)
                        reflectiveQuadToRelative(0f, -0.34f)
                        reflectiveQuadTo(4.53f, 11.33f)
                        lineTo(1.95f, 9.38f)
                        lineTo(4.7f, 4.63f)
                        lineTo(7.68f, 5.88f)
                        quadTo(7.95f, 5.68f, 8.25f, 5.5f)
                        reflectiveQuadTo(8.85f, 5.2f)
                        lineTo(9.25f, 2f)
                        horizontalLineToRelative(5.5f)
                        lineToRelative(0.4f, 3.2f)
                        quadToRelative(0.33f, 0.13f, 0.61f, 0.3f)
                        reflectiveQuadToRelative(0.56f, 0.38f)
                        lineTo(19.3f, 4.63f)
                        lineToRelative(2.75f, 4.75f)
                        lineToRelative(-2.57f, 1.95f)
                        quadToRelative(0.02f, 0.18f, 0.02f, 0.34f)
                        reflectiveQuadToRelative(0f, 0.34f)
                        reflectiveQuadToRelative(0f, 0.34f)
                        reflectiveQuadToRelative(-0.05f, 0.34f)
                        lineToRelative(2.57f, 1.95f)
                        lineToRelative(-2.75f, 4.75f)
                        lineTo(16.33f, 18.13f)
                        quadToRelative(-0.27f, 0.2f, -0.57f, 0.38f)
                        reflectiveQuadToRelative(-0.6f, 0.3f)
                        lineTo(14.75f, 22f)
                        horizontalLineTo(9.25f)
                        close()
                        moveTo(11f, 20f)
                        horizontalLineToRelative(1.98f)
                        lineToRelative(0.35f, -2.65f)
                        quadToRelative(0.78f, -0.2f, 1.44f, -0.59f)
                        reflectiveQuadToRelative(1.21f, -0.94f)
                        lineToRelative(2.47f, 1.03f)
                        lineToRelative(0.98f, -1.7f)
                        lineTo(17.28f, 13.52f)
                        quadToRelative(0.13f, -0.35f, 0.17f, -0.74f)
                        reflectiveQuadTo(17.5f, 12f)
                        reflectiveQuadTo(17.45f, 11.21f)
                        quadTo(17.4f, 10.83f, 17.28f, 10.48f)
                        lineTo(19.43f, 8.85f)
                        lineTo(18.45f, 7.15f)
                        lineTo(15.98f, 8.2f)
                        quadTo(15.43f, 7.63f, 14.76f, 7.24f)
                        reflectiveQuadTo(13.33f, 6.65f)
                        lineTo(13f, 4f)
                        horizontalLineTo(11.03f)
                        lineTo(10.68f, 6.65f)
                        quadTo(9.9f, 6.85f, 9.24f, 7.24f)
                        reflectiveQuadTo(8.03f, 8.17f)
                        lineTo(5.55f, 7.15f)
                        lineTo(4.58f, 8.85f)
                        lineToRelative(2.15f, 1.6f)
                        quadTo(6.6f, 10.83f, 6.55f, 11.2f)
                        reflectiveQuadTo(6.5f, 12f)
                        quadToRelative(0f, 0.4f, 0.05f, 0.77f)
                        reflectiveQuadToRelative(0.17f, 0.75f)
                        lineTo(4.58f, 15.15f)
                        lineToRelative(0.98f, 1.7f)
                        lineTo(8.03f, 15.8f)
                        quadToRelative(0.55f, 0.58f, 1.21f, 0.96f)
                        reflectiveQuadToRelative(1.44f, 0.59f)
                        lineTo(11f, 20f)
                        close()
                        moveToRelative(1.05f, -4.5f)
                        quadToRelative(1.45f, 0f, 2.47f, -1.03f)
                        reflectiveQuadTo(15.55f, 12f)
                        reflectiveQuadTo(14.53f, 9.52f)
                        reflectiveQuadTo(12.05f, 8.5f)
                        quadToRelative(-1.47f, 0f, -2.49f, 1.02f)
                        reflectiveQuadTo(8.55f, 12f)
                        reflectiveQuadToRelative(1.01f, 2.47f)
                        reflectiveQuadToRelative(2.49f, 1.03f)
                        close()
                        moveTo(12f, 12f)
                        close()
                    }
                }
                .build()
        return _settings!!
    }

private var _settings: ImageVector? = null
