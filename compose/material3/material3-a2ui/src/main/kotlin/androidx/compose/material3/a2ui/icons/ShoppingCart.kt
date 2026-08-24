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
internal val ShoppingCart: ImageVector
    get() {
        if (_shopping_cart != null) {
            return _shopping_cart!!
        }
        _shopping_cart =
            ImageVector.Builder(
                    name = "shopping_cart",
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
                        moveTo(5.59f, 21.41f)
                        quadTo(5f, 20.83f, 5f, 20f)
                        reflectiveQuadTo(5.59f, 18.59f)
                        reflectiveQuadTo(7f, 18f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        quadTo(9f, 19.18f, 9f, 20f)
                        reflectiveQuadTo(8.41f, 21.41f)
                        quadTo(7.83f, 22f, 7f, 22f)
                        reflectiveQuadTo(5.59f, 21.41f)
                        close()
                        moveToRelative(10f, 0f)
                        quadTo(15f, 20.83f, 15f, 20f)
                        reflectiveQuadToRelative(0.59f, -1.41f)
                        reflectiveQuadTo(17f, 18f)
                        reflectiveQuadToRelative(1.41f, 0.59f)
                        quadTo(19f, 19.18f, 19f, 20f)
                        reflectiveQuadToRelative(-0.59f, 1.41f)
                        reflectiveQuadTo(17f, 22f)
                        reflectiveQuadTo(15.59f, 21.41f)
                        close()
                        moveTo(6.15f, 6f)
                        lineToRelative(2.4f, 5f)
                        horizontalLineToRelative(7f)
                        lineTo(18.3f, 6f)
                        horizontalLineTo(6.15f)
                        close()
                        moveTo(5.2f, 4f)
                        horizontalLineTo(19.95f)
                        quadToRelative(0.57f, 0f, 0.88f, 0.51f)
                        reflectiveQuadToRelative(0.02f, 1.04f)
                        lineToRelative(-3.55f, 6.4f)
                        quadToRelative(-0.27f, 0.5f, -0.74f, 0.78f)
                        reflectiveQuadTo(15.55f, 13f)
                        horizontalLineTo(8.1f)
                        lineTo(7f, 15f)
                        horizontalLineTo(19f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(7f)
                        quadTo(5.88f, 17f, 5.3f, 16.01f)
                        quadTo(4.73f, 15.03f, 5.25f, 14.05f)
                        lineTo(6.6f, 11.6f)
                        lineTo(3f, 4f)
                        horizontalLineTo(1f)
                        verticalLineTo(2f)
                        horizontalLineTo(4.25f)
                        lineTo(5.2f, 4f)
                        close()
                        moveToRelative(3.35f, 7f)
                        horizontalLineToRelative(7f)
                        horizontalLineToRelative(-7f)
                        close()
                    }
                }
                .build()
        return _shopping_cart!!
    }

private var _shopping_cart: ImageVector? = null
