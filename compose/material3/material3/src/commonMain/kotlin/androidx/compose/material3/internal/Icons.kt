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
                            autoMirror = true,
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
                            autoMirror = true,
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

        internal val MoreVert: ImageVector
            get() {
                if (_moreVert != null) {
                    return _moreVert!!
                }
                _moreVert =
                    materialIcon(name = "Filled.MoreVert") {
                        materialPath {
                            moveTo(12.0f, 8.0f)
                            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                            close()
                            moveTo(12.0f, 10.0f)
                            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                            close()
                            moveTo(12.0f, 16.0f)
                            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                            close()
                        }
                    }
                return _moreVert!!
            }

        private var _moreVert: ImageVector? = null
    }

    internal object Outlined {

        val Schedule: ImageVector
            get() {
                if (_schedule != null) {
                    return _schedule!!
                }
                _schedule =
                    materialIcon(name = "Outlined.Schedule") {
                        materialPath {
                            moveTo(11.99f, 2.0f)
                            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
                            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
                            close()
                            moveTo(12.0f, 20.0f)
                            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
                            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
                            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
                            reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
                            close()
                            moveTo(12.5f, 7.0f)
                            lineTo(11.0f, 7.0f)
                            verticalLineToRelative(6.0f)
                            lineToRelative(5.25f, 3.15f)
                            lineToRelative(0.75f, -1.23f)
                            lineToRelative(-4.5f, -2.67f)
                            close()
                        }
                    }
                return _schedule!!
            }

        private var _schedule: ImageVector? = null

        val Keyboard: ImageVector
            get() {
                if (_keyboard != null) {
                    return _keyboard!!
                }
                _keyboard =
                    materialIcon(name = "Outlined.Keyboard") {
                        materialPath {
                            moveTo(20.0f, 7.0f)
                            verticalLineToRelative(10.0f)
                            lineTo(4.0f, 17.0f)
                            lineTo(4.0f, 7.0f)
                            horizontalLineToRelative(16.0f)
                            moveToRelative(0.0f, -2.0f)
                            lineTo(4.0f, 5.0f)
                            curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                            lineTo(2.0f, 17.0f)
                            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                            horizontalLineToRelative(16.0f)
                            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                            lineTo(22.0f, 7.0f)
                            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                            close()
                            moveTo(11.0f, 8.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(-2.0f)
                            close()
                            moveTo(11.0f, 11.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(-2.0f)
                            close()
                            moveTo(8.0f, 8.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(8.0f, 10.0f)
                            close()
                            moveTo(8.0f, 11.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(8.0f, 13.0f)
                            close()
                            moveTo(5.0f, 11.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(5.0f, 13.0f)
                            close()
                            moveTo(5.0f, 8.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(5.0f, 10.0f)
                            close()
                            moveTo(8.0f, 14.0f)
                            horizontalLineToRelative(8.0f)
                            verticalLineToRelative(2.0f)
                            lineTo(8.0f, 16.0f)
                            close()
                            moveTo(14.0f, 11.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(-2.0f)
                            close()
                            moveTo(14.0f, 8.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(-2.0f)
                            close()
                            moveTo(17.0f, 11.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(-2.0f)
                            close()
                            moveTo(17.0f, 8.0f)
                            horizontalLineToRelative(2.0f)
                            verticalLineToRelative(2.0f)
                            horizontalLineToRelative(-2.0f)
                            close()
                        }
                    }
                return _keyboard!!
            }

        private var _keyboard: ImageVector? = null

        val SwipeVertical: ImageVector
            get() {
                if (_swipe_vertical != null) {
                    return _swipe_vertical!!
                }
                _swipe_vertical =
                    ImageVector.Builder(
                            name = "swipe_vertical",
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
                                moveTo(2f, 22f)
                                verticalLineTo(20.5f)
                                horizontalLineTo(4.03f)
                                quadTo(2.55f, 18.7f, 1.78f, 16.52f)
                                reflectiveQuadTo(1f, 12f)
                                reflectiveQuadTo(1.78f, 7.47f)
                                reflectiveQuadTo(4.03f, 3.5f)
                                horizontalLineTo(2f)
                                verticalLineTo(2f)
                                horizontalLineTo(7f)
                                verticalLineTo(7f)
                                horizontalLineTo(5.5f)
                                verticalLineTo(4.1f)
                                quadTo(4.05f, 5.75f, 3.28f, 7.77f)
                                reflectiveQuadTo(2.5f, 12f)
                                reflectiveQuadToRelative(0.78f, 4.23f)
                                reflectiveQuadTo(5.5f, 19.9f)
                                verticalLineTo(17f)
                                horizontalLineTo(7f)
                                verticalLineToRelative(5f)
                                horizontalLineTo(2f)
                                close()
                                moveTo(16.45f, 20.83f)
                                quadToRelative(-0.57f, 0.2f, -1.16f, 0.19f)
                                reflectiveQuadTo(14.15f, 20.73f)
                                lineTo(7.6f, 17.68f)
                                lineToRelative(0.45f, -1f)
                                quadToRelative(0.25f, -0.5f, 0.7f, -0.81f)
                                reflectiveQuadToRelative(1f, -0.36f)
                                lineToRelative(1.7f, -0.13f)
                                lineTo(8.65f, 7.7f)
                                quadTo(8.5f, 7.3f, 8.68f, 6.94f)
                                reflectiveQuadTo(9.25f, 6.43f)
                                quadToRelative(0.4f, -0.15f, 0.76f, 0.02f)
                                reflectiveQuadToRelative(0.51f, 0.57f)
                                lineToRelative(3.7f, 10.18f)
                                lineToRelative(-2.5f, 0.18f)
                                lineTo(15f, 18.9f)
                                quadToRelative(0.18f, 0.08f, 0.38f, 0.09f)
                                reflectiveQuadToRelative(0.38f, -0.04f)
                                lineToRelative(3.93f, -1.43f)
                                quadToRelative(0.78f, -0.27f, 1.13f, -1.04f)
                                reflectiveQuadToRelative(0.07f, -1.54f)
                                lineTo(19.5f, 11.2f)
                                quadToRelative(-0.15f, -0.4f, 0.03f, -0.76f)
                                quadTo(19.7f, 10.07f, 20.1f, 9.92f)
                                reflectiveQuadToRelative(0.76f, 0.03f)
                                reflectiveQuadToRelative(0.51f, 0.57f)
                                lineToRelative(1.38f, 3.75f)
                                quadToRelative(0.58f, 1.58f, -0.11f, 3.06f)
                                quadToRelative(-0.69f, 1.49f, -2.26f, 2.06f)
                                lineToRelative(-3.93f, 1.43f)
                                close()
                                moveTo(14.2f, 14.2f)
                                lineTo(12.85f, 10.43f)
                                quadTo(12.7f, 10.02f, 12.88f, 9.66f)
                                reflectiveQuadTo(13.45f, 9.15f)
                                reflectiveQuadToRelative(0.76f, 0.03f)
                                reflectiveQuadToRelative(0.51f, 0.58f)
                                lineTo(16.1f, 13.5f)
                                lineToRelative(-1.9f, 0.7f)
                                close()
                                moveToRelative(2.83f, -1.03f)
                                lineTo(16f, 10.35f)
                                quadTo(15.85f, 9.95f, 16.03f, 9.59f)
                                quadTo(16.2f, 9.23f, 16.6f, 9.07f)
                                reflectiveQuadTo(17.36f, 9.1f)
                                quadToRelative(0.36f, 0.17f, 0.51f, 0.57f)
                                lineToRelative(1.03f, 2.8f)
                                lineToRelative(-1.88f, 0.7f)
                                close()
                                moveToRelative(0.2f, 2.2f)
                                close()
                            }
                        }
                        .build()
                return _swipe_vertical!!
            }

        private var _swipe_vertical: ImageVector? = null
    }
}

private inline fun materialIcon(
    name: String,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector =
    ImageVector.Builder(
            name = name,
            defaultWidth = MaterialIconDimension.dp,
            defaultHeight = MaterialIconDimension.dp,
            viewportWidth = MaterialIconDimension,
            viewportHeight = MaterialIconDimension,
        )
        .block()
        .build()

private inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector =
    ImageVector.Builder(
            name = name,
            defaultWidth = MaterialIconDimension.dp,
            defaultHeight = MaterialIconDimension.dp,
            viewportWidth = MaterialIconDimension,
            viewportHeight = MaterialIconDimension,
            autoMirror = autoMirror,
        )
        .block()
        .build()

private inline fun ImageVector.Builder.materialPath(
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit,
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
        pathBuilder = pathBuilder,
    )

// All Material icons (currently) are 24dp by 24dp, with a viewport size of 24 by 24.
private const val MaterialIconDimension = 24f
