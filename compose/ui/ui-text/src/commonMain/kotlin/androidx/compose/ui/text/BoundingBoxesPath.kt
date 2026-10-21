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

package androidx.compose.ui.text

import androidx.collection.FloatList
import androidx.collection.MutableFloatList
import androidx.collection.MutableLongLongMap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.internal.requirePrecondition
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MAX_CACHED_RANGES = 64

/**
 * Each packed bounding box occupies 4 consecutive [Float] entries: `[left, top, right, bottom]`.
 */
private const val FLOATS_PER_BOX = 4

/**
 * Reusable scratch storage and bounded per-range coordinate cache stored on [MultiParagraph] to
 * compute range bounding boxes and rectilinear contour paths without heap allocations during draw
 * passes.
 */
internal class BoundaryBoxComputeCache {
    /** Packed bounding box coordinates `[left0, top0, right0, bottom0, ...]`. */
    val rectCoords: MutableFloatList = MutableFloatList()

    /** Packed clockwise polygon vertex coordinates `[x0, y0, x1, y1, ...]`. */
    val vertices: MutableFloatList = MutableFloatList()

    /**
     * Maps a 64-bit `rangeKey` packed via [packInts] (`start` in the upper 32 bits, `end` in the
     * lower 32 bits) to a 64-bit slice descriptor packed via [packInts] (`poolStart` in the upper
     * 32 bits, `floatCount` in the lower 32 bits, unpacked with [unpackInt1] and [unpackInt2])
     * within [cachedCoordsPool].
     *
     * Stores primitive `Long -> Long` entries in [MutableLongLongMap] to avoid boxing [TextRange]
     * or `Pair<Int, Int>` keys and skip repeated platform span/line measurements across redraws.
     */
    val rangeCoordsCache: MutableLongLongMap = MutableLongLongMap()

    /** Flat backing pool holding cached `[left, top, right, bottom, ...]` slices. */
    val cachedCoordsPool: MutableFloatList = MutableFloatList()
}

/**
 * Number of packed bounding boxes in this [FloatList].
 *
 * Every 4 consecutive floats represent one `[left, top, right, bottom]` box, so `size / 4` yields
 * the total box count.
 */
internal val FloatList.boxCount: Int
    get() = size / FLOATS_PER_BOX

/** Returns the `left` coordinate at index `boxIndex * 4`. */
internal fun FloatList.boxLeft(boxIndex: Int): Float = get(boxIndex * FLOATS_PER_BOX)

/** Returns the `top` coordinate at index `boxIndex * 4 + 1`. */
internal fun FloatList.boxTop(boxIndex: Int): Float = get(boxIndex * FLOATS_PER_BOX + 1)

/** Returns the `right` coordinate at index `boxIndex * 4 + 2`. */
internal fun FloatList.boxRight(boxIndex: Int): Float = get(boxIndex * FLOATS_PER_BOX + 2)

/** Returns the `bottom` coordinate at index `boxIndex * 4 + 3`. */
internal fun FloatList.boxBottom(boxIndex: Int): Float = get(boxIndex * FLOATS_PER_BOX + 3)

/** Appends `[left, top, right, bottom]` as 4 consecutive floats to this [MutableFloatList]. */
internal fun MutableFloatList.addBox(left: Float, top: Float, right: Float, bottom: Float) {
    add(left)
    add(top)
    add(right)
    add(bottom)
}

/**
 * Overwrites the 4 box coordinates `[left, top, right, bottom]` starting at index `boxIndex * 4`.
 */
internal fun MutableFloatList.setBox(
    boxIndex: Int,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    val base = boxIndex * FLOATS_PER_BOX
    this[base] = left
    this[base + 1] = top
    this[base + 2] = right
    this[base + 3] = bottom
}

/**
 * Appends coalesced per-line bounding box coordinates (`[left, top, right, bottom]` per box) for
 * the local character range from [startOffset] (inclusive) to [endOffset] (exclusive) in this
 * [Paragraph] into [target], shifted vertically by [paragraphTop].
 */
internal expect fun Paragraph.fillRangeBoundingBoxes(
    startOffset: Int,
    endOffset: Int,
    paragraphTop: Float,
    target: MutableFloatList,
)

/**
 * Appends a clockwise axis-aligned rectangle `[left, top, right, bottom]` directly to this [Path].
 */
internal expect fun Path.addClockwiseRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
)

/**
 * Appends a clockwise rounded rectangle `[left, top, right, bottom]` with uniform corner radii
 * `([radiusX], [radiusY])` directly to this [Path].
 */
internal expect fun Path.addClockwiseRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radiusX: Float,
    radiusY: Float,
)

/**
 * Populates [target] in-place with flat coalesced per-line bounding box coordinates for `[start,
 * end)`.
 *
 * Writes `[left0, top0, right0, bottom0, ...]` across all paragraphs intersecting `[start, end)`
 * without allocating `ArrayList<Rect>` or [Rect] instances on the draw path.
 *
 * Algorithm stages:
 * 1. **Reset and validate**: Clear [target] and check `0 <= start <= end <=
 *    layoutInput.text.length`.
 * 2. **Fast exit for empty or truncated ranges**: Return immediately with an empty [target] when
 *    the range is collapsed (`start == end`), the layout has no lines or paragraphs, or [start]
 *    falls at or past the end of the last laid-out paragraph (for example, truncated by
 *    `maxLines`).
 * 3. **Cache lookup (`rangeKey -> cachedCoordsPool` slice)**: Pack [start] (upper 32 bits) and
 *    [end] (lower 32 bits) into a 64-bit `Long` `rangeKey` via [packInts] and look it up in
 *    [BoundaryBoxComputeCache.rangeCoordsCache]. On a hit (`cachedSlice != -1L`), unpack
 *    `poolStart` ([unpackInt1]) and `floatCount` ([unpackInt2]) and copy that uninflated slice from
 *    [BoundaryBoxComputeCache.cachedCoordsPool] into [target], skipping platform [Paragraph] line
 *    and run measurements on subsequent draw passes.
 * 4. **Paragraph traversal (cache miss)**: Walk all paragraphs overlapping `TextRange(start, end)`
 *    via [findParagraphsByRange], converting `[start, end)` to local paragraph offsets and
 *    appending each paragraph's coalesced per-line boxes (shifted vertically by
 *    [ParagraphInfo.top]) into [target].
 * 5. **Bounded cache population**: Copy [target]'s uninflated coordinates into
 *    [BoundaryBoxComputeCache.cachedCoordsPool] under `rangeKey` (clearing both
 *    [BoundaryBoxComputeCache.rangeCoordsCache] and [BoundaryBoxComputeCache.cachedCoordsPool]
 *    first when [MAX_CACHED_RANGES] is reached so memory stays bounded across changing selection
 *    ranges). Storing a separate copy in [BoundaryBoxComputeCache.cachedCoordsPool] ensures callers
 *    that clear or mutate [target] in-place (such as [calculatePathFromBoundingBoxes]) never
 *    corrupt cached entries.
 *
 * @param start inclusive start character offset within [TextLayoutInput.text]
 * @param end exclusive end character offset within [TextLayoutInput.text]
 * @param target scratch [MutableFloatList] cleared and populated in-place with packed `[left, top,
 *   right, bottom]` box coordinates (4 floats per box, ordered top-to-bottom by line and
 *   left-to-right within each line)
 */
internal fun TextLayoutResult.fillRangeBoundingBoxCoords(
    start: Int,
    end: Int,
    target: MutableFloatList,
) {
    // Stage 1: Reset target and validate range bounds against the input text length.
    target.clear()
    val textLength = layoutInput.text.length
    requirePrecondition(start in 0..end && end <= textLength) {
        "Start($start) or End($end) is out of range [0..$textLength], or start > end!"
    }

    // Stage 2: Fast exit for collapsed ranges, empty layouts, or ranges truncated past maxLines.
    val paragraphInfoList = multiParagraph.paragraphInfoList
    if (
        start == end ||
            lineCount == 0 ||
            paragraphInfoList.isEmpty() ||
            start >= paragraphInfoList.last().endIndex
    ) {
        return
    }

    // Stage 3: Look up [start, end) in the primitive Long -> Long coordinate cache.
    // rangeKey packs start (upper 32 bits) and end (lower 32 bits) into a single Long key so
    // MutableLongLongMap can look up ranges without allocating a boxed key object.
    // cachedSlice packs poolStart (upper 32 bits) and floatCount (lower 32 bits) inside
    // cachedCoordsPool; -1L indicates a cache miss (non-negative ints never pack to -1L).
    val rangeKey = packInts(start, end)
    val cache = multiParagraph.boundaryBoxComputeCache
    val cachedSlice = cache.rangeCoordsCache.getOrDefault(rangeKey, -1L)
    if (cachedSlice != -1L) {
        val poolStart = unpackInt1(cachedSlice)
        val floatCount = unpackInt2(cachedSlice)
        if (floatCount > 0) {
            target.ensureCapacity(floatCount)
            val pool = cache.cachedCoordsPool
            for (idx in poolStart until poolStart + floatCount) {
                target.add(pool[idx])
            }
        }
        return
    }

    // Stage 4: Cache miss — collect coalesced per-line boxes across overlapping paragraphs.
    findParagraphsByRange(paragraphInfoList, TextRange(start, end)) { paragraphInfo ->
        with(paragraphInfo) {
            paragraph.fillRangeBoundingBoxes(
                startOffset = start.toLocalIndex(),
                endOffset = end.toLocalIndex(),
                paragraphTop = top,
                target = target,
            )
        }
    }

    // Stage 5: Evict cache if full, then store an unmutated copy of target under rangeKey.
    if (cache.rangeCoordsCache.size >= MAX_CACHED_RANGES) {
        cache.rangeCoordsCache.clear()
        cache.cachedCoordsPool.clear()
    }
    val poolStart = cache.cachedCoordsPool.size
    val floatCount = target.size
    if (floatCount > 0) {
        cache.cachedCoordsPool.addAll(target)
    }
    cache.rangeCoordsCache[rangeKey] = packInts(poolStart, floatCount)
}

/**
 * Populates [target] with coalesced per-line bounding rectangles for the character range from
 * [start] (inclusive) to [end] (exclusive).
 *
 * Characters on the same line that are visually contiguous (such as characters within the same
 * directional run) are coalesced into a single [Rect] per run. Bidirectional (LTR/RTL) text runs on
 * a single line produce multiple disjoint [Rect]s in left-to-right visual order. Trailing
 * whitespace at line wraps and ellipsized characters are excluded.
 *
 * @param start inclusive start character offset
 * @param end exclusive end character offset
 * @param target [ArrayList] cleared and populated with the resulting bounding boxes
 * @return [target] populated with the bounding boxes for `[start, end)`
 */
internal fun TextLayoutResult.calculateBoundingBoxList(
    start: Int,
    end: Int,
    target: ArrayList<Rect> = ArrayList(),
): ArrayList<Rect> {
    target.clear()
    val coords = multiParagraph.boundaryBoxComputeCache.rectCoords
    fillRangeBoundingBoxCoords(
        start = start,
        end = end,
        target = coords,
    )
    val count = coords.boxCount
    if (count == 0) return target

    target.ensureCapacity(count)
    for (i in 0 until count) {
        target.add(
            Rect(
                left = coords.boxLeft(i),
                top = coords.boxTop(i),
                right = coords.boxRight(i),
                bottom = coords.boxBottom(i),
            )
        )
    }
    return target
}

/**
 * Populates [target] with a contour [Path] enclosing the bounding boxes for the character range
 * from [start] (inclusive) to [end] (exclusive), applying optional [cornerRadius] and [mergeLines].
 *
 * Algorithm stages:
 * 1. Obtain the packed per-line bounding box coordinates via [fillRangeBoundingBoxCoords] using
 *    [MultiParagraph.boundaryBoxComputeCache].
 * 2. When [mergeLines] is `false`, emit each box as an independent rectangle or rounded rectangle
 *    via [addRectToPath].
 * 3. When [mergeLines] is `true`, iterate through the boxes line by line:
 *     - If a line has multiple disjoint BiDi runs (`lineEnd - i > 1`), emit each box on that line
 *       as an independent rectangle or rounded rectangle.
 *     - If a line has a single run, greedily collect all consecutive single-run lines that are
 *       vertically adjacent and horizontally overlapping via [canMergeWithPreviousLine], and emit
 *       their unified contour via [mergeAndAddConnectingLinesToPath].
 */
internal fun TextLayoutResult.calculatePathFromBoundingBoxes(
    start: Int,
    end: Int,
    cornerRadius: CornerRadius = CornerRadius.Zero,
    mergeLines: Boolean = true,
    target: Path = Path(),
): Path {
    // Rewind unsets the fill type so reset it here.
    val fillType = target.fillType
    target.rewind()
    target.fillType = fillType
    val cache = multiParagraph.boundaryBoxComputeCache
    val boxes = cache.rectCoords
    fillRangeBoundingBoxCoords(start = start, end = end, target = boxes)
    val count = boxes.boxCount
    if (count == 0) return target

    val hasCornerRadius = cornerRadius.x > 0f || cornerRadius.y > 0f
    if (!mergeLines) {
        for (i in 0 until count) {
            addRectToPath(
                path = target,
                left = boxes.boxLeft(i),
                top = boxes.boxTop(i),
                right = boxes.boxRight(i),
                bottom = boxes.boxBottom(i),
                cornerRadius = cornerRadius,
                hasCornerRadius = hasCornerRadius,
            )
        }
        return target
    }

    var i = 0
    while (i < count) {
        val lineEnd = findIndexOfLastBoxInTheSameLine(boxes, i)
        // If the line has multiple BiDi runs, emit each box on that line as an
        // independent (rounded) rectangle.
        if (lineEnd - i > 1) {
            while (i < lineEnd) {
                addRectToPath(
                    path = target,
                    left = boxes.boxLeft(i),
                    top = boxes.boxTop(i),
                    right = boxes.boxRight(i),
                    bottom = boxes.boxBottom(i),
                    cornerRadius = cornerRadius,
                    hasCornerRadius = hasCornerRadius,
                )
                i++
            }
        } else {
            val stackStart = i
            i++
            while (
                i < count &&
                    // Check if the next line is a single-run line
                    findIndexOfLastBoxInTheSameLine(boxes, i) == i + 1 &&
                    // and can be merged with the previous line.
                    canMergeWithPreviousLine(boxes, i - 1, i)
            ) {
                // Merge the next line with the previous line and continue.
                i++
            }
            // Emit the merged line stack.
            mergeAndAddConnectingLinesToPath(
                path = target,
                boxes = boxes,
                stackStartIndex = stackStart,
                stackEndIndex = i,
                vertices = cache.vertices,
                cornerRadius = cornerRadius,
                hasCornerRadius = hasCornerRadius,
            )
        }
    }
    return target
}

/**
 * Returns the exclusive end box index of the contiguous slice of [boxes] starting at
 * [lineStartIndex] that belong to the same visual text line (matching `top` and `bottom` within
 * `0.5px`).
 */
private fun findIndexOfLastBoxInTheSameLine(boxes: FloatList, lineStartIndex: Int): Int {
    val firstTop = boxes.boxTop(lineStartIndex)
    val firstBottom = boxes.boxBottom(lineStartIndex)
    var index = lineStartIndex + 1
    val count = boxes.boxCount
    while (
        index < count &&
            abs(boxes.boxTop(index) - firstTop) < 0.5f &&
            abs(boxes.boxBottom(index) - firstBottom) < 0.5f
    ) {
        index++
    }
    return index
}

/**
 * Returns `true` if the box at [currIndex] is on the line immediately following the box at
 * [prevIndex] and the two boxes overlap horizontally (`max(left) < min(right)`), meaning their
 * contours can be unified into a single rectilinear polygon when `mergeLines = true`.
 */
private fun TextLayoutResult.canMergeWithPreviousLine(
    boxes: FloatList,
    prevIndex: Int,
    currIndex: Int,
): Boolean {
    if (lineCount == 0) return false
    val prevCenterY = (boxes.boxTop(prevIndex) + boxes.boxBottom(prevIndex)) * 0.5f
    val currCenterY = (boxes.boxTop(currIndex) + boxes.boxBottom(currIndex)) * 0.5f
    val prevLine = getLineForVerticalPosition(prevCenterY)
    val currLine = getLineForVerticalPosition(currCenterY)
    val hasHorizontalOverlap =
        max(boxes.boxLeft(prevIndex), boxes.boxLeft(currIndex)) <
            min(boxes.boxRight(prevIndex), boxes.boxRight(currIndex))
    // only merge if the boxes are on consecutive lines and overlap horizontally
    return currLine == prevLine + 1 && hasHorizontalOverlap
}

/**
 * Appends `[left, top, right, bottom]` to [path] as a clockwise rounded rectangle (when
 * [hasCornerRadius] is `true`) or rectangle (when [hasCornerRadius] is `false`) if its height is
 * positive.
 */
private fun addRectToPath(
    path: Path,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: CornerRadius,
    hasCornerRadius: Boolean,
) {
    // Drop any box whose resulting height is `<= 0f`.
    if (bottom <= top) return
    if (hasCornerRadius) {
        path.addClockwiseRoundRect(left, top, right, bottom, cornerRadius.x, cornerRadius.y)
    } else {
        path.addClockwiseRect(left, top, right, bottom)
    }
}

/**
 * Converts a stack of vertically consecutive, horizontally overlapping single-line boxes in
 * `boxes[stackStartIndex until stackEndIndex]` into a single unified clockwise rectilinear polygon
 * contour, rounds its convex and concave corners via [addPolygonWithRoundedCorners], and appends it
 * to [path].
 *
 * How the rectilinear polygon perimeter is constructed (clockwise from top-left):
 * ```
 *   v0 (firstLeft, minTop)                  v1 (firstRight, minTop)
 *    +---------------------------------------+
 *    |                 Line 0                |
 *    |                              v3       + v2 (currRight, seamY0)
 *    |                               +-------+
 *    |                 Line 1        |
 *    |                               +----+ v5 (nextRight, seamY1)
 *    +-----------+                  v4    |
 *   v9          v8 (currLeft,             |
 *                |  seamY1)    Line 2     |
 *                +------------------------+
 *               v7 (currLeft,            v6 (currRight,
 *                   maxBottom)               maxBottom)
 * ```
 * 1. **Top edge (`v0 -> v1`)**: Start at the top-left of the first line `(firstLeft, minTop)` and
 *    walk to its top-right `(currRight, minTop)`.
 * 2. **Right boundary (`v1 -> ... -> v6`, top-to-bottom)**: Walk from `stackStartIndex` to
 *    `stackEndIndex - 1`. Whenever the next line's right edge differs from `currRight` by at least
 *    `0.5px`, emit two 90-degree step vertices at the horizontal seam `seamY`: `(currRight, seamY)`
 *    followed by `(nextRight, seamY)` (such as `v2 -> v3` and `v4 -> v5`). If the right edges are
 *    within `0.5px`, no intermediate vertices are emitted along that seam.
 * 3. **Bottom edge (`v6 -> v7`)**: At the bottom of the last line `stackEndIndex - 1`, walk from
 *    its bottom-right `(currRight, maxBottom)` to its bottom-left `(currLeft, maxBottom)`.
 * 4. **Left boundary (`v7 -> ... -> v0`, bottom-to-top)**: Walk from `stackEndIndex - 1` back to
 *    `stackStartIndex`. Whenever the previous line's left edge differs from `currLeft` by at least
 *    `0.5px`, emit two 90-degree step vertices at the horizontal seam `seamY`: `(currLeft, seamY)`
 *    followed by `(prevLeft, seamY)` (such as `v8 -> v9`). Collinear left edges within `0.5px`
 *    (such as Line 1 and Line 0 along `v9 -> v0`) continue straight up to `v0` without step
 *    vertices.
 */
private fun mergeAndAddConnectingLinesToPath(
    path: Path,
    boxes: FloatList,
    stackStartIndex: Int,
    stackEndIndex: Int,
    vertices: MutableFloatList,
    cornerRadius: CornerRadius,
    hasCornerRadius: Boolean,
) {
    val lineCount = stackEndIndex - stackStartIndex
    if (lineCount <= 0) return

    // If there is only one line, just add the rectangle to the path.
    if (lineCount == 1) {
        addRectToPath(
            path = path,
            left = boxes.boxLeft(stackStartIndex),
            top = boxes.boxTop(stackStartIndex),
            right = boxes.boxRight(stackStartIndex),
            bottom = boxes.boxBottom(stackStartIndex),
            cornerRadius = cornerRadius,
            hasCornerRadius = hasCornerRadius,
        )
        return
    }

    val lastBoxIndex = stackEndIndex - 1
    val minTop = boxes.boxTop(stackStartIndex)
    val maxBottom = boxes.boxBottom(lastBoxIndex)

    // If the line stack has no height, do not add anything to the path.
    if (maxBottom <= minTop) return

    // Reuse scratch vertex list. The maximum number of vertices is 8 * lineCount (16 * lineCount
    // floats), when each line has two horizontal seams and two corners.
    vertices.clear()

    val firstLeft = boxes.boxLeft(stackStartIndex)
    var currRight = boxes.boxRight(stackStartIndex)

    // Add the top-left and top-right vertices of the first line.
    vertices.add(firstLeft)
    vertices.add(minTop)
    vertices.add(currRight)
    vertices.add(minTop)

    // Add the vertices along the right side of the line stack.
    for (i in stackStartIndex until lastBoxIndex) {
        val nextRight = boxes.boxRight(i + 1)
        // If the next line's right edge differs from the current line's right edge by at least
        // 0.5px, add the two 90-degree step vertices at the horizontal seam.
        if (abs(currRight - nextRight) >= 0.5f) {
            val seamY =
                ((boxes.boxBottom(i) + boxes.boxTop(i + 1)) / 2f).coerceIn(minTop, maxBottom)
            vertices.add(currRight)
            vertices.add(seamY)
            vertices.add(nextRight)
            vertices.add(seamY)
            currRight = nextRight
        }
    }

    // Add the bottom-right and bottom-left vertices of the last line.
    val lastLeft = boxes.boxLeft(lastBoxIndex)
    var currLeft = if (abs(lastLeft - firstLeft) < 0.5f) firstLeft else lastLeft
    vertices.add(currRight)
    vertices.add(maxBottom)
    vertices.add(currLeft)
    vertices.add(maxBottom)

    // Add the vertices along the left side of the line stack.
    for (i in lastBoxIndex downTo stackStartIndex + 1) {
        val rawPrevLeft = boxes.boxLeft(i - 1)
        // If the previous line's left edge is within 0.5px of the first line's left edge,
        // consider them to be the same.
        val prevLeft = if (abs(rawPrevLeft - firstLeft) < 0.5f) firstLeft else rawPrevLeft
        // If the previous line's left edge differs from the current line's left edge by at least
        // 0.5px, add the two 90-degree step vertices at the horizontal seam.
        if (abs(currLeft - prevLeft) >= 0.5f) {
            val seamY =
                ((boxes.boxBottom(i - 1) + boxes.boxTop(i)) / 2f).coerceIn(minTop, maxBottom)
            vertices.add(currLeft)
            vertices.add(seamY)
            vertices.add(prevLeft)
            vertices.add(seamY)
            currLeft = prevLeft
        }
    }

    // Add the polygon to the path, rounding the corners if necessary.
    addPolygonWithRoundedCorners(
        path = path,
        vertices = vertices,
        vertexCount = vertices.size / 2,
        cornerRadius = cornerRadius,
        hasCornerRadius = hasCornerRadius,
    )
}

/**
 * Appends a closed rectilinear polygon defined by the [vertexCount] clockwise vertices in
 * [vertices] (packed as `[x0, y0, x1, y1, ...]`) to [path], rounding each 90-degree corner (both
 * outer convex corners and inner concave step corners) with a quadratic Bézier curve when
 * [hasCornerRadius] is `true`.
 *
 * Corner rounding geometry:
 * - For each vertex `i`, let `prev = (i - 1) mod V` and `next = (i + 1) mod V`. Because the polygon
 *   is axis-aligned (rectilinear), one incident edge is horizontal (`maxRx = (|dxIn| + |dxOut|) /
 *   2f`) and the other is vertical (`maxRy = (|dyIn| + |dyOut|) / 2f`), with axis-aligned unit
 *   direction vectors `(uInX, uInY)` and `(uOutX, uOutY)`.
 * - To prevent adjacent rounded corners on a short horizontal or vertical edge from overlapping
 *   while preserving the elliptical aspect ratio of [cornerRadius] (`x != y`), both `(rx, ry)` are
 *   scaled down by the same uniform factor `scale = min(1f, maxRx / rawRx, maxRy / rawRy)` whenever
 *   either requested radius exceeds half of its corresponding axis edge length.
 * - The straight segment from the previous vertex ends at the corner entry point `(currX - rx *
 *   uInX, currY - ry * uInY)`, and `quadraticTo(currX, currY, exitX, exitY)` uses the sharp vertex
 *   `(currX, currY)` as the Bézier control point to curve smoothly into the exit point `(currX +
 *   rx * uOutX, currY + ry * uOutY)`. This identically rounds both convex outer corners and concave
 *   inner step corners.
 */
private fun addPolygonWithRoundedCorners(
    path: Path,
    vertices: FloatList,
    vertexCount: Int,
    cornerRadius: CornerRadius,
    hasCornerRadius: Boolean,
) {
    if (vertexCount < 3) return

    if (!hasCornerRadius) {
        // If there is no corner rounding, just add the vertices to the path.
        path.moveTo(vertices[0], vertices[1])
        for (i in 1 until vertexCount) {
            path.lineTo(vertices[i * 2], vertices[i * 2 + 1])
        }
        path.close()
        return
    }

    for (i in 0 until vertexCount) {
        val prev = (i - 1 + vertexCount) % vertexCount
        val next = (i + 1) % vertexCount
        val currX = vertices[i * 2]
        val currY = vertices[i * 2 + 1]

        val dxIn = currX - vertices[prev * 2]
        val dyIn = currY - vertices[prev * 2 + 1]
        val lenIn = abs(dxIn) + abs(dyIn)

        val dxOut = vertices[next * 2] - currX
        val dyOut = vertices[next * 2 + 1] - currY
        val lenOut = abs(dxOut) + abs(dyOut)

        if (lenIn < 0.01f || lenOut < 0.01f) {
            if (i == 0) {
                path.moveTo(currX, currY)
            } else {
                path.lineTo(currX, currY)
            }
        } else {
            val uInX = dxIn / lenIn
            val uInY = dyIn / lenIn
            val uOutX = dxOut / lenOut
            val uOutY = dyOut / lenOut

            val maxRx = (abs(dxIn) + abs(dxOut)) / 2f
            val maxRy = (abs(dyIn) + abs(dyOut)) / 2f
            val rawRx = max(0f, cornerRadius.x)
            val rawRy = max(0f, cornerRadius.y)
            var scale = 1f
            if (rawRx > maxRx) {
                scale = min(scale, maxRx / rawRx)
            }
            if (rawRy > maxRy) {
                scale = min(scale, maxRy / rawRy)
            }
            val rx = rawRx * scale
            val ry = rawRy * scale

            val entryX = currX - rx * uInX
            val entryY = currY - ry * uInY
            val exitX = currX + rx * uOutX
            val exitY = currY + ry * uOutY

            if (i == 0) {
                path.moveTo(entryX, entryY)
            } else {
                path.lineTo(entryX, entryY)
            }
            path.quadraticTo(currX, currY, exitX, exitY)
        }
    }
    path.close()
}
