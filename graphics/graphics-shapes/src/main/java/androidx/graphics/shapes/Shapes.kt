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
import androidx.annotation.IntRange
import kotlin.math.cos

/**
 * Creates a circular shape, approximating the rounding of the shape around the underlying
 * polygon vertices.
 *
 * @param numVertices The number of vertices in the underlying polygon with which to
 * approximate the circle, default value is 8
 * @param radius optional radius for the circle, default value is 1.0
 * @param center optional center for the circle, default value is (0, 0)
 *
 * @throws IllegalArgumentException [numVertices] must be at least 3
 */
@JvmOverloads
fun RoundedPolygon.Companion.circle(
    @IntRange(from = 3) numVertices: Int = 8,
    radius: Float = 1f,
    center: PointF = Zero
): RoundedPolygon {

    if (numVertices < 3) throw IllegalArgumentException("Circle must have at least three vertices")

    // Half of the angle between two adjacent vertices on the polygon
    val theta = FloatPi / numVertices
    // Radius of the underlying RoundedPolygon object given the desired radius of the circle
    val polygonRadius = radius / cos(theta)
    return RoundedPolygon(
        numVertices, rounding = CornerRounding(radius), radius = polygonRadius,
        center = center
    )
}

/**
 * Creates a rectangular shape with the given width/height around the given center.
 * Optional rounding parameters can be used to create a rounded rectangle instead.
 *
 * As with all [RoundedPolygon] objects, if this shape is created with default dimensions and
 * center, it is sized to fit within the 2x2 bounding box around a center of (0, 0) and will
 * need to be scaled and moved using [RoundedPolygon.transform] to fit the intended area
 * in a UI.
 *
 * @param width The width of the rectangle, default value is 2
 * @param height The height of the rectangle, default value is 2
 * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
 * have different rounding properties, then use [perVertexRounding] instead. The default
 * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
 * themselves in the final shape and not curves rounded around the vertices.
 * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
 * parameter is not null, then it must be of size 4 for the four corners of the shape. If this
 * parameter is null, then the polygon will use the [rounding] parameter for every vertex instead.
 * The default value is null.
 * @param center The center of the rectangle, around which all vertices will be placed
 * equidistantly. The default center is at (0,0).
 */
fun RoundedPolygon.Companion.rectangle(
    width: Float = 2f,
    height: Float = 2f,
    rounding: CornerRounding = CornerRounding.Unrounded,
    perVertexRounding: List<CornerRounding>? = null,
    center: PointF = Zero
): RoundedPolygon {
    val left = center.x - width / 2
    val top = center.y - height / 2
    val right = center.x + width / 2
    val bottom = center.y + height / 2

    return RoundedPolygon(
        listOf(
            PointF(right, bottom), PointF(left, bottom), PointF(left, top),
            PointF(right, top)
        ), rounding, perVertexRounding, center
    )
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
fun RoundedPolygon.Companion.star(
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
