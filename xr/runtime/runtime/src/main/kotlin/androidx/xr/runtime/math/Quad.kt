/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.runtime.math

/**
 * Represents a 4-point quadrilateral polygon in 2D space.
 *
 * Can represent mathematically skewed or distorted geometry that breaks strict axis constraints.
 *
 * @property upperLeft The top-left [Vector2] corner.
 * @property upperRight The top-right [Vector2] corner.
 * @property lowerRight The bottom-right [Vector2] corner.
 * @property lowerLeft The bottom-left [Vector2] corner.
 */
public class Quad
private constructor(
    public val upperLeft: Vector2,
    public val upperRight: Vector2,
    public val lowerRight: Vector2,
    public val lowerLeft: Vector2,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Quad) return false

        if (upperLeft != other.upperLeft) return false
        if (upperRight != other.upperRight) return false
        if (lowerRight != other.lowerRight) return false
        if (lowerLeft != other.lowerLeft) return false

        return true
    }

    override fun hashCode(): Int {
        var result = upperLeft.hashCode()
        result = 31 * result + upperRight.hashCode()
        result = 31 * result + lowerRight.hashCode()
        result = 31 * result + lowerLeft.hashCode()
        return result
    }

    override fun toString(): String {
        return "Quad(upperLeft=$upperLeft, upperRight=$upperRight, lowerRight=$lowerRight, lowerLeft=$lowerLeft)"
    }

    public companion object {
        /**
         * Creates a Quad from 4 explicitly defined corners allowing for arbitrary skew.
         *
         * @param upperLeft The top-left [Vector2] corner.
         * @param upperRight The top-right [Vector2] corner.
         * @param lowerRight The bottom-right [Vector2] corner.
         * @param lowerLeft The bottom-left [Vector2] corner.
         * @return A newly constructed [Quad] containing the specified geometry.
         * @throws IllegalArgumentException if any of the provided coordinates contain a NaN float.
         */
        @JvmStatic
        public fun createFromCorners(
            upperLeft: Vector2,
            upperRight: Vector2,
            lowerRight: Vector2,
            lowerLeft: Vector2,
        ): Quad {
            require(!upperLeft.x.isNaN() && !upperLeft.y.isNaN()) {
                "Quad upperLeft coordinates must not contain NaN"
            }
            require(!upperRight.x.isNaN() && !upperRight.y.isNaN()) {
                "Quad upperRight coordinates must not contain NaN"
            }
            require(!lowerRight.x.isNaN() && !lowerRight.y.isNaN()) {
                "Quad lowerRight coordinates must not contain NaN"
            }
            require(!lowerLeft.x.isNaN() && !lowerLeft.y.isNaN()) {
                "Quad lowerLeft coordinates must not contain NaN"
            }
            return Quad(upperLeft, upperRight, lowerRight, lowerLeft)
        }

        /**
         * Creates a perfectly axis-aligned rectangular Quad.
         *
         * @param min A [Vector2] representing the absolute minimum coordinate corner.
         * @param max A [Vector2] representing the absolute maximum coordinate corner.
         * @return A newly constructed [Quad] encompassing the bounding box limit.
         * @throws IllegalArgumentException if any coordinate is NaN, or if min bounding coordinates
         *   are greater than their respective max bounding coordinates.
         */
        @JvmStatic
        public fun createAxisAligned(min: Vector2, max: Vector2): Quad {
            require(!min.x.isNaN() && !min.y.isNaN()) {
                "Quad min coordinates must not contain NaN"
            }
            require(!max.x.isNaN() && !max.y.isNaN()) {
                "Quad max coordinates must not contain NaN"
            }
            require(min.x <= max.x && min.y <= max.y) {
                "Quad min coordinates must be less than or equal to max coordinates"
            }
            return createFromCorners(
                upperLeft = Vector2(min.x, min.y),
                upperRight = Vector2(max.x, min.y),
                lowerRight = Vector2(max.x, max.y),
                lowerLeft = Vector2(min.x, max.y),
            )
        }
    }
}
