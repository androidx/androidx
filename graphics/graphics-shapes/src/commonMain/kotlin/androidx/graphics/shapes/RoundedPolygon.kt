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

import androidx.annotation.IntRange
import androidx.collection.MutableFloatList
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The RoundedPolygon class allows simple construction of polygonal shapes with optional rounding at
 * the vertices. Polygons can be constructed with either the number of vertices desired or an
 * ordered list of vertices.
 */
class RoundedPolygon
internal constructor(internal val features: List<Feature>, val centerX: Float, val centerY: Float) {
    /** A flattened version of the [Feature]s, as a List<Cubic>. */
    val cubics = buildList {
        // The first/last mechanism here ensures that the final anchor point in the shape
        // exactly matches the first anchor point. There can be rendering artifacts introduced
        // by those points being slightly off, even by much less than a pixel
        var firstCubic: Cubic? = null
        var lastCubic: Cubic? = null
        var firstFeatureSplitStart: List<Cubic>? = null
        var firstFeatureSplitEnd: List<Cubic>? = null
        if (features.size > 0 && features[0].cubics.size == 3) {
            val centerCubic = features[0].cubics[1]
            val (start, end) = centerCubic.split(.5f)
            firstFeatureSplitStart = mutableListOf(features[0].cubics[0], start)
            firstFeatureSplitEnd = mutableListOf(end, features[0].cubics[2])
        }
        // iterating one past the features list size allows us to insert the initial split
        // cubic if it exists
        for (i in 0..features.size) {
            val featureCubics =
                if (i == 0 && firstFeatureSplitEnd != null) firstFeatureSplitEnd
                else if (i == features.size) {
                    if (firstFeatureSplitStart != null) firstFeatureSplitStart else break
                } else features[i].cubics
            for (j in featureCubics.indices) {
                // Skip zero-length curves; they add nothing and can trigger rendering artifacts
                val cubic = featureCubics[j]
                if (!cubic.zeroLength()) {
                    if (lastCubic != null) add(lastCubic)
                    lastCubic = cubic
                    if (firstCubic == null) firstCubic = cubic
                } else {
                    if (lastCubic != null) {
                        // Dropping several zero-ish length curves in a row can lead to
                        // enough discontinuity to throw an exception later, even though the
                        // distances are quite small. Account for that by making the last
                        // cubic use the latest anchor point, always.
                        lastCubic = Cubic(lastCubic.points.copyOf()) // Make a copy before mutating
                        lastCubic.points[6] = cubic.anchor1X
                        lastCubic.points[7] = cubic.anchor1Y
                    }
                }
            }
        }
        if (lastCubic != null && firstCubic != null) {
            add(
                Cubic(
                    lastCubic.anchor0X,
                    lastCubic.anchor0Y,
                    lastCubic.control0X,
                    lastCubic.control0Y,
                    lastCubic.control1X,
                    lastCubic.control1Y,
                    firstCubic.anchor0X,
                    firstCubic.anchor0Y
                )
            )
        } else {
            // Empty / 0-sized polygon.
            add(
                Cubic(
                    centerX,
                    centerY,
                    centerX,
                    centerY,
                    centerX,
                    centerY,
                    centerX,
                    centerY,
                )
            )
        }
    }

    init {
        var prevCubic = cubics[cubics.size - 1]
        debugLog("RoundedPolygon") { "Cubic-1 = $prevCubic" }
        for (index in cubics.indices) {
            val cubic = cubics[index]
            debugLog("RoundedPolygon") { "Cubic = $cubic" }
            if (
                abs(cubic.anchor0X - prevCubic.anchor1X) > DistanceEpsilon ||
                    abs(cubic.anchor0Y - prevCubic.anchor1Y) > DistanceEpsilon
            ) {
                debugLog("RoundedPolygon") {
                    "Ix: $index | (${cubic.anchor0X},${cubic.anchor0Y}) vs $prevCubic"
                }
                throw IllegalArgumentException(
                    "RoundedPolygon must be contiguous, with the anchor points of all curves " +
                        "matching the anchor points of the preceding and succeeding cubics"
                )
            }
            prevCubic = cubic
        }
    }

    /**
     * Transforms (scales/translates/etc.) this [RoundedPolygon] with the given [PointTransformer]
     * and returns a new [RoundedPolygon]. This is a low level API and there should be more platform
     * idiomatic ways to transform a [RoundedPolygon] provided by the platform specific wrapper.
     *
     * @param f The [PointTransformer] used to transform this [RoundedPolygon]
     */
    fun transformed(f: PointTransformer): RoundedPolygon {
        val center = Point(centerX, centerY).transformed(f)
        return RoundedPolygon(
            buildList {
                for (i in features.indices) {
                    add(features[i].transformed(f))
                }
            },
            center.x,
            center.y
        )
    }

    /**
     * Creates a new RoundedPolygon, moving and resizing this one, so it's completely inside the
     * (0, 0) -> (1, 1) square, centered if there extra space in one direction
     */
    fun normalized(): RoundedPolygon {
        val bounds = calculateBounds()
        val width = bounds[2] - bounds[0]
        val height = bounds[3] - bounds[1]
        val side = max(width, height)
        // Center the shape if bounds are not a square
        val offsetX = (side - width) / 2 - bounds[0] /* left */
        val offsetY = (side - height) / 2 - bounds[1] /* top */
        return transformed { x, y -> TransformResult((x + offsetX) / side, (y + offsetY) / side) }
    }

    override fun toString(): String =
        "[RoundedPolygon." +
            " Cubics = " +
            cubics.joinToString() +
            " || Features = " +
            features.joinToString() +
            " || Center = ($centerX, $centerY)]"

    /**
     * Like [calculateBounds], this function calculates the axis-aligned bounds of the object and
     * returns that rectangle. But this function determines the max dimension of the shape (by
     * calculating the distance from its center to the start and midpoint of each curve) and returns
     * a square which can be used to hold the object in any rotation. This function can be used, for
     * example, to calculate the max size of a UI element meant to hold this shape in any rotation.
     *
     * @param bounds a buffer to hold the results. If not supplied, a temporary buffer will be
     *   created.
     * @return The axis-aligned max bounding box for this object, where the rectangles left, top,
     *   right, and bottom values will be stored in entries 0, 1, 2, and 3, in that order.
     */
    fun calculateMaxBounds(bounds: FloatArray = FloatArray(4)): FloatArray {
        require(bounds.size >= 4) { "Required bounds size of 4" }
        var maxDistSquared = 0f
        for (i in cubics.indices) {
            val cubic = cubics[i]
            val anchorDistance = distanceSquared(cubic.anchor0X - centerX, cubic.anchor0Y - centerY)
            val middlePoint = cubic.pointOnCurve(.5f)
            val middleDistance = distanceSquared(middlePoint.x - centerX, middlePoint.y - centerY)
            maxDistSquared = max(maxDistSquared, max(anchorDistance, middleDistance))
        }
        val distance = sqrt(maxDistSquared)
        bounds[0] = centerX - distance
        bounds[1] = centerY - distance
        bounds[2] = centerX + distance
        bounds[3] = centerY + distance
        return bounds
    }

    /**
     * Calculates the axis-aligned bounds of the object.
     *
     * @param approximate when true, uses a faster calculation to create the bounding box based on
     *   the min/max values of all anchor and control points that make up the shape. Default value
     *   is true.
     * @param bounds a buffer to hold the results. If not supplied, a temporary buffer will be
     *   created.
     * @return The axis-aligned bounding box for this object, where the rectangles left, top, right,
     *   and bottom values will be stored in entries 0, 1, 2, and 3, in that order.
     */
    @JvmOverloads
    fun calculateBounds(
        bounds: FloatArray = FloatArray(4),
        approximate: Boolean = true
    ): FloatArray {
        require(bounds.size >= 4) { "Required bounds size of 4" }
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (i in cubics.indices) {
            val cubic = cubics[i]
            cubic.calculateBounds(bounds, approximate = approximate)
            minX = min(minX, bounds[0])
            minY = min(minY, bounds[1])
            maxX = max(maxX, bounds[2])
            maxY = max(maxY, bounds[3])
        }
        bounds[0] = minX
        bounds[1] = minY
        bounds[2] = maxX
        bounds[3] = maxY
        return bounds
    }

    companion object {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoundedPolygon) return false

        return features == other.features
    }

    override fun hashCode(): Int {
        return features.hashCode()
    }
}

/**
 * This constructor takes the number of vertices in the resulting polygon. These vertices are
 * positioned on a virtual circle around a given center with each vertex positioned [radius]
 * distance from that center, equally spaced (with equal angles between them). If no radius is
 * supplied, the shape will be created with a default radius of 1, resulting in a shape whose
 * vertices lie on a unit circle, with width/height of 2. That default polygon will probably need to
 * be rescaled using [transformed] into the appropriate size for the UI in which it will be drawn.
 *
 * The [rounding] and [perVertexRounding] parameters are optional. If not supplied, the result will
 * be a regular polygon with straight edges and unrounded corners.
 *
 * @param numVertices The number of vertices in this polygon.
 * @param radius The radius of the polygon, in pixels. This radius determines the initial size of
 *   the object, but it can be transformed later by using the [transformed] function.
 * @param centerX The X coordinate of the center of the polygon, around which all vertices will be
 *   placed. The default center is at (0,0).
 * @param centerY The Y coordinate of the center of the polygon, around which all vertices will be
 *   placed. The default center is at (0,0).
 * @param rounding The [CornerRounding] properties of all vertices. If some vertices should have
 *   different rounding properties, then use [perVertexRounding] instead. The default rounding value
 *   is [CornerRounding.Unrounded], meaning that the polygon will use the vertices themselves in the
 *   final shape and not curves rounded around the vertices.
 * @param perVertexRounding The [CornerRounding] properties of every vertex. If this parameter is
 *   not null, then it must have [numVertices] elements. If this parameter is null, then the polygon
 *   will use the [rounding] parameter for every vertex instead. The default value is null.
 * @throws IllegalArgumentException If [perVertexRounding] is not null and its size is not equal to
 *   [numVertices].
 * @throws IllegalArgumentException [numVertices] must be at least 3.
 */
@JvmOverloads
fun RoundedPolygon(
    @IntRange(from = 3) numVertices: Int,
    radius: Float = 1f,
    centerX: Float = 0f,
    centerY: Float = 0f,
    rounding: CornerRounding = CornerRounding.Unrounded,
    perVertexRounding: List<CornerRounding>? = null
) =
    RoundedPolygon(
        verticesFromNumVerts(numVertices, radius, centerX, centerY),
        rounding = rounding,
        perVertexRounding = perVertexRounding,
        centerX = centerX,
        centerY = centerY
    )

/** Creates a copy of the given [RoundedPolygon] */
fun RoundedPolygon(source: RoundedPolygon) =
    RoundedPolygon(source.features, source.centerX, source.centerY)

/**
 * This function takes the vertices (either supplied or calculated, depending on the constructor
 * called), plus [CornerRounding] parameters, and creates the actual [RoundedPolygon] shape,
 * rounding around the vertices (or not) as specified. The result is a list of [Cubic] curves which
 * represent the geometry of the final shape.
 *
 * @param vertices The list of vertices in this polygon specified as pairs of x/y coordinates in
 *   this FloatArray. This should be an ordered list (with the outline of the shape going from each
 *   vertex to the next in order of this list), otherwise the results will be undefined.
 * @param rounding The [CornerRounding] properties of all vertices. If some vertices should have
 *   different rounding properties, then use [perVertexRounding] instead. The default rounding value
 *   is [CornerRounding.Unrounded], meaning that the polygon will use the vertices themselves in the
 *   final shape and not curves rounded around the vertices.
 * @param perVertexRounding The [CornerRounding] properties of all vertices. If this parameter is
 *   not null, then it must have the same size as [vertices]. If this parameter is null, then the
 *   polygon will use the [rounding] parameter for every vertex instead. The default value is null.
 * @param centerX The X coordinate of the center of the polygon, around which all vertices will be
 *   placed. The default center is at (0,0).
 * @param centerY The Y coordinate of the center of the polygon, around which all vertices will be
 *   placed. The default center is at (0,0).
 * @throws IllegalArgumentException if the number of vertices is less than 3 (the [vertices]
 *   parameter has less than 6 Floats). Or if the [perVertexRounding] parameter is not null and the
 *   size doesn't match the number vertices.
 */
// TODO(performance): Update the map calls to more efficient code that doesn't allocate Iterators
//  unnecessarily.
@JvmOverloads
fun RoundedPolygon(
    vertices: FloatArray,
    rounding: CornerRounding = CornerRounding.Unrounded,
    perVertexRounding: List<CornerRounding>? = null,
    centerX: Float = Float.MIN_VALUE,
    centerY: Float = Float.MIN_VALUE
): RoundedPolygon {
    if (vertices.size < 6) {
        throw IllegalArgumentException("Polygons must have at least 3 vertices")
    }
    if (vertices.size % 2 == 1) {
        throw IllegalArgumentException("The vertices array should have even size")
    }
    if (perVertexRounding != null && perVertexRounding.size * 2 != vertices.size) {
        throw IllegalArgumentException(
            "perVertexRounding list should be either null or " +
                "the same size as the number of vertices (vertices.size / 2)"
        )
    }
    val corners = mutableListOf<List<Cubic>>()
    val n = vertices.size / 2
    val roundedCorners = mutableListOf<RoundedCorner>()
    for (i in 0 until n) {
        val vtxRounding = perVertexRounding?.get(i) ?: rounding
        val prevIndex = ((i + n - 1) % n) * 2
        val nextIndex = ((i + 1) % n) * 2
        roundedCorners.add(
            RoundedCorner(
                Point(vertices[prevIndex], vertices[prevIndex + 1]),
                Point(vertices[i * 2], vertices[i * 2 + 1]),
                Point(vertices[nextIndex], vertices[nextIndex + 1]),
                vtxRounding
            )
        )
    }

    // For each side, check if we have enough space to do the cuts needed, and if not split
    // the available space, first for round cuts, then for smoothing if there is space left.
    // Each element in this list is a pair, that represent how much we can do of the cut for
    // the given side (side i goes from corner i to corner i+1), the elements of the pair are:
    // first is how much we can use of expectedRoundCut, second how much of expectedCut
    val cutAdjusts =
        (0 until n).map { ix ->
            val expectedRoundCut =
                roundedCorners[ix].expectedRoundCut + roundedCorners[(ix + 1) % n].expectedRoundCut
            val expectedCut =
                roundedCorners[ix].expectedCut + roundedCorners[(ix + 1) % n].expectedCut
            val vtxX = vertices[ix * 2]
            val vtxY = vertices[ix * 2 + 1]
            val nextVtxX = vertices[((ix + 1) % n) * 2]
            val nextVtxY = vertices[((ix + 1) % n) * 2 + 1]
            val sideSize = distance(vtxX - nextVtxX, vtxY - nextVtxY)

            // Check expectedRoundCut first, and ensure we fulfill rounding needs first for
            // both corners before using space for smoothing
            if (expectedRoundCut > sideSize) {
                // Not enough room for fully rounding, see how much we can actually do.
                sideSize / expectedRoundCut to 0f
            } else if (expectedCut > sideSize) {
                // We can do full rounding, but not full smoothing.
                1f to (sideSize - expectedRoundCut) / (expectedCut - expectedRoundCut)
            } else {
                // There is enough room for rounding & smoothing.
                1f to 1f
            }
        }
    // Create and store list of beziers for each [potentially] rounded corner
    for (i in 0 until n) {
        // allowedCuts[0] is for the side from the previous corner to this one,
        // allowedCuts[1] is for the side from this corner to the next one.
        val allowedCuts = MutableFloatList(2)
        for (delta in 0..1) {
            val (roundCutRatio, cutRatio) = cutAdjusts[(i + n - 1 + delta) % n]
            allowedCuts.add(
                roundedCorners[i].expectedRoundCut * roundCutRatio +
                    (roundedCorners[i].expectedCut - roundedCorners[i].expectedRoundCut) * cutRatio
            )
        }
        corners.add(
            roundedCorners[i].getCubics(allowedCut0 = allowedCuts[0], allowedCut1 = allowedCuts[1])
        )
    }
    // Finally, store the calculated cubics. This includes all of the rounded corners
    // from above, along with new cubics representing the edges between those corners.
    val tempFeatures = mutableListOf<Feature>()
    for (i in 0 until n) {
        // Determine whether corner at this vertex is concave or convex, based on the
        // relationship of the prev->curr/curr->next vectors
        // Note that these indices are for pairs of values (points), they need to be
        // doubled to access the xy values in the vertices float array
        val prevVtxIndex = (i + n - 1) % n
        val nextVtxIndex = (i + 1) % n
        val currVertex = Point(vertices[i * 2], vertices[i * 2 + 1])
        val prevVertex = Point(vertices[prevVtxIndex * 2], vertices[prevVtxIndex * 2 + 1])
        val nextVertex = Point(vertices[nextVtxIndex * 2], vertices[nextVtxIndex * 2 + 1])
        val convex = (currVertex - prevVertex).clockwise(nextVertex - currVertex)
        tempFeatures.add(Feature.Corner(corners[i], currVertex, roundedCorners[i].center, convex))
        tempFeatures.add(
            Feature.Edge(
                listOf(
                    Cubic.straightLine(
                        corners[i].last().anchor1X,
                        corners[i].last().anchor1Y,
                        corners[(i + 1) % n].first().anchor0X,
                        corners[(i + 1) % n].first().anchor0Y
                    )
                )
            )
        )
    }

    val (cx, cy) =
        if (centerX == Float.MIN_VALUE || centerY == Float.MIN_VALUE) {
            calculateCenter(vertices)
        } else {
            Point(centerX, centerY)
        }
    return RoundedPolygon(tempFeatures, cx, cy)
}

/**
 * Calculates an estimated center position for the polygon, returning it. This function should only
 * be called if the center is not already calculated or provided. The Polygon constructor which
 * takes `numVertices` calculates its own center, since it knows exactly where it is centered, at
 * (0, 0).
 *
 * Note that this center will be transformed whenever the shape itself is transformed. Any
 * transforms that occur before the center is calculated will be taken into account automatically
 * since the center calculation is an average of the current location of all cubic anchor points.
 */
private fun calculateCenter(vertices: FloatArray): Point {
    var cumulativeX = 0f
    var cumulativeY = 0f
    var index = 0
    while (index < vertices.size) {
        cumulativeX += vertices[index++]
        cumulativeY += vertices[index++]
    }
    return Point(cumulativeX / (vertices.size / 2), cumulativeY / (vertices.size / 2))
}

/**
 * Private utility class that holds the information about each corner in a polygon. The shape of the
 * corner can be returned by calling the [getCubics] function, which will return a list of curves
 * representing the corner geometry. The shape of the corner depends on the [rounding] constructor
 * parameter.
 *
 * If rounding is null, there is no rounding; the corner will simply be a single point at [p1]. This
 * point will be represented by a [Cubic] of length 0 at that point.
 *
 * If rounding is not null, the corner will be rounded either with a curve approximating a circular
 * arc of the radius specified in [rounding], or with three curves if [rounding] has a nonzero
 * smoothing parameter. These three curves are a circular arc in the middle and two symmetrical
 * flanking curves on either side. The smoothing parameter determines the curvature of the flanking
 * curves.
 *
 * This is a class because we usually need to do the work in 2 steps, and prefer to keep state
 * between: first we determine how much we want to cut to comply with the parameters, then we are
 * given how much we can actually cut (because of space restrictions outside this corner)
 *
 * @param p0 the vertex before the one being rounded
 * @param p1 the vertex of this rounded corner
 * @param p2 the vertex after the one being rounded
 * @param rounding the optional parameters specifying how this corner should be rounded
 */
private class RoundedCorner(
    val p0: Point,
    val p1: Point,
    val p2: Point,
    val rounding: CornerRounding? = null
) {
    val d1: Point
    val d2: Point
    val cornerRadius: Float
    val smoothing: Float
    val cosAngle: Float
    val sinAngle: Float
    val expectedRoundCut: Float

    init {
        val v01 = p0 - p1
        val v21 = p2 - p1
        val d01 = v01.getDistance()
        val d21 = v21.getDistance()
        if (d01 > 0f && d21 > 0f) {
            d1 = v01 / d01
            d2 = v21 / d21
            cornerRadius = rounding?.radius ?: 0f
            smoothing = rounding?.smoothing ?: 0f

            // cosine of angle at p1 is dot product of unit vectors to the other two vertices
            cosAngle = d1.dotProduct(d2)

            // identity: sin^2 + cos^2 = 1
            // sinAngle gives us the intersection
            sinAngle = sqrt(1 - square(cosAngle))
            // How much we need to cut, as measured on a side, to get the required radius
            // calculating where the rounding circle hits the edge
            // This uses the identity of tan(A/2) = sinA/(1 + cosA), where tan(A/2) = radius/cut
            expectedRoundCut =
                if (sinAngle > 1e-3) {
                    cornerRadius * (cosAngle + 1) / sinAngle
                } else {
                    0f
                }
        } else {
            // One (or both) of the sides is empty, not much we can do.
            d1 = Point(0f, 0f)
            d2 = Point(0f, 0f)
            cornerRadius = 0f
            smoothing = 0f
            cosAngle = 0f
            sinAngle = 0f
            expectedRoundCut = 0f
        }
    }

    // smoothing changes the actual cut. 0 is same as expectedRoundCut, 1 doubles it
    val expectedCut: Float
        get() = ((1 + smoothing) * expectedRoundCut)

    // the center of the circle approximated by the rounding curve (or the middle of the three
    // curves if smoothing is requested). The center is the same as p0 if there is no rounding.
    var center: Point = Point(0f, 0f)

    @JvmOverloads
    fun getCubics(allowedCut0: Float, allowedCut1: Float = allowedCut0): List<Cubic> {
        // We use the minimum of both cuts to determine the radius, but if there is more space
        // in one side we can use it for smoothing.
        val allowedCut = min(allowedCut0, allowedCut1)
        // Nothing to do, just use lines, or a point
        if (
            expectedRoundCut < DistanceEpsilon ||
                allowedCut < DistanceEpsilon ||
                cornerRadius < DistanceEpsilon
        ) {
            center = p1
            return listOf(Cubic.straightLine(p1.x, p1.y, p1.x, p1.y))
        }
        // How much of the cut is required for the rounding part.
        val actualRoundCut = min(allowedCut, expectedRoundCut)
        // We have two smoothing values, one for each side of the vertex
        // Space is used for rounding values first. If there is space left over, then we
        // apply smoothing, if it was requested
        val actualSmoothing0 = calculateActualSmoothingValue(allowedCut0)
        val actualSmoothing1 = calculateActualSmoothingValue(allowedCut1)
        // Scale the radius if needed
        val actualR = cornerRadius * actualRoundCut / expectedRoundCut
        // Distance from the corner (p1) to the center
        val centerDistance = sqrt(square(actualR) + square(actualRoundCut))
        // Center of the arc we will use for rounding
        center = p1 + ((d1 + d2) / 2f).getDirection() * centerDistance
        val circleIntersection0 = p1 + d1 * actualRoundCut
        val circleIntersection2 = p1 + d2 * actualRoundCut
        val flanking0 =
            computeFlankingCurve(
                actualRoundCut,
                actualSmoothing0,
                p1,
                p0,
                circleIntersection0,
                circleIntersection2,
                center,
                actualR
            )
        val flanking2 =
            computeFlankingCurve(
                    actualRoundCut,
                    actualSmoothing1,
                    p1,
                    p2,
                    circleIntersection2,
                    circleIntersection0,
                    center,
                    actualR
                )
                .reverse()
        return listOf(
            flanking0,
            Cubic.circularArc(
                center.x,
                center.y,
                flanking0.anchor1X,
                flanking0.anchor1Y,
                flanking2.anchor0X,
                flanking2.anchor0Y
            ),
            flanking2
        )
    }

    /**
     * If allowedCut (the amount we are able to cut) is greater than the expected cut (without
     * smoothing applied yet), then there is room to apply smoothing and we calculate the actual
     * smoothing value here.
     */
    private fun calculateActualSmoothingValue(allowedCut: Float): Float {
        return if (allowedCut > expectedCut) {
            smoothing
        } else if (allowedCut > expectedRoundCut) {
            smoothing * (allowedCut - expectedRoundCut) / (expectedCut - expectedRoundCut)
        } else {
            0f
        }
    }

    /**
     * Compute a Bezier to connect the linear segment defined by corner and sideStart with the
     * circular segment defined by circleCenter, circleSegmentIntersection,
     * otherCircleSegmentIntersection and actualR. The bezier will start at the linear segment and
     * end on the circular segment.
     *
     * @param actualRoundCut How much we are cutting of the corner to add the circular segment (this
     *   is before smoothing, that will cut some more).
     * @param actualSmoothingValues How much we want to smooth (this is the smooth parameter,
     *   adjusted down if there is not enough room).
     * @param corner The point at which the linear side ends
     * @param sideStart The point at which the linear side starts
     * @param circleSegmentIntersection The point at which the linear side and the circle intersect.
     * @param otherCircleSegmentIntersection The point at which the opposing linear side and the
     *   circle intersect.
     * @param circleCenter The center of the circle.
     * @param actualR The radius of the circle.
     * @return a Bezier cubic curve that connects from the (cut) linear side and the (cut) circular
     *   segment in a smooth way.
     */
    private fun computeFlankingCurve(
        actualRoundCut: Float,
        actualSmoothingValues: Float,
        corner: Point,
        sideStart: Point,
        circleSegmentIntersection: Point,
        otherCircleSegmentIntersection: Point,
        circleCenter: Point,
        actualR: Float
    ): Cubic {
        // sideStart is the anchor, 'anchor' is actual control point
        val sideDirection = (sideStart - corner).getDirection()
        val curveStart = corner + sideDirection * actualRoundCut * (1 + actualSmoothingValues)
        // We use an approximation to cut a part of the circle section proportional to 1 - smooth,
        // When smooth = 0, we take the full section, when smooth = 1, we take nothing.
        // TODO: revisit this, it can be problematic as it approaches 180 degrees
        val p =
            interpolate(
                circleSegmentIntersection,
                (circleSegmentIntersection + otherCircleSegmentIntersection) / 2f,
                actualSmoothingValues
            )
        // The flanking curve ends on the circle
        val curveEnd =
            circleCenter + directionVector(p.x - circleCenter.x, p.y - circleCenter.y) * actualR
        // The anchor on the circle segment side is in the intersection between the tangent to the
        // circle in the circle/flanking curve boundary and the linear segment.
        val circleTangent = (curveEnd - circleCenter).rotate90()
        val anchorEnd =
            lineIntersection(sideStart, sideDirection, curveEnd, circleTangent)
                ?: circleSegmentIntersection
        // From what remains, we pick a point for the start anchor.
        // 2/3 seems to come from design tools?
        val anchorStart = (curveStart + anchorEnd * 2f) / 3f
        return Cubic(curveStart, anchorStart, anchorEnd, curveEnd)
    }

    /**
     * Returns the intersection point of the two lines d0->d1 and p0->p1, or null if the lines do
     * not intersect
     */
    private fun lineIntersection(p0: Point, d0: Point, p1: Point, d1: Point): Point? {
        val rotatedD1 = d1.rotate90()
        val den = d0.dotProduct(rotatedD1)
        if (abs(den) < DistanceEpsilon) return null
        val num = (p1 - p0).dotProduct(rotatedD1)
        // Also check the relative value. This is equivalent to abs(den/num) < DistanceEpsilon,
        // but avoid doing a division
        if (abs(den) < DistanceEpsilon * abs(num)) return null
        val k = num / den
        return p0 + d0 * k
    }
}

private fun verticesFromNumVerts(
    numVertices: Int,
    radius: Float,
    centerX: Float,
    centerY: Float
): FloatArray {
    val result = FloatArray(numVertices * 2)
    var arrayIndex = 0
    for (i in 0 until numVertices) {
        val vertex =
            radialToCartesian(radius, (FloatPi / numVertices * 2 * i)) + Point(centerX, centerY)
        result[arrayIndex++] = vertex.x
        result[arrayIndex++] = vertex.y
    }
    return result
}
