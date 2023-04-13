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
 * on either an inner or outer radius. The two radii specified in the constructor must both
 * both nonzero. If the radii are equal, the result will be a regular (not star) polygon with twice
 * the number of vertices specified in [numVerticesPerRadius].
 *
 * @param numVerticesPerRadius The number of vertices along each of the two radii.
 * @param radius Outer radius for this star shape, must be greater than 0. Default value is 1.
 * @param innerRadius Inner radius for this star shape, must be greater than 0 and less
 * than or equal to [radius]. Note that equal radii would be the same as creating a
 * [RoundedPolygon] directly, but with 2 * [numVerticesPerRadius] vertices. Default value is .5.
 * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
 * have different rounding properties, then use [perVertexRounding] instead. The default
 * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
 * themselves in the final shape and not curves rounded around the vertices.
 * @param innerRounding Optional rounding parameters for the vertices on the [innerRadius]. If
 * null (the default value), inner vertices will use the [rounding] or [perVertexRounding]
 * parameters instead.
 * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
 * parameter is not null, then it must have the same size as 2 * [numVerticesPerRadius]. If this
 * parameter is null, then the polygon will use the [rounding] parameter for every vertex instead.
 * The default value is null.
 * @param center The center of the polygon, around which all vertices will be placed. The
 * default center is at (0,0).
 *
 * @throws IllegalArgumentException if either [radius] or [innerRadius] are <= 0 or
 * [innerRadius] > [radius].
 */
@JvmOverloads
fun Star(
    numVerticesPerRadius: Int,
    radius: Float = 1f,
    innerRadius: Float = .5f,
    rounding: CornerRounding = CornerRounding.Unrounded,
    innerRounding: CornerRounding? = null,
    perVertexRounding: List<CornerRounding>? = null,
    center: PointF = Zero
): RoundedPolygon {
    if (radius <= 0f || innerRadius <= 0f) {
        throw IllegalArgumentException("Star radii must both be greater than 0")
    }
    if (innerRadius >= radius) {
        throw IllegalArgumentException("innerRadius must be less than radius")
    }

    var pvRounding = perVertexRounding
    // If no per-vertex rounding supplied and caller asked for inner rounding,
    // create per-vertex rounding list based on supplied outer/inner rounding parameters
    if (pvRounding == null && innerRounding != null) {
        pvRounding = (0 until numVerticesPerRadius).flatMap {
            listOf(rounding, innerRounding)
        }
    }

    // Star polygon is just a polygon with all vertices supplied (where we generate
    // those vertices to be on the inner/outer radii)
    return RoundedPolygon((0 until numVerticesPerRadius).flatMap {
        listOf(
            radialToCartesian(radius, (FloatPi / numVerticesPerRadius * 2 * it), center),
            radialToCartesian(innerRadius, FloatPi / numVerticesPerRadius * (2 * it + 1), center)
        )
    }, rounding, pvRounding, center)
}
