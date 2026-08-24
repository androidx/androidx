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
internal val Person: ImageVector
    get() {
        if (_person != null) {
            return _person!!
        }
        _person =
            ImageVector.Builder(
                    name = "person",
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
                        moveTo(9.18f, 10.83f)
                        quadTo(8f, 9.65f, 8f, 8f)
                        reflectiveQuadTo(9.18f, 5.18f)
                        reflectiveQuadTo(12f, 4f)
                        reflectiveQuadToRelative(2.83f, 1.18f)
                        reflectiveQuadTo(16f, 8f)
                        reflectiveQuadToRelative(-1.17f, 2.82f)
                        reflectiveQuadTo(12f, 12f)
                        reflectiveQuadTo(9.18f, 10.83f)
                        close()
                        moveTo(4f, 20f)
                        verticalLineTo(17.2f)
                        quadTo(4f, 16.35f, 4.44f, 15.64f)
                        quadTo(4.88f, 14.93f, 5.6f, 14.55f)
                        quadTo(7.15f, 13.77f, 8.75f, 13.39f)
                        reflectiveQuadTo(12f, 13f)
                        reflectiveQuadToRelative(3.25f, 0.39f)
                        reflectiveQuadToRelative(3.15f, 1.16f)
                        quadToRelative(0.72f, 0.38f, 1.16f, 1.09f)
                        reflectiveQuadTo(20f, 17.2f)
                        verticalLineTo(20f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(6f, 18f)
                        horizontalLineTo(18f)
                        verticalLineTo(17.2f)
                        quadToRelative(0f, -0.27f, -0.14f, -0.5f)
                        quadTo(17.73f, 16.48f, 17.5f, 16.35f)
                        quadTo(16.15f, 15.68f, 14.78f, 15.34f)
                        reflectiveQuadTo(12f, 15f)
                        reflectiveQuadTo(9.23f, 15.34f)
                        reflectiveQuadTo(6.5f, 16.35f)
                        quadTo(6.28f, 16.48f, 6.14f, 16.7f)
                        quadTo(6f, 16.93f, 6f, 17.2f)
                        verticalLineTo(18f)
                        close()
                        moveTo(13.41f, 9.41f)
                        quadTo(14f, 8.82f, 14f, 8f)
                        reflectiveQuadTo(13.41f, 6.59f)
                        reflectiveQuadTo(12f, 6f)
                        reflectiveQuadTo(10.59f, 6.59f)
                        quadTo(10f, 7.18f, 10f, 8f)
                        reflectiveQuadToRelative(0.59f, 1.41f)
                        reflectiveQuadTo(12f, 10f)
                        reflectiveQuadTo(13.41f, 9.41f)
                        close()
                        moveTo(12f, 8f)
                        close()
                        moveToRelative(0f, 10f)
                        close()
                    }
                }
                .build()
        return _person!!
    }

private var _person: ImageVector? = null
