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

package androidx.compose.material3.internal

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
    internal object AutoMirrored {
        internal object Filled {
            internal val KeyboardArrowLeft: ImageVector
                get() {
                    if (_keyboardArrowLeft != null) {
                        return _keyboardArrowLeft!!
                    }
                    _keyboardArrowLeft =
                        materialIcon(
                            name = "AutoMirrored.Filled.KeyboardArrowLeft",
                            autoMirror = true
                        ) {
                            materialPath {
                                moveTo(15.41f, 16.59f)
                                lineTo(10.83f, 12.0f)
                                lineToRelative(4.58f, -4.59f)
                                lineTo(14.0f, 6.0f)
                                lineToRelative(-6.0f, 6.0f)
                                lineToRelative(6.0f, 6.0f)
                                lineToRelative(1.41f, -1.41f)
                                close()
                            }
                        }
                    return _keyboardArrowLeft!!
                }

            private var _keyboardArrowLeft: ImageVector? = null

            internal val KeyboardArrowRight: ImageVector
                get() {
                    if (_keyboardArrowRight != null) {
                        return _keyboardArrowRight!!
                    }
                    _keyboardArrowRight =
                        materialIcon(
                            name = "AutoMirrored.Filled.KeyboardArrowRight",
                            autoMirror = true
                        ) {
                            materialPath {
                                moveTo(8.59f, 16.59f)
                                lineTo(13.17f, 12.0f)
                                lineTo(8.59f, 7.41f)
                                lineTo(10.0f, 6.0f)
                                lineToRelative(6.0f, 6.0f)
                                lineToRelative(-6.0f, 6.0f)
                                lineToRelative(-1.41f, -1.41f)
                                close()
                            }
                        }
                    return _keyboardArrowRight!!
                }

            private var _keyboardArrowRight: ImageVector? = null
        }
    }

    internal object Filled {
        internal val Close: ImageVector
            get() {
                if (_close != null) {
                    return _close!!
                }
                _close =
                    materialIcon(name = "Filled.Close") {
                        materialPath {
                            moveTo(19.0f, 6.41f)
                            lineTo(17.59f, 5.0f)
                            lineTo(12.0f, 10.59f)
                            lineTo(6.41f, 5.0f)
                            lineTo(5.0f, 6.41f)
                            lineTo(10.59f, 12.0f)
                            lineTo(5.0f, 17.59f)
                            lineTo(6.41f, 19.0f)
                            lineTo(12.0f, 13.41f)
                            lineTo(17.59f, 19.0f)
                            lineTo(19.0f, 17.59f)
                            lineTo(13.41f, 12.0f)
                            close()
                        }
                    }
                return _close!!
            }

        private var _close: ImageVector? = null

        internal val Check: ImageVector
            get() {
                if (_check != null) {
                    return _check!!
                }
                _check =
                    materialIcon(name = "Filled.Check") {
                        materialPath {
                            moveTo(9.0f, 16.17f)
                            lineTo(4.83f, 12.0f)
                            lineToRelative(-1.42f, 1.41f)
                            lineTo(9.0f, 19.0f)
                            lineTo(21.0f, 7.0f)
                            lineToRelative(-1.41f, -1.41f)
                            close()
                        }
                    }
                return _check!!
            }

        private var _check: ImageVector? = null

        internal val Edit: ImageVector
            get() {
                if (_edit != null) {
                    return _edit!!
                }
                _edit =
                    materialIcon(name = "Filled.Edit") {
                        materialPath {
                            moveTo(3.0f, 17.25f)
                            verticalLineTo(21.0f)
                            horizontalLineToRelative(3.75f)
                            lineTo(17.81f, 9.94f)
                            lineToRelative(-3.75f, -3.75f)
                            lineTo(3.0f, 17.25f)
                            close()
                            moveTo(20.71f, 7.04f)
                            curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                            lineToRelative(-2.34f, -2.34f)
                            curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                            lineToRelative(-1.83f, 1.83f)
                            lineToRelative(3.75f, 3.75f)
                            lineToRelative(1.83f, -1.83f)
                            close()
                        }
                    }
                return _edit!!
            }

        private var _edit: ImageVector? = null

        internal val DateRange: ImageVector
            get() {
                if (_dateRange != null) {
                    return _dateRange!!
                }
                _dateRange =
                    materialIcon(name = "Filled.DateRange") {
                        materialPath {
                            moveTo(9.0f, 11.0f)
                            lineTo(7.0f, 11.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(-2.0f)
                            close()
                            moveTo(13.0f, 11.0f)
                            horizontalLineToRelative(-2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(-2.0f)
                            close()
                            moveTo(17.0f, 11.0f)
                            horizontalLineToRelative(-2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(-2.0f)
                            close()
                            moveTo(19.0f, 4.0f)
                            horizontalLineToRelative(-1.0f)
                            lineTo(18.0f, 2.0f)
                            horizontalLineToRelative(-2.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(8.0f, 4.0f)
                            lineTo(8.0f, 2.0f)
                            lineTo(6.0f, 2.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(5.0f, 4.0f)
                            curveToRelative(-1.11f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                            lineTo(3.0f, 20.0f)
                            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                            horizontalLineToRelative(14.0f)
                            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                            lineTo(21.0f, 6.0f)
                            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                            close()
                            moveTo(19.0f, 20.0f)
                            lineTo(5.0f, 20.0f)
                            lineTo(5.0f, 9.0f)
                            horizontalLineToRelative(14.0f)
                            verticalLineToRelative(11.0f)
                            close()
                        }
                    }
                return _dateRange!!
            }

        private var _dateRange: ImageVector? = null

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

private inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder
): ImageVector =
    ImageVector.Builder(
            name = name,
            defaultWidth = MaterialIconDimension.dp,
            defaultHeight = MaterialIconDimension.dp,
            viewportWidth = MaterialIconDimension,
            viewportHeight = MaterialIconDimension,
            autoMirror = autoMirror
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

// All Material icons (currently) are 24dp by 24dp, with a viewport size of 24 by 24.
private const val MaterialIconDimension = 24f
