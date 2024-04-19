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

import androidx.compose.ui.graphics.vector.PathParser

/**
 * Adds the specified SVG [path data](https://www.w3.org/TR/SVG2/paths.html#PathData) to
 * this [Path]. The SVG path data encodes a series of instructions that will be applied
 * to this path. For instance, the following path data:
 *
 * `M200,300 Q400,50 600,300 L1000,300`
 *
 * Will generate the following series of instructions for this path:
 *
 * ```
 * moveTo(200f, 300f)
 * quadraticTo(400f, 50f, 600f, 300f)
 * lineTo(1000f, 300f)
 * ```
 *
 * To convert a [Path] to its SVG path data representation, please refer to [Path.toSvg].
 *
 * @throws IllegalArgumentException if the path data contains an invalid instruction
 *
 * @see toSvg
 */
fun Path.addSvg(pathData: String) {
    // TODO: PathParser will allocate a bunch of PathNodes which aren't necessary here,
    //       we should instead have an internal version of parsePathString() that adds
    //       commands directly to a path without creating intermediate nodes
    PathParser().parsePathString(pathData).toPath(this)
}

/**
 * Returns an SVG representation of this path. The caller can choose whether the returned
 * SVG represents a fully-formed SVG document or only the
 * [path data](https://www.w3.org/TR/SVG2/paths.html#PathData). By default, only the path
 * data is returned which can be used either with [Path.addSvg] or
 * [androidx.compose.ui.graphics.vector.PathParser].
 *
 * @param asDocument When set to true, this function returns a fully-formed SVG document,
 *        otherwise returns only the path data.
 *
 * @see androidx.compose.ui.graphics.vector.PathParser
 * @see addSvg
 */
fun Path.toSvg(asDocument: Boolean = false) = buildString {
    val bounds = this@toSvg.getBounds()

    if (asDocument) {
        append("""<svg xmlns="http://www.w3.org/2000/svg" """)
        appendLine("""viewBox="${bounds.left} ${bounds.top} ${bounds.width} ${bounds.height}">""")
    }

    val iterator = this@toSvg.iterator()
    val points = FloatArray(8)
    var lastType = PathSegment.Type.Done

    if (iterator.hasNext()) {
        if (asDocument) {
            if (this@toSvg.fillType == PathFillType.EvenOdd) {
                append("""  <path fill-rule="evenodd" d="""")
            } else {
                append("""  <path d="""")
            }
        }

        while (iterator.hasNext()) {
            val type = iterator.next(points)
            when (type) {
                PathSegment.Type.Move -> {
                    append("${command(PathSegment.Type.Move, lastType)}${points[0]} ${points[1]}")
                }
                PathSegment.Type.Line -> {
                    append("${command(PathSegment.Type.Line, lastType)}${points[2]} ${points[3]}")
                }
                PathSegment.Type.Quadratic -> {
                    append(command(PathSegment.Type.Quadratic, lastType))
                    append("${points[2]} ${points[3]} ${points[4]} ${points[5]}")
                }
                PathSegment.Type.Conic -> continue // We convert conics to quadratics
                PathSegment.Type.Cubic -> {
                    append(command(PathSegment.Type.Cubic, lastType))
                    append("${points[2]} ${points[3]} ")
                    append("${points[4]} ${points[5]} ")
                    append("${points[6]} ${points[7]}")
                }
                PathSegment.Type.Close -> {
                    append(command(PathSegment.Type.Close, lastType))
                }
                PathSegment.Type.Done -> continue // Won't happen inside this loop
            }
            lastType = type
        }

        if (asDocument) {
            appendLine(""""/>""")
        }
    }
    if (asDocument) {
        appendLine("""</svg>""")
    }
}

private fun command(type: PathSegment.Type, lastType: PathSegment.Type) =
    if (type != lastType) {
        when (type) {
            PathSegment.Type.Move -> "M"
            PathSegment.Type.Line -> "L"
            PathSegment.Type.Quadratic -> "Q"
            PathSegment.Type.Cubic -> "C"
            PathSegment.Type.Close -> "Z"
            else -> ""
        }
    } else " "
