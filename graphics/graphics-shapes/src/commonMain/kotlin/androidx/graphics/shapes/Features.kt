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

/**
 * This class holds information about a corner (rounded or not) or an edge of a given polygon. The
 * features of a Polygon can be used to manipulate the shape with more context of what the shape
 * actually is, rather than simply manipulating the raw curves and lines which describe it.
 */
internal abstract class Feature(val cubics: List<Cubic>) {
    internal abstract fun transformed(f: PointTransformer): Feature

    internal abstract fun reversed(): Feature

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
                convex
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
    }
}
