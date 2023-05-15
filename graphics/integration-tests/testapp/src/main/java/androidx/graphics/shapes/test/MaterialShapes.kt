/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.shapes.test

import android.graphics.PointF
import androidx.core.graphics.plus
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import kotlin.math.cos
import kotlin.math.sin

/**
 * This class holds standard Material shape design implementations.
 */
class MaterialShapes {
    companion object {

        private val FloatPI = Math.PI.toFloat()

        // TODO: remove this when it is integrated into Ktx
        operator fun PointF.times(factor: Float): PointF {
            return PointF(this.x * factor, this.y * factor)
        }

        private val SquarePoints = listOf(
            PointF(1f, 1f),
            PointF(-1f, 1f),
            PointF(-1f, -1f),
            PointF(1f, -1f)
        )
        internal fun Float.toRadians(): Float {
            return this / 360f * 2 * FloatPI
        }

        private val Zero = PointF(0f, 0f)

        internal fun directionVector(angleRadians: Float) =
            PointF(cos(angleRadians), sin(angleRadians))

        private fun radialToCartesian(radius: Float, angleRadians: Float, center: PointF = Zero) =
            directionVector(angleRadians) * radius + center

        @JvmStatic
        fun triangleChip(radiusRatio: Float, rounding: CornerRounding): RoundedPolygon {
            val points = listOf(
                radialToCartesian(1f, 270f.toRadians()),
                radialToCartesian(1f, 30f.toRadians()),
                radialToCartesian(radiusRatio, 90f.toRadians()),
                radialToCartesian(1f, 150f.toRadians()),
            )
            return RoundedPolygon(points, rounding)
        }

        @JvmOverloads
        @JvmStatic
        fun quarty(roundnessRatio: Float, smooth: Float = 0f): RoundedPolygon {
            return RoundedPolygon(
                SquarePoints,
                perVertexRounding = listOf(
                    CornerRounding(), CornerRounding(), CornerRounding(),
                    CornerRounding(roundnessRatio, smooth)
                )
            )
        }

        @JvmOverloads
        @JvmStatic
        fun blobR(radiusRatio: Float, roundnessRatio: Float, smooth: Float = 0f): RoundedPolygon {
            return RoundedPolygon(listOf(
                PointF(-radiusRatio, -roundnessRatio),
                PointF(radiusRatio, -roundnessRatio),
                PointF(radiusRatio, roundnessRatio),
                PointF(-radiusRatio, roundnessRatio),
            ), CornerRounding(roundnessRatio, smooth)
            )
        }

        @JvmOverloads
        @JvmStatic
        fun cornerSouthEast(roundnessRatio: Float, smooth: Float = 0f): RoundedPolygon {
            return RoundedPolygon(
                SquarePoints,
                perVertexRounding = listOf(
                    CornerRounding(roundnessRatio, smooth), CornerRounding(), CornerRounding(),
                    CornerRounding()
                )
            )
        }

        @JvmStatic
        fun scallop(): RoundedPolygon {
            return RoundedPolygon.star(12, innerRadius = .928f,
                rounding = CornerRounding(radius = .928f))
        }

        @JvmOverloads
        @JvmStatic
        fun clover(
            rounding: Float = .32f,
            innerRadius: Float = .352f,
            innerRounding: CornerRounding? = null,
            scale: Float = 1f
        ): RoundedPolygon {
            val poly = RoundedPolygon.star(4, innerRadius = innerRadius,
                rounding = CornerRounding(rounding * scale),
                innerRounding = innerRounding)
            return poly
        }

        @JvmStatic
        fun alice(): RoundedPolygon {
            return triangleChip(0.1f, CornerRounding(.22f))
        }

        @JvmStatic
        fun wiggleStar(): RoundedPolygon {
            return RoundedPolygon.star(8, .784f, rounding = CornerRounding(.82f))
        }

        @JvmStatic
        fun wovel(): RoundedPolygon {
            return RoundedPolygon.star(15, .892f, rounding = CornerRounding(1f))
        }

        @JvmStatic
        fun more(): RoundedPolygon {
            return RoundedPolygon(numVertices = 3, rounding = CornerRounding(.2f))
        }

        @JvmStatic
        fun cube5D(): RoundedPolygon {
            return RoundedPolygon(numVertices = 6, rounding = CornerRounding(.3f))
        }

        @JvmStatic
        fun pentagon(): RoundedPolygon {
            return RoundedPolygon(numVertices = 5, rounding = CornerRounding(.3f))
        }
    }
}