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
public val VolumeUpIcon: ImageVector
    get() {
        if (_volume_up != null) {
            return _volume_up!!
        }
        _volume_up =
            ImageVector.Builder(
                    name = "volume_up",
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
                        moveTo(19f, 11.98f)
                        quadTo(19f, 9.9f, 17.9f, 8.19f)
                        quadTo(16.8f, 6.47f, 14.95f, 5.63f)
                        quadTo(14.58f, 5.45f, 14.4f, 5.09f)
                        reflectiveQuadTo(14.35f, 4.35f)
                        quadTo(14.5f, 3.95f, 14.89f, 3.77f)
                        reflectiveQuadToRelative(0.79f, 0f)
                        quadToRelative(2.43f, 1.07f, 3.88f, 3.29f)
                        quadTo(21f, 9.27f, 21f, 11.98f)
                        reflectiveQuadToRelative(-1.45f, 4.91f)
                        reflectiveQuadToRelative(-3.88f, 3.29f)
                        quadToRelative(-0.4f, 0.18f, -0.79f, 0f)
                        reflectiveQuadTo(14.35f, 19.6f)
                        quadTo(14.23f, 19.23f, 14.4f, 18.86f)
                        reflectiveQuadToRelative(0.55f, -0.54f)
                        quadTo(16.8f, 17.48f, 17.9f, 15.76f)
                        reflectiveQuadTo(19f, 11.98f)
                        close()
                        moveTo(7f, 15f)
                        horizontalLineTo(4f)
                        quadTo(3.58f, 15f, 3.29f, 14.71f)
                        reflectiveQuadTo(3f, 14f)
                        verticalLineTo(10f)
                        quadTo(3f, 9.57f, 3.29f, 9.29f)
                        reflectiveQuadTo(4f, 9f)
                        horizontalLineTo(7f)
                        lineTo(10.3f, 5.7f)
                        quadTo(10.78f, 5.22f, 11.39f, 5.49f)
                        reflectiveQuadTo(12f, 6.43f)
                        verticalLineTo(17.58f)
                        quadToRelative(0f, 0.68f, -0.61f, 0.94f)
                        reflectiveQuadTo(10.3f, 18.3f)
                        lineTo(7f, 15f)
                        close()
                        moveToRelative(9.5f, -3f)
                        quadToRelative(0f, 1.05f, -0.47f, 1.99f)
                        reflectiveQuadToRelative(-1.25f, 1.54f)
                        quadToRelative(-0.25f, 0.15f, -0.51f, 0.01f)
                        reflectiveQuadTo(14f, 15.1f)
                        verticalLineTo(8.85f)
                        quadToRelative(0f, -0.3f, 0.26f, -0.44f)
                        quadToRelative(0.26f, -0.14f, 0.51f, 0.01f)
                        quadTo(15.55f, 9.05f, 16.03f, 10f)
                        reflectiveQuadToRelative(0.47f, 2f)
                        close()
                        moveTo(10f, 8.85f)
                        lineTo(7.85f, 11f)
                        horizontalLineTo(5f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(7.85f)
                        lineTo(10f, 15.15f)
                        verticalLineTo(8.85f)
                        close()
                        moveTo(7.5f, 12f)
                        close()
                    }
                }
                .build()
        return _volume_up!!
    }

private var _volume_up: ImageVector? = null
