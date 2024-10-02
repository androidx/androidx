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

package androidx.wear.compose.material3.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object Icons {
    internal val Add: ImageVector
        get() {
            if (_add != null) {
                return _add!!
            }
            _add =
                materialIcon(name = "Add") {
                    materialPath {
                        moveTo(440f, 520f)
                        lineTo(240f, 520f)
                        quadTo(223f, 520f, 211.5f, 508.5f)
                        quadTo(200f, 497f, 200f, 480f)
                        quadTo(200f, 463f, 211.5f, 451.5f)
                        quadTo(223f, 440f, 240f, 440f)
                        lineTo(440f, 440f)
                        lineTo(440f, 240f)
                        quadTo(440f, 223f, 451.5f, 211.5f)
                        quadTo(463f, 200f, 480f, 200f)
                        quadTo(497f, 200f, 508.5f, 211.5f)
                        quadTo(520f, 223f, 520f, 240f)
                        lineTo(520f, 440f)
                        lineTo(720f, 440f)
                        quadTo(737f, 440f, 748.5f, 451.5f)
                        quadTo(760f, 463f, 760f, 480f)
                        quadTo(760f, 497f, 748.5f, 508.5f)
                        quadTo(737f, 520f, 720f, 520f)
                        lineTo(520f, 520f)
                        lineTo(520f, 720f)
                        quadTo(520f, 737f, 508.5f, 748.5f)
                        quadTo(497f, 760f, 480f, 760f)
                        quadTo(463f, 760f, 451.5f, 748.5f)
                        quadTo(440f, 737f, 440f, 720f)
                        lineTo(440f, 520f)
                        close()
                    }
                }
            return _add!!
        }

    private var _add: ImageVector? = null

    internal val Remove: ImageVector
        get() {
            if (_remove != null) {
                return _remove!!
            }
            _remove =
                materialIcon(name = "Remove") {
                    materialPath {
                        moveTo(240f, 520f)
                        quadTo(223f, 520f, 211.5f, 508.5f)
                        quadTo(200f, 497f, 200f, 480f)
                        quadTo(200f, 463f, 211.5f, 451.5f)
                        quadTo(223f, 440f, 240f, 440f)
                        lineTo(720f, 440f)
                        quadTo(737f, 440f, 748.5f, 451.5f)
                        quadTo(760f, 463f, 760f, 480f)
                        quadTo(760f, 497f, 748.5f, 508.5f)
                        quadTo(737f, 520f, 720f, 520f)
                        lineTo(240f, 520f)
                        close()
                    }
                }
            return _remove!!
        }

    private var _remove: ImageVector? = null

    internal val Check: ImageVector
        get() {
            if (_check != null) {
                return _check!!
            }
            _check =
                materialIcon(name = "Check") {
                    materialPath {
                        moveTo(382f, 597.87f)
                        lineTo(716.7f, 263.17f)
                        quadTo(730.37f, 249.5f, 748.76f, 249.5f)
                        quadTo(767.15f, 249.5f, 780.83f, 263.17f)
                        quadTo(794.5f, 276.85f, 794.5f, 295.62f)
                        quadTo(794.5f, 314.39f, 780.83f, 328.07f)
                        lineTo(414.07f, 695.59f)
                        quadTo(400.39f, 709.26f, 382f, 709.26f)
                        quadTo(363.61f, 709.26f, 349.93f, 695.59f)
                        lineTo(178.41f, 524.07f)
                        quadTo(164.74f, 510.39f, 165.12f, 491.62f)
                        quadTo(165.5f, 472.85f, 179.17f, 459.17f)
                        quadTo(192.85f, 445.5f, 211.62f, 445.5f)
                        quadTo(230.39f, 445.5f, 244.07f, 459.17f)
                        lineTo(382f, 597.87f)
                        close()
                    }
                }
            return _check!!
        }

    private var _check: ImageVector? = null

    internal object AutoMirrored {
        internal val KeyboardArrowRight: ImageVector
            get() {
                if (_keyboardArrowRight != null) {
                    return _keyboardArrowRight!!
                }
                _keyboardArrowRight =
                    materialIcon(
                        name = "AutoMirrored.KeyboardArrowRight",
                        autoMirror = true,
                    ) {
                        materialPath {
                            moveTo(496.35f, 480f)
                            lineTo(344.17f, 327.83f)
                            quadTo(331.5f, 315.15f, 331.5f, 296f)
                            quadTo(331.5f, 276.85f, 344.17f, 264.17f)
                            quadTo(356.85f, 251.5f, 376f, 251.5f)
                            quadTo(395.15f, 251.5f, 407.83f, 264.17f)
                            lineTo(591.59f, 447.93f)
                            quadTo(598.3f, 454.65f, 601.4f, 462.85f)
                            quadTo(604.5f, 471.04f, 604.5f, 480f)
                            quadTo(604.5f, 488.96f, 601.4f, 497.15f)
                            quadTo(598.3f, 505.35f, 591.59f, 512.07f)
                            lineTo(407.83f, 695.83f)
                            quadTo(395.15f, 708.5f, 376f, 708.5f)
                            quadTo(356.85f, 708.5f, 344.17f, 695.83f)
                            quadTo(331.5f, 683.15f, 331.5f, 664f)
                            quadTo(331.5f, 644.85f, 344.17f, 632.17f)
                            lineTo(496.35f, 480f)
                            close()
                        }
                    }
                return _keyboardArrowRight!!
            }

        private var _keyboardArrowRight: ImageVector? = null
    }
}

private inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> ImageVector.Builder
): ImageVector =
    ImageVector.Builder(
            name = name,
            defaultWidth = MaterialIconDimension,
            defaultHeight = MaterialIconDimension,
            viewportWidth = MaterialIconViewPortDimension,
            viewportHeight = MaterialIconViewPortDimension,
            autoMirror = autoMirror
        )
        .block()
        .build()

private inline fun ImageVector.Builder.materialPath(pathBuilder: PathBuilder.() -> Unit) =
    path(fill = SolidColor(Color.White), pathBuilder = pathBuilder)

private val MaterialIconDimension = 24.dp
private const val MaterialIconViewPortDimension = 960f
