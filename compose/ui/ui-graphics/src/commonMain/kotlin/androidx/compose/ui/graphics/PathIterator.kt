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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.PathIterator.ConicEvaluation

/**
 * A path iterator can be used to iterate over all the [segments][PathSegment] that make up a path.
 * Those segments may in turn define multiple contours inside the path. Conic segments are by
 * default evaluated as approximated quadratic segments. To preserve conic segments as conics, set
 * [conicEvaluation] to [AsConic][ConicEvaluation.AsConic]. The error of the approximation is
 * controlled by [tolerance].
 *
 * A [PathIterator] can be created implicitly through a given [Path] object: using one of the two
 * [Path.iterator] functions.
 *
 * @param path The [Path] to iterate over
 * @param conicEvaluation Indicates how to evaluate conic segments
 * @param tolerance When [conicEvaluation] is set to [PathIterator.ConicEvaluation.AsQuadratics]
 *   defines the maximum distance between the original conic curve and its quadratic approximations
 */
expect fun PathIterator(
    path: Path,
    conicEvaluation: ConicEvaluation = ConicEvaluation.AsQuadratics,
    tolerance: Float = 0.25f
): PathIterator

/**
 * A path iterator can be used to iterate over all the [segments][PathSegment] that make up a path.
 * Those segments may in turn define multiple contours inside the path.
 *
 * The handling of conic segments is defined by the [conicEvaluation] property. When set to
 * [AsConic][ConicEvaluation.AsConic], conic segments are preserved, but when set to
 * [AsConic][ConicEvaluation.AsQuadratics], conic segments are approximated using 1 or more
 * quadratic segments. The error of the approximation is controlled by [tolerance].
 */
interface PathIterator : Iterator<PathSegment> {
    /**
     * Used to define how conic segments are evaluated when iterating over a [Path] using
     * [PathIterator].
     */
    enum class ConicEvaluation {
        /** Conic segments are returned as conic segments. */
        AsConic,

        /**
         * Conic segments are returned as quadratic approximations. The quality of the approximation
         * is defined by a tolerance value.
         */
        AsQuadratics
    }

    /** The [Path] this iterator iterates on. */
    val path: Path

    /**
     * Indicates whether conic segments, when present, are preserved as-is or converted to quadratic
     * segments, using an approximation whose error is controlled by [tolerance].
     */
    val conicEvaluation: ConicEvaluation

    /**
     * Error of the approximation used to evaluate conic segments if they are converted to
     * quadratics. The error is defined as the maximum distance between the original conic segment
     * and its quadratic approximation. See [conicEvaluation].
     */
    val tolerance: Float

    /**
     * Returns the number of verbs present in this iterator, i.e. the number of calls to [next]
     * required to complete the iteration.
     *
     * By default, [calculateSize] returns the true number of operations in the iterator. Deriving
     * this result requires converting any conics to quadratics, if [conicEvaluation] is set to
     * [ConicEvaluation.AsQuadratics], which takes extra processing time. Set
     * [includeConvertedConics] to false if an approximate size, not including conic conversion, is
     * sufficient.
     *
     * @param includeConvertedConics The returned size includes any required conic conversions.
     *   Default is true, so it will return the exact size, at the cost of iterating through all
     *   elements and converting any conics as appropriate. Set to false to save on processing, at
     *   the cost of a less exact result.
     */
    fun calculateSize(includeConvertedConics: Boolean = true): Int

    /** Returns `true` if the iteration has more elements. */
    override fun hasNext(): Boolean

    /**
     * Returns the [type][PathSegment.Type] of the next [path segment][PathSegment] in the iteration
     * and fills [outPoints] with the points specific to the segment type. Each pair of floats in
     * the [outPoints] array represents a point for the given segment. The number of pairs of floats
     * depends on the [PathSegment.Type]:
     * - [Move][PathSegment.Type.Move]: 1 pair (indices 0 to 1)
     * - [Line][PathSegment.Type.Line]: 2 pairs (indices 0 to 3)
     * - [Quadratic][PathSegment.Type.Quadratic]: 3 pairs (indices 0 to 5)
     * - [Conic][PathSegment.Type.Conic]: 3 pairs (indices 0 to 5), and the conic
     *   [weight][PathSegment.weight] at index 6. The value of the last float is undefined. See
     *   [PathSegment.Type.Conic] for more details
     * - [Cubic][PathSegment.Type.Cubic]: 4 pairs (indices 0 to 7)
     * - [Close][PathSegment.Type.Close]: 0 pair
     * - [Done][PathSegment.Type.Done]: 0 pair
     *
     * This method does not allocate any memory.
     *
     * @param outPoints A [FloatArray] large enough to hold 8 floats starting at [offset], throws an
     *   [IllegalStateException] otherwise.
     * @param offset Offset in [outPoints] where to store the result
     */
    fun next(outPoints: FloatArray, offset: Int = 0): PathSegment.Type

    /**
     * Returns the next [path segment][PathSegment] in the iteration, or [DoneSegment] if the
     * iteration is finished. To save on allocations, use the alternative [next] function, which
     * takes a [FloatArray].
     */
    override fun next(): PathSegment
}
