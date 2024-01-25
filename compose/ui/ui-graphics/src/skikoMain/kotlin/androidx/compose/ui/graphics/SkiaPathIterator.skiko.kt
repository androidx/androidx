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

import org.jetbrains.skia.PathVerb

actual fun PathIterator(
    path: Path,
    conicEvaluation: PathIterator.ConicEvaluation,
    tolerance: Float
): PathIterator = SkikoPathIterator(path, conicEvaluation, tolerance)

// The code below would be used to handle conic to quadratic conversions.
// The core Skia API exposed by Skiko accepts a "power of 2" number of
// quadratics when calling Path.convertConicToQuads() but our APIs expose
// a tolerance/error value. This code computes that "power of 2" number
// (from 0 to 5) given a specified tolerance value.
//
// const val MaxConicToQuadraticSubdivisions = 5
//
// private fun Point.isFinite() = x.isFinite() && y.isFinite()
//
// private fun toleranceToSubdivisions(
//     p0: Point,
//     p1: Point,
//     p2: Point,
//     weight: Float,
//     tolerance: Float
// ): Int {
//     if (
//         tolerance <= 0.0f ||
//         !tolerance.isFinite() ||
//         !p0.isFinite() ||
//         !p1.isFinite() ||
//         !p2.isFinite()
//     ) {
//         return 0
//     }
//
//     val a = weight - 1.0f
//     val k = a / (4.0f * (2.0f + a))
//     val x = k * (p0.x - 2.0f * p1.x + p2.x)
//     val y = k * (p0.y - 2.0f * p1.y + p2.y)
//
//     var error = sqrt(x * x + y * y);
//     var subdivisions = 0
//     while (subdivisions < MaxConicToQuadraticSubdivisions) {
//         if (error <= tolerance) break
//         error *= 0.25f
//         subdivisions++
//     }
//     return subdivisions
// }

class SkikoPathIterator(
    override val path: Path,
    override val conicEvaluation: PathIterator.ConicEvaluation,
    override val tolerance: Float
) : PathIterator {
    private val skiaPath = path.asSkiaPath()
    private val iterator = skiaPath.iterator()

    // TODO: Handle conversion from conics to quadratics
    override fun calculateSize(includeConvertedConics: Boolean): Int = skiaPath.verbsCount

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(outPoints: FloatArray, offset: Int): PathSegment.Type {
        check(outPoints.size - offset >= 8) { "The points array must contain at least 8 floats" }

        if (!hasNext()) return PathSegment.Type.Done

        val segment = iterator.next()
        requireNotNull(segment)

        return when (segment.verb) {
            PathVerb.MOVE -> {
                outPoints[offset] = segment.p0!!.x
                outPoints[offset + 1] = segment.p0!!.y
                PathSegment.Type.Move
            }
            PathVerb.LINE -> {
                outPoints[offset] = segment.p0!!.x
                outPoints[offset + 1] = segment.p0!!.y
                outPoints[offset + 2] = segment.p1!!.x
                outPoints[offset + 3] = segment.p1!!.y
                PathSegment.Type.Line
            }
            PathVerb.QUAD -> {
                outPoints[offset] = segment.p0!!.x
                outPoints[offset + 1] = segment.p0!!.y
                outPoints[offset + 2] = segment.p1!!.x
                outPoints[offset + 3] = segment.p1!!.y
                outPoints[offset + 4] = segment.p2!!.x
                outPoints[offset + 5] = segment.p2!!.y
                PathSegment.Type.Quadratic
            }
            // TODO: convert conics to quadratics when conicEvaluation is set to AsQuadratics
            PathVerb.CONIC -> {
                outPoints[offset] = segment.p0!!.x
                outPoints[offset + 1] = segment.p0!!.y
                outPoints[offset + 2] = segment.p1!!.x
                outPoints[offset + 3] = segment.p1!!.y
                outPoints[offset + 4] = segment.p2!!.x
                outPoints[offset + 5] = segment.p2!!.y
                outPoints[offset + 6] = segment.conicWeight
                outPoints[offset + 7] = segment.conicWeight
                PathSegment.Type.Conic
            }
            PathVerb.CUBIC -> {
                outPoints[offset] = segment.p0!!.x
                outPoints[offset + 1] = segment.p0!!.y
                outPoints[offset + 2] = segment.p1!!.x
                outPoints[offset + 3] = segment.p1!!.y
                outPoints[offset + 4] = segment.p2!!.x
                outPoints[offset + 5] = segment.p2!!.y
                outPoints[offset + 6] = segment.p3!!.x
                outPoints[offset + 7] = segment.p3!!.y
                PathSegment.Type.Cubic
            }
            PathVerb.CLOSE -> PathSegment.Type.Close
            PathVerb.DONE -> PathSegment.Type.Done
        }
    }

    override fun next(): PathSegment {
        if (!hasNext()) return DoneSegment

        val segment = iterator.next()
        requireNotNull(segment)

        return when (segment.verb) {
            PathVerb.MOVE -> PathSegment(
                PathSegment.Type.Move,
                floatArrayOf(segment.p0!!.x, segment.p0!!.y),
                0.0f
            )
            PathVerb.LINE -> PathSegment(
                PathSegment.Type.Line,
                floatArrayOf(
                    segment.p0!!.x, segment.p0!!.y,
                    segment.p1!!.x, segment.p1!!.y,
                ),
                0.0f
            )
            PathVerb.QUAD -> PathSegment(
                PathSegment.Type.Quadratic,
                floatArrayOf(
                    segment.p0!!.x, segment.p0!!.y,
                    segment.p1!!.x, segment.p1!!.y,
                    segment.p2!!.x, segment.p2!!.y,
                ),
                0.0f
            )
            // TODO: convert conics to quadratics when conicEvaluation is set to AsQuadratics
            PathVerb.CONIC -> PathSegment(
                PathSegment.Type.Quadratic,
                floatArrayOf(
                    segment.p0!!.x, segment.p0!!.y,
                    segment.p1!!.x, segment.p1!!.y,
                    segment.p2!!.x, segment.p2!!.y,
                ),
                segment.conicWeight
            )
            PathVerb.CUBIC -> PathSegment(
                PathSegment.Type.Cubic,
                floatArrayOf(
                    segment.p0!!.x, segment.p0!!.y,
                    segment.p1!!.x, segment.p1!!.y,
                    segment.p2!!.x, segment.p2!!.y,
                    segment.p3!!.x, segment.p3!!.y
                ),
                0.0f
            )
            PathVerb.CLOSE -> CloseSegment
            PathVerb.DONE -> DoneSegment
        }
    }
}
