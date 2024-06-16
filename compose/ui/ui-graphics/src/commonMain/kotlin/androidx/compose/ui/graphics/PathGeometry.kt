/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * Computes this [Path]'s direction (or winding, or orientation), which can be either
 * [Path.Direction.Clockwise] or [Path.Direction.CounterClockwise].
 *
 * If the path is made of multiple contours (the path contains multiple "move" commands), the
 * direction returned by this property is the direction of the first contour.
 *
 * If the path is empty (contains no lines/curves), the direction is [Path.Direction.Clockwise].
 *
 * If the path has no area (single straight line), the direction is [Path.Direction.Clockwise].
 *
 * Calling this property does not cache the result, the direction is computed Calling this method
 * does not cache the result, the direction is computed every time the method is called.
 *
 * If you need to query the direction of individual contours, you should [divide][Path.divide] the
 * path first.
 */
fun Path.computeDirection(): Path.Direction {
    var first = true

    val iterator = iterator()
    val points = FloatArray(8)

    var area = 0.0f

    var startX = 0.0f
    var startY = 0.0f

    var endX = 0.0f
    var endY = 0.0f

    // Compute the signed area of the path by summing the area of each curve inside
    // the path. If the total area is positive, the path is clockwise, otherwise the
    // path is counter-clockwise.
    // See "Computing the area and winding number for a Bézier curve", Jackowski 2012
    // (https://tug.org/TUGboat/tb33-1/tb103jackowski.pdf) for more details.
    // Computing the direction only makes sense for a single contour. If we encounter
    // more than one contour, we return the direction of the first contour.
    // To compute the signed area, we convert lines and quadratic segments to cubic
    // segments.
    var type = iterator.next(points)
    while (type != PathSegment.Type.Done) {
        @Suppress("KotlinConstantConditions")
        when (type) {
            PathSegment.Type.Move -> {
                if (!first) {
                    break
                }

                first = false

                startX = points[0]
                startY = points[1]
            }
            PathSegment.Type.Line -> {
                val x0 = points[0]
                val y0 = points[1]

                val x1 = points[2]
                val y1 = points[3]

                // To compute the area, the placement of the control points does not
                // matter as long as they are on the line. We set them to the start
                // and end points to avoid extra computations.
                area += cubicArea(x0, y0, x0, y0, x1, y1, x1, y1)

                endX = x1
                endY = y1
            }
            PathSegment.Type.Quadratic -> {
                val x0 = points[0]
                val y0 = points[1]

                val x1 = points[2]
                val y1 = points[3]

                val x2 = points[4]
                val y2 = points[5]

                val c1x = x0 + 2.0f / 3.0f * (x1 - x0)
                val c1y = y0 + 2.0f / 3.0f * (y1 - y0)

                val c2x = x2 + 2.0f / 3.0f * (x1 - x2)
                val c2y = y2 + 2.0f / 3.0f * (y1 - y2)

                area += cubicArea(x0, y0, c1x, c1y, c2x, c2y, x2, y2)

                endX = x2
                endY = y2
            }
            PathSegment.Type.Conic -> continue // We convert conics to quadratics
            PathSegment.Type.Cubic -> {
                area +=
                    cubicArea(
                        points[0],
                        points[1],
                        points[2],
                        points[3],
                        points[4],
                        points[5],
                        points[6],
                        points[7]
                    )

                endX = points[6]
                endY = points[7]
            }
            PathSegment.Type.Close -> {
                if (!endX.closeTo(startX) || !endY.closeTo(startY)) {
                    area += cubicArea(endX, endY, endX, endY, startX, startY, startX, startY)

                    endX = startX
                    endY = startY
                }
            }
            PathSegment.Type.Done -> break
        }
        type = iterator.next(points)
    }

    return if (area >= 0.0f) {
        Path.Direction.Clockwise
    } else {
        Path.Direction.CounterClockwise
    }
}

/**
 * Divides this path into a list of paths. Each contour inside this path is returned as a separate
 * [Path]. For instance the following code snippet creates two rectangular contours:
 * ```
 * val p = Path()
 * p.addRect(...)
 * p.addRect(...)
 *
 * val contours = p.divide()
 * ```
 *
 * The list returned by calling `p.divide()` will contain two `Path` instances, each representing
 * one of the two rectangles.
 *
 * Empty contours (contours with no lines/curves) are omitted from the resulting list.
 *
 * @param contours An optional mutable list of [Path] that will hold the result of the division.
 * @return A list of [Path] representing all the contours in this path. The returned list is either
 *   a newly allocated list if the [contours] parameter was left unspecified, or the [contours]
 *   parameter.
 */
fun Path.divide(contours: MutableList<Path> = mutableListOf()): MutableList<Path> {
    var path = Path()

    var first = true
    var isEmpty = true // Path.isEmpty returns true if there's a moveTo()

    val iterator = iterator()
    val points = FloatArray(8)

    var type = iterator.next(points)
    while (type != PathSegment.Type.Done) {
        @Suppress("KotlinConstantConditions")
        when (type) {
            PathSegment.Type.Move -> {
                if (!first && !isEmpty) {
                    contours.add(path)
                    path = Path()
                }
                first = false
                isEmpty = true
                path.moveTo(points[0], points[1])
            }
            PathSegment.Type.Line -> {
                path.lineTo(points[2], points[3])
                isEmpty = false
            }
            PathSegment.Type.Quadratic -> {
                path.quadraticTo(points[2], points[3], points[4], points[5])
                isEmpty = false
            }
            PathSegment.Type.Conic -> continue // We convert conics to quadratics
            PathSegment.Type.Cubic -> {
                path.cubicTo(points[2], points[3], points[4], points[5], points[6], points[7])
                isEmpty = false
            }
            PathSegment.Type.Close -> path.close()
            PathSegment.Type.Done -> continue // Won't happen inside this loop
        }
        type = iterator.next(points)
    }

    if (!first && !isEmpty) {
        contours.add(path)
    }

    return contours
}

/**
 * Reverses the segments of this path into the specified [destination], turning a clockwise path
 * into a counter-clockwise path and vice-versa. Each contour in the path is reversed independently,
 * and the contours appear in the [destination] in reverse order.
 *
 * This method preserves the general structure of this path as much as possible:
 * - Lines become lines
 * - Quadratic Bézier curves become quadratic Bézier curves
 * - Cubic Bézier curves become cubic Bézier curves
 * - Close and move commands remain close and move commands
 * - Conic segments become quadratic Bézier curves
 *
 * @return A [Path] containing the reverse of this [Path]. The returned path is either a newly
 *   allocated [Path] if the [destination] parameter was left unspecified, or the [destination]
 *   parameter.
 */
fun Path.reverse(destination: Path = Path()): Path {
    val iterator = iterator()

    val count = iterator.calculateSize(false)
    val segments = ArrayList<PathSegment.Type>(count)
    val data = ArrayList<FloatArray>(count)

    // Gather all the segments going forward so we can iterate backward
    // to construct the new reversed path. It would be unnecessary if
    // PathIterator supported reverse iteration.
    var points = FloatArray(8)
    var type = iterator.next(points)
    while (type != PathSegment.Type.Done) {
        segments.add(type)
        if (type != PathSegment.Type.Close) {
            data.add(points.copyOf(floatCountForType(type)))
        }
        type = iterator.next(points)
    }

    var insertMove = true
    var insertClose = false
    var dataIndex = data.size

    for (i in segments.size - 1 downTo 0) {
        if (insertMove) {
            dataIndex--
            points = data[dataIndex]
            val offset = points.lastIndex
            destination.moveTo(points[offset - 1], points[offset])
            insertMove = false
        } else {
            points = data[dataIndex]
        }

        when (segments[i]) {
            PathSegment.Type.Move -> {
                if (insertClose) {
                    destination.close()
                    insertClose = false
                }
                insertMove = true
            }
            PathSegment.Type.Line -> {
                destination.lineTo(points[0], points[1])
                dataIndex--
            }
            PathSegment.Type.Quadratic -> {
                destination.quadraticTo(points[2], points[3], points[0], points[1])
                dataIndex--
            }
            PathSegment.Type.Conic -> {} // won't happen, we convert to quadratics
            PathSegment.Type.Cubic -> {
                destination.cubicTo(
                    points[4],
                    points[5],
                    points[2],
                    points[3],
                    points[0],
                    points[1]
                )
                dataIndex--
            }
            PathSegment.Type.Close -> insertClose = true
            PathSegment.Type.Done -> {} // won't happen, we filtered it out in the previous loop
        }
    }

    if (insertClose) {
        destination.close()
    }

    return destination
}

private fun floatCountForType(type: PathSegment.Type) =
    when (type) {
        PathSegment.Type.Move -> 2
        PathSegment.Type.Line -> 4
        PathSegment.Type.Quadratic -> 6
        PathSegment.Type.Conic -> 8 // won't happen
        PathSegment.Type.Cubic -> 8
        PathSegment.Type.Close -> 0
        PathSegment.Type.Done -> 0
    }
