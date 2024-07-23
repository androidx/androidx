/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object Icons {
    internal object Filled {
        internal val ArrowDropDown: ImageVector
            get() {
                if (_arrowDropDown != null) {
                    return _arrowDropDown!!
                }
                _arrowDropDown =
                    materialIcon(name = "Filled.ArrowDropDown") {
                        materialPath {
                            moveTo(7.0f, 10.0f)
                            lineToRelative(5.0f, 5.0f)
                            lineToRelative(5.0f, -5.0f)
                            close()
                        }
                    }
                return _arrowDropDown!!
            }

        private var _arrowDropDown: ImageVector? = null
    }
}

private inline fun materialIcon(
    name: String,
    block: ImageVector.Builder.() -> ImageVector.Builder
): ImageVector =
    ImageVector.Builder(
            name = name,
            defaultWidth = MaterialIconDimension.dp,
            defaultHeight = MaterialIconDimension.dp,
            viewportWidth = MaterialIconDimension,
            viewportHeight = MaterialIconDimension
        )
        .block()
        .build()

private inline fun ImageVector.Builder.materialPath(
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit
) =
    path(
        fill = SolidColor(Color.Black),
        fillAlpha = fillAlpha,
        stroke = null,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = pathFillType,
        pathBuilder = pathBuilder
    )

private const val MaterialIconDimension = 24f
