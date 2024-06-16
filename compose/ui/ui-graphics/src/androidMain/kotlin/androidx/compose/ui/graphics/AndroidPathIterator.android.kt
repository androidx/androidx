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

import androidx.graphics.path.PathIterator as PlatformPathIterator
import androidx.graphics.path.PathIterator.ConicEvaluation as PlatformConicEvaluation
import androidx.graphics.path.PathSegment.Type as PlatformPathSegmentType

actual fun PathIterator(
    path: Path,
    conicEvaluation: PathIterator.ConicEvaluation,
    tolerance: Float
): PathIterator = AndroidPathIterator(path, conicEvaluation, tolerance)

private class AndroidPathIterator(
    override val path: Path,
    override val conicEvaluation: PathIterator.ConicEvaluation,
    override val tolerance: Float
) : PathIterator {
    private val segmentPoints = FloatArray(8)

    private val implementation =
        PlatformPathIterator(
            path.asAndroidPath(),
            when (conicEvaluation) {
                PathIterator.ConicEvaluation.AsConic -> PlatformConicEvaluation.AsConic
                PathIterator.ConicEvaluation.AsQuadratics -> PlatformConicEvaluation.AsQuadratics
            },
            tolerance
        )

    override fun calculateSize(includeConvertedConics: Boolean): Int =
        implementation.calculateSize(includeConvertedConics)

    override fun hasNext(): Boolean = implementation.hasNext()

    override fun next(outPoints: FloatArray, offset: Int): PathSegment.Type =
        implementation.next(outPoints, offset).toPathSegmentType()

    override fun next(): PathSegment {
        val p = segmentPoints
        // Compiler hint to bypass bound checks
        if (p.size < 8) return DoneSegment

        val type = implementation.next(p, 0).toPathSegmentType()
        if (type == PathSegment.Type.Done) return DoneSegment
        if (type == PathSegment.Type.Close) return CloseSegment

        val points =
            when (type) {
                PathSegment.Type.Move -> floatArrayOf(p[0], p[1])
                PathSegment.Type.Line -> floatArrayOf(p[0], p[1], p[2], p[3])
                PathSegment.Type.Quadratic -> floatArrayOf(p[0], p[1], p[2], p[3], p[4], p[5])
                PathSegment.Type.Conic -> floatArrayOf(p[0], p[1], p[2], p[3], p[4], p[5])
                PathSegment.Type.Cubic ->
                    floatArrayOf(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7])
                else -> FloatArray(0) // Unreachable since we tested for Done and Close above
            }

        return PathSegment(type, points, if (type == PathSegment.Type.Conic) p[6] else 0.0f)
    }
}

private fun PlatformPathSegmentType.toPathSegmentType() =
    when (this) {
        PlatformPathSegmentType.Move -> PathSegment.Type.Move
        PlatformPathSegmentType.Line -> PathSegment.Type.Line
        PlatformPathSegmentType.Quadratic -> PathSegment.Type.Quadratic
        PlatformPathSegmentType.Conic -> PathSegment.Type.Conic
        PlatformPathSegmentType.Cubic -> PathSegment.Type.Cubic
        PlatformPathSegmentType.Close -> PathSegment.Type.Close
        PlatformPathSegmentType.Done -> PathSegment.Type.Done
    }
