/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.PI

// A shader (as used by [Paint.shader]) that renders a color gradient.
//
// There are several types of gradients, represented by the various constructors
// on this class.
class Gradient private constructor(private val shader: android.graphics.Shader) : Shader() {

    companion object {
        /**
         * Creates a linear gradient from `from` to `to`.
         *
         * If `colorStops` is provided, `colorStops[i]` is a number from 0.0 to 1.0
         * that specifies where `color[i]` begins in the gradient. If `colorStops` is
         * not provided, then only two stops, at 0.0 and 1.0, are implied (and
         * `color` must therefore only have two entries).
         *
         * The behavior before `from` and after `to` is described by the `tileMode`
         * argument. For details, see the [TileMode] enum.
         *
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_clamp_linear.png)
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_mirror_linear.png)
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_repeated_linear.png)
         *
         * If `from`, `to`, `colors`, or `tileMode` are null, or if `colors` or
         * `colorStops` contain null values, this constructor will throw a
         * [NoSuchMethodError].
         */
        //    Gradient.linear(
        fun linear(
            from: Offset,
            to: Offset,
            colors: List<Color>,
            colorStops: List<Float>? = null,
            tileMode: TileMode = TileMode.clamp
        ): Gradient {
            _validateColorStops(colors, colorStops)
            val linearGradient = android.graphics.LinearGradient(
                from.dx,
                from.dy,
                to.dx,
                to.dy,
                toIntArray(colors),
                toFloatArray(colorStops),
                toFrameworkTileMode(tileMode)
            )
            return Gradient(linearGradient)
        }

        /**
         * Creates a radial gradient centered at `center` that ends at `radius`
         * distance from the center.
         *
         * If `colorStops` is provided, `colorStops[i]` is a number from 0.0 to 1.0
         * that specifies where `color[i]` begins in the gradient. If `colorStops` is
         * not provided, then only two stops, at 0.0 and 1.0, are implied (and
         * `color` must therefore only have two entries).
         *
         * The behavior before and after the radius is described by the `tileMode`
         * argument. For details, see the [TileMode] enum.
         *
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_clamp_radial.png)
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_mirror_radial.png)
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_repeated_radial.png)
         *
         * If `center`, `radius`, `colors`, or `tileMode` are null, or if `colors` or
         * `colorStops` contain null values, this constructor will throw a
         * [NoSuchMethodError].
         *
         * If `matrix4` is provided, the gradient fill will be transformed by the
         * specified 4x4 matrix relative to the local coordinate system. `matrix4` must
         * be a column-major matrix packed into a list of 16 values.
         *
         * If `focal` is provided and not equal to `center` and `focalRadius` is
         * provided and not equal to 0.0, the generated shader will be a two point
         * conical radial gradient, with `focal` being the center of the focal
         * circle and `focalRadius` being the radius of that circle. If `focal` is
         * provided and not equal to `center`, at least one of the two offsets must
         * not be equal to [Offset.zero].
         */
        // TODO(Migration/njawad change matrix4 parameter to Float64List to match Flutter implementation)
        fun radial(
            center: Offset,
            radius: Float,
            color: List<Color>,
            colorStops: List<Float>?,
            tileMode: TileMode = TileMode.clamp,
            @Suppress("UNUSED_PARAMETER") matrix4: Matrix4,
            focal: Offset?,
            focalRadius: Float
        ): Gradient {
            _validateColorStops(color, colorStops)
            if (focal == null || (focal == center && focalRadius == 0.0f)) {
                TODO("Migration/njawad: add focal support to RadialGradient in framework")
            } else {
                // TODO(Migration/njawad use matrix parameter in creation of RadialGradient)
                val radial = android.graphics.RadialGradient(
                    center.dx,
                    center.dy,
                    radius,
                    toIntArray(color),
                    toFloatArray(colorStops),
                    toFrameworkTileMode(tileMode)
                )
                return Gradient(radial)
            }
        }

        /**
         * Creates a sweep gradient centered at `center` that starts at `startAngle`
         * and ends at `endAngle`.
         *
         * `startAngle` and `endAngle` should be provided in radians, with zero
         * radians being the horizontal line to the right of the `center` and with
         * positive angles going clockwise around the `center`.
         *
         * If `colorStops` is provided, `colorStops[i]` is a number from 0.0 to 1.0
         * that specifies where `color[i]` begins in the gradient. If `colorStops` is
         * not provided, then only two stops, at 0.0 and 1.0, are implied (and
         * `color` must therefore only have two entries).
         *
         * The behavior before `startAngle` and after `endAngle` is described by the
         * `tileMode` argument. For details, see the [TileMode] enum.
         *
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_clamp_sweep.png)
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_mirror_sweep.png)
         * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/tile_mode_repeated_sweep.png)
         *
         * If `center`, `colors`, `tileMode`, `startAngle`, or `endAngle` are null,
         * or if `colors` or `colorStops` contain null values, this constructor will
         * throw a [NoSuchMethodError].
         *
         * If `matrix4` is provided, the gradient fill will be transformed by the
         * specified 4x4 matrix relative to the local coordinate system. `matrix4` must
         * be a column-major matrix packed into a list of 16 values.
         */
        // TODO(Migration/njawad change matrix4 parameter to Float64List to match Flutter implementation)
        @Suppress("UNUSED_PARAMETER")
        fun sweep(
            center: Offset,
            colors: List<Color>,
            colorStops: List<Float>,
            tileMode: TileMode = TileMode.clamp,
            startAngle: Float,
            endAngle: Float = PI * 2,
            matrix4: Matrix4
        ): Gradient {
            _validateColorStops(colors, colorStops)
            // TODO(Migration/njawad framework SweepGradient does not support angle ranges/TileModes)
            val sweep = android.graphics.SweepGradient(
                center.dx, center.dy,
                toIntArray(colors),
                toFloatArray(colorStops)
            )
            return Gradient(sweep)
        }

        private fun toFrameworkTileMode(tileMode: TileMode): android.graphics.Shader.TileMode {
            return when (tileMode) {
                TileMode.clamp -> android.graphics.Shader.TileMode.CLAMP
                TileMode.mirror -> android.graphics.Shader.TileMode.MIRROR
                TileMode.repeated -> android.graphics.Shader.TileMode.REPEAT
            }
        }

        private fun toFloatArray(stops: List<Float>?): FloatArray? {
            return if (stops != null) {
                FloatArray(stops.size) { i ->
                    stops[i]
                }
            } else {
                return null
            }
        }

        private fun toIntArray(colors: List<Color>): IntArray {
            return IntArray(colors.size) { i ->
                colors[i].toArgb()
            }
        }

        private fun _validateColorStops(colors: List<Color>, colorStops: List<Float>?) {
            if (colorStops == null) {
                if (colors.size != 2) {
                    throw IllegalArgumentException(
                        "colors must have length 2 if colorStops " +
                                "is omitted."
                    )
                }
            } else if (colors.size != colorStops.size) {
                throw IllegalArgumentException(
                    "colors and colorStops arguments must have" +
                            " equal length."
                )
            }
        }
    }

    override fun toFrameworkShader(): android.graphics.Shader {
        return shader
    }
}
