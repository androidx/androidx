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

package androidx.compose.foundation.shape

import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.MutableCubic
import androidx.graphics.shapes.RoundedPolygon

/**
 * Creates a [Shape] that morphs between [start] and [end] as [progress] moves from `0f` to `1f`.
 *
 * Reads [progress] each time the outline is resolved. Values at or below `0f` resolve to [start];
 * values at or above `1f` resolve to [end]. Create a separate instance for each morphing element.
 * Two shapes created with equal endpoints and the same [progress] instance compare equal.
 *
 * @sample androidx.compose.foundation.samples.MorphPolygonShapeSample
 * @param start starting shape of the morph
 * @param end ending shape of the morph
 * @param progress returns the current morph progress
 */
@RememberInComposition
public fun MorphPolygonShape(start: PolygonShape, end: PolygonShape, progress: () -> Float): Shape =
    MorphPolygonShapeImpl(start, end, progress)

private class MorphPolygonShapeImpl(
    val start: PolygonShape,
    val end: PolygonShape,
    val progress: () -> Float,
) : Shape {

    // Single-entry cache keyed on the endpoints' resolved polygon instances: the endpoints'
    // caches return a new instance whenever the resolution inputs or their content change, so an
    // identity match means the Morph is current. Per frame we only interpolate; the two resting
    // outlines are built lazily the first time progress actually rests at a boundary.
    private var morph: Morph? = null
    private var startPolygon: RoundedPolygon? = null
    private var endPolygon: RoundedPolygon? = null
    private var startOutline: Outline? = null
    private var endOutline: Outline? = null

    private var lastProgress: Float = Float.NaN
    private var lastOutline: Outline? = null

    private val path = Path()
    private val mutableCubic = MutableCubic()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val startPoly = start.resolvePolygon(size, layoutDirection, density)
        val endPoly = end.resolvePolygon(size, layoutDirection, density)
        if (startPoly !== startPolygon || endPoly !== endPolygon) {
            morph = Morph(startPoly, endPoly)
            startPolygon = startPoly
            endPolygon = endPoly
            startOutline = null
            endOutline = null
            lastProgress = Float.NaN
            lastOutline = null
        }

        val p = progress()
        // Bypass the morph computation at the resting boundaries.
        if (p <= 0f) {
            return startOutline
                ?: Outline.Generic(startPolygon!!.asComposePath(Path())).also { startOutline = it }
        }
        if (p >= 1f) {
            return endOutline
                ?: Outline.Generic(endPolygon!!.asComposePath(Path())).also { endOutline = it }
        }
        if (p == lastProgress && lastOutline != null) return lastOutline!!

        morph!!.asComposePath(p, path, mutableCubic)
        val outline = Outline.Generic(path)
        lastProgress = p
        lastOutline = outline
        return outline
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MorphPolygonShapeImpl) return false
        return start == other.start && end == other.end && progress === other.progress
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + progress.hashCode()
        return result
    }
}

/**
 * Generates a Compose [Path] from the given [Morph] at the specified [progress].
 *
 * @param progress The interpolation progress between the start and end polygons (usually 0f to 1f).
 * @param path An optional [Path] object to rewind and reuse. If not provided, a new one will be
 *   allocated.
 * @param mutableCubic An optional [MutableCubic] reused while iterating the morph's cubics, to
 *   avoid per-frame allocation. If not provided, a new one will be allocated.
 */
internal fun Morph.asComposePath(
    progress: Float,
    path: Path = Path(),
    mutableCubic: MutableCubic = MutableCubic(),
): Path {
    path.rewind()
    var first = true
    forEachCubic(progress, mutableCubic) { cubic ->
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(
            cubic.control0X,
            cubic.control0Y,
            cubic.control1X,
            cubic.control1Y,
            cubic.anchor1X,
            cubic.anchor1Y,
        )
    }
    path.close()
    return path
}
