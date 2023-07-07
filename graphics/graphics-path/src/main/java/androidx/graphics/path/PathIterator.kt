/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("PathUtilities")
package androidx.graphics.path

import android.graphics.Path
import android.os.Build

/**
 * A path iterator can be used to iterate over all the [segments][PathSegment] that make up
 * a path. Those segments may in turn define multiple contours inside the path. Conic segments
 * are by default evaluated as approximated quadratic segments. To preserve conic segments as
 * conics, set [conicEvaluation] to [AsConic][ConicEvaluation.AsConic]. The error of the
 * approximation is controlled by [tolerance].
 *
 * [PathIterator] objects are created implicitly through a given [Path] object; to create a
 * [PathIterator], call one of the two [Path.iterator] extension functions.
 */
@Suppress("NotCloseable", "IllegalExperimentalApiUsage")
class PathIterator constructor(
    val path: Path,
    val conicEvaluation: ConicEvaluation = ConicEvaluation.AsQuadratics,
    val tolerance: Float = 0.25f
) : Iterator<PathSegment> {

    internal val implementation: PathIteratorImpl
    init {
        implementation =
            when {
                Build.VERSION.SDK_INT >= 34 -> {
                    PathIteratorApi34Impl(path, conicEvaluation, tolerance)
                }
                else -> {
                    PathIteratorPreApi34Impl(path, conicEvaluation, tolerance)
                }
            }
    }

    enum class ConicEvaluation {
        /**
         * Conic segments are returned as conic segments.
         */
        AsConic,

        /**
         * Conic segments are returned as quadratic approximations. The quality of the
         * approximation is defined by a tolerance value.
         */
        AsQuadratics
    }

    /**
     * Returns the number of verbs present in this iterator, i.e. the number of calls to
     * [next] required to complete the iteration.
     *
     * By default, [calculateSize] returns the true number of operations in the iterator. Deriving
     * this result requires converting any conics to quadratics, if [conicEvaluation] is
     * set to [ConicEvaluation.AsQuadratics], which takes extra processing time. Set
     * [includeConvertedConics] to false if an approximate size, not including conic
     * conversion, is sufficient.
     *
     * @param includeConvertedConics The returned size includes any required conic conversions.
     * Default is true, so it will return the exact size, at the cost of iterating through
     * all elements and converting any conics as appropriate. Set to false to save on processing,
     * at the cost of a less exact result.
     */
    fun calculateSize(includeConvertedConics: Boolean = true) =
        implementation.calculateSize(includeConvertedConics)

    /**
     * Returns `true` if the iteration has more elements.
     */
    override fun hasNext(): Boolean = implementation.hasNext()

    /**
     * Returns the type of the current segment in the iteration, or [Done][PathSegment.Type.Done]
     * if the iteration is finished.
     */
    fun peek() = implementation.peek()

    /**
     * Returns the [type][PathSegment.Type] of the next [path segment][PathSegment] in the iteration
     * and fills [points] with the points specific to the segment type. Each pair of floats in
     * the [points] array represents a point for the given segment. The number of pairs of floats
     * depends on the [PathSegment.Type]:
     * - [Move][PathSegment.Type.Move]: 1 pair (indices 0 to 1)
     * - [Move][PathSegment.Type.Line]: 2 pairs (indices 0 to 3)
     * - [Move][PathSegment.Type.Quadratic]: 3 pairs (indices 0 to 5)
     * - [Move][PathSegment.Type.Conic]: 4 pairs (indices 0 to 7), the last pair contains the
     *   [weight][PathSegment.weight] twice
     * - [Move][PathSegment.Type.Cubic]: 4 pairs (indices 0 to 7)
     * - [Close][PathSegment.Type.Close]: 0 pair
     * - [Done][PathSegment.Type.Done]: 0 pair
     * This method does not allocate any memory.
     *
     * @param points A [FloatArray] large enough to hold 8 floats starting at [offset],
     *               throws an [IllegalStateException] otherwise.
     * @param offset Offset in [points] where to store the result
     */
    @JvmOverloads
    fun next(points: FloatArray, offset: Int = 0): PathSegment.Type =
        implementation.next(points, offset)

    /**
     * Returns the next [path segment][PathSegment] in the iteration, or [DoneSegment] if
     * the iteration is finished. To save on allocations, use the alternative [next] function, which
     * takes a [FloatArray].
     */
    override fun next(): PathSegment = implementation.next()
}

/**
 * Creates a new [PathIterator] for this [path][android.graphics.Path] that evaluates
 * conics as quadratics. To preserve conics, use the [Path.iterator] function that takes a
 * [PathIterator.ConicEvaluation] parameter.
 */
operator fun Path.iterator() = PathIterator(this)

/**
 * Creates a new [PathIterator] for this [path][android.graphics.Path]. To preserve conics as
 * conics (not convert them to quadratics), set [conicEvaluation] to
 * [PathIterator.ConicEvaluation.AsConic].
 */
fun Path.iterator(conicEvaluation: PathIterator.ConicEvaluation, tolerance: Float = 0.25f) =
    PathIterator(this, conicEvaluation, tolerance)
