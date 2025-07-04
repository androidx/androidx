/*
 * Copyright 2023 The Android Open Source Project
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

import kotlin.math.abs

// TODO: b/374764251 Introduce an IgnorableFeature?

/**
 * While a polygon's shape can be drawn solely using a list of [Cubic] objects representing its raw
 * curves and lines, features add an extra layer of context to groups of cubics. Features group
 * cubics into (straight) edges, convex corners, or concave corners. For example, rounding a
 * rectangle adds many cubics around its edges, but the rectangle's overall number of corners
 * remains the same. [Morph] therefore uses this grouping for several reasons:
 * - Noise Reduction: Grouping cubics reduces the amount of noise introduced by individual cubics
 *   (as seen in the rounded rectangle example).
 * - Mapping Base: The grouping serves as the base set for [Morph]'s mapping process.
 * - Curve Type Mapping: [Morph] maps similar curve types (convex, concave) together. Note that
 *   edges or features created with [buildIgnorableFeature] are ignored in the default mapping.
 *
 * By using features, you can manipulate polygon shapes with more context and control.
 */
abstract class Feature(val cubics: List<Cubic>) {

    companion object Factory {
        /**
         * Group a list of [Cubic] objects to a feature that should be ignored in the default
         * [Morph] mapping. The feature can have any indentation.
         *
         * Sometimes, it's helpful to ignore certain features when morphing shapes. This is because
         * only the features you mark as important will be smoothly transitioned between the start
         * and end shapes. Additionally, the default morph algorithm will try to match convex
         * corners to convex corners and concave to concave. Marking features as ignorable will
         * influence this matching. For example, given a 12-pointed star, marking all concave
         * corners as ignorable will create a [Morph] that only considers the outer corners of the
         * star. As a result, depending on the morphed to shape, the animation may have fewer
         * intersections and rotations. Another example for the other way around is a [Morph]
         * between a pointed up triangle to a square. Marking the square's top edge as a convex
         * corner matches it to the triangle's upper corner. Instead of moving triangle's upper
         * corner to one of rectangle's corners, the animation now splits the triangle to match
         * squares' outer corners.
         *
         * @param cubics The list of raw cubics describing the feature's shape
         * @throws IllegalArgumentException for lists of empty cubics or non-continuous cubics
         */
        fun buildIgnorableFeature(cubics: List<Cubic>): Feature = validated(Edge(cubics))

        /**
         * Group a [Cubic] object to an edge (neither inward or outward identification in a shape).
         *
         * @param cubic The raw cubic describing the edge's shape
         * @throws IllegalArgumentException for lists of empty cubics or non-continuous cubics
         */
        fun buildEdge(cubic: Cubic): Feature = Edge(listOf(cubic))

        /**
         * Group a list of [Cubic] objects to a convex corner (outward indentation in a shape).
         *
         * @param cubics The list of raw cubics describing the corner's shape
         * @throws IllegalArgumentException for lists of empty cubics or non-continuous cubics
         */
        fun buildConvexCorner(cubics: List<Cubic>): Feature = validated(Corner(cubics, true))

        /**
         * Group a list of [Cubic] objects to a concave corner (inward indentation in a shape).
         *
         * @param cubics The list of raw cubics describing the corner's shape
         * @throws IllegalArgumentException for lists of empty cubics or non-continuous cubics
         */
        fun buildConcaveCorner(cubics: List<Cubic>): Feature = validated(Corner(cubics, false))

        private fun validated(feature: Feature): Feature {
            require(feature.cubics.isNotEmpty()) { "Features need at least one cubic." }

            require(isContinuous(feature)) {
                "Feature must be continuous, with the anchor points of all cubics " +
                    "matching the anchor points of the preceding and succeeding cubics"
            }

            return feature
        }

        private fun isContinuous(feature: Feature): Boolean {
            var prevCubic = feature.cubics.first()
            for (index in 1..feature.cubics.lastIndex) {
                val cubic = feature.cubics[index]
                if (
                    abs(cubic.anchor0X - prevCubic.anchor1X) > DistanceEpsilon ||
                        abs(cubic.anchor0Y - prevCubic.anchor1Y) > DistanceEpsilon
                ) {
                    return false
                }
                prevCubic = cubic
            }
            return true
        }
    }

    /**
     * Transforms the points in this [Feature] with the given [PointTransformer] and returns a new
     * [Feature]
     *
     * @param f The [PointTransformer] used to transform this [Feature]
     */
    abstract fun transformed(f: PointTransformer): Feature

    /**
     * Returns a new [Feature] with the points that define the shape of this [Feature] in reversed
     * order.
     */
    abstract fun reversed(): Feature

    /**
     * Whether this Feature gets ignored in the Morph mapping. See [buildIgnorableFeature] for more
     * details.
     */
    abstract val isIgnorableFeature: Boolean

    /** Whether this Feature is an Edge with no inward or outward indentation. */
    abstract val isEdge: Boolean

    /** Whether this Feature is a convex corner (outward indentation in a shape). */
    abstract val isConvexCorner: Boolean

    /** Whether this Feature is a concave corner (inward indentation in a shape). */
    abstract val isConcaveCorner: Boolean

    /**
     * Edges have only a list of the cubic curves which make up the edge. Edges lie between corners
     * and have no vertex or concavity; the curves are simply straight lines (represented by Cubic
     * curves).
     */
    internal class Edge(cubics: List<Cubic>) : Feature(cubics) {
        override fun transformed(f: PointTransformer) =
            Edge(
                buildList {
                    // Performance: Builds the list by avoiding creating an unnecessary Iterator to
                    // iterate through the cubics List.
                    for (i in cubics.indices) {
                        add(cubics[i].transformed(f))
                    }
                }
            )

        override fun reversed(): Edge {
            val reversedCubics = mutableListOf<Cubic>()

            for (i in cubics.lastIndex downTo 0) {
                reversedCubics.add(cubics[i].reverse())
            }

            return Edge(reversedCubics)
        }

        override fun toString(): String = "Edge"

        override val isIgnorableFeature = true

        override val isEdge = true

        override val isConvexCorner = false

        override val isConcaveCorner = false
    }

    /**
     * Corners contain the list of cubic curves which describe how the corner is rounded (or not),
     * and a flag indicating whether the corner is convex. A regular polygon has all convex corners,
     * while a star polygon generally (but not necessarily) has both convex (outer) and concave
     * (inner) corners.
     */
    internal class Corner(cubics: List<Cubic>, val convex: Boolean = true) : Feature(cubics) {
        override fun transformed(f: PointTransformer): Feature {
            return Corner(
                buildList {
                    // Performance: Builds the list by avoiding creating an unnecessary Iterator to
                    // iterate through the cubics List.
                    for (i in cubics.indices) {
                        add(cubics[i].transformed(f))
                    }
                },
                convex,
            )
        }

        override fun reversed(): Corner {
            val reversedCubics = mutableListOf<Cubic>()

            for (i in cubics.lastIndex downTo 0) {
                reversedCubics.add(cubics[i].reverse())
            }

            // TODO: b/369320447 - Revert flag negation when [RoundedPolygon] ignores orientation
            // for setting the flag
            return Corner(reversedCubics, !convex)
        }

        override fun toString(): String {
            return "Corner: cubics=${cubics.joinToString(separator = ", "){"[$it]"}} convex=$convex"
        }

        override val isIgnorableFeature = false

        override val isEdge = false

        override val isConvexCorner = convex

        override val isConcaveCorner = !convex
    }
}
