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

package androidx.graphics.shapes

import android.graphics.PointF
import androidx.annotation.FloatRange

private const val Sqrt2 = 1.41421356f

/**
 * Circle creates a square polygon shape whose four corners are rounded with circular
 * arcs.
 *
 * @param radius optional radius for the circle, default value is 1.0.
 * @param center optional center for the circle, default value is (0, 0)
 */
@JvmOverloads
fun Circle(radius: Float = 1f, center: PointF = Zero): RoundedPolygon {
    return RoundedPolygon(4, rounding = CornerRounding(radius), radius = radius * Sqrt2,
        center = center)
}

/**
 * Creates a star polygon, which is like a regular polygon except every other vertex is
 * on either an inner or outer radius. The two radii are specified in the constructor by
 * providing the [innerRadiusRatio], which is a value representing the proportion (from
 * 0 to 1, non-inclusive) of the inner radius compared to the outer one.
 * @throws IllegalArgumentException if radius ratio not in [0,1]
 */
@JvmOverloads
fun Star(
    numOuterVertices: Int,
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    innerRadiusRatio: Float,
    radius: Float = 1f,
    rounding: CornerRounding = CornerRounding.Unrounded,
    innerRounding: CornerRounding? = null,
    perVertexRounding: List<CornerRounding>? = null,
    center: PointF = Zero
): RoundedPolygon {
    if (innerRadiusRatio <= 0f || innerRadiusRatio >= 1f) {
        throw IllegalArgumentException("Inner radius ratio must be in range (0,1), exclusive" +
            "of 0 and 1")
    }

    var pvRounding = perVertexRounding
    // If no per-vertex rounding supplied and caller asked for inner rounding,
    // create per-vertex rounding list based on supplied outer/inner rounding parameters
    if (pvRounding == null && innerRounding != null) {
        pvRounding = (0 until numOuterVertices).flatMap {
            listOf(rounding, innerRounding)
        }
    }
    // Star polygon is just a polygon with all vertices supplied (where we generate
    // those vertices to be on the inner/outer radii)
    return RoundedPolygon((0 until numOuterVertices).flatMap {
        listOf(
            radialToCartesian(radius, (FloatPi / numOuterVertices * 2 * it), center),
            radialToCartesian(radius * innerRadiusRatio,
                FloatPi / numOuterVertices * (2 * it + 1), center)
        )
    }, rounding, pvRounding, center)
}
