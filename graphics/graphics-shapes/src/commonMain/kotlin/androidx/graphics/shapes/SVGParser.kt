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

package androidx.graphics.shapes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Converts each command (beside move to) of a svg path to a list of [Cubic]s by calling
 * [SVGPathParser.parse]. Any svg complying to the specification found at
 * https://www.w3.org/TR/SVG/paths.html is supported. Parameters can either be split by whitespace
 * or by commas. There is very little error handling, so use with valid paths and consult the debug
 * logs for unexpected cubics.
 *
 * TODO: b/370041761 Remove manual parsing and low level operations when multiplatform SVG parser
 *   exists
 */
internal class SVGPathParser private constructor(startPosition: Point) {

    companion object {

        /**
         * Converts the path elements of svgPath to their cubic counterparts. svgPath corresponds to
         * the data found in the path's "d" property.
         */
        internal fun parse(svgPath: String): List<Cubic> {
            val paths = svgPath.split(Regex("(?=[mM])")).filter { it.isNotBlank() }
            var current = Point(0f, 0f)

            // The input may contain multiple move to commands, in which later ones can be
            // relative. Therefore we need to finish one path before we parse the following,
            // so we have the correct start positions
            return buildList {
                paths.forEach { path ->
                    val commandStrings =
                        path.split(Regex("(?=[a-zA-Z])")).filter { it.isNotBlank() }

                    // Paths start with move commands that define the starting position
                    // Subsequent pairs are equal to line commands
                    val moveToCommand = Command.parse(commandStrings.first(), current)
                    current = moveToCommand.start + Point(moveToCommand[0], moveToCommand[1])

                    val parser = SVGPathParser(current)

                    // Move to command already handled, handle subsequent line commands (if any)
                    parser.parseCommand(moveToCommand.asLine(current))
                    commandStrings.drop(1).forEach {
                        parser.parseCommand(Command.parse(it, parser.position))
                    }

                    addAll(parser.cubics)
                }
            }
        }
    }

    private val cubics: MutableList<Cubic> = mutableListOf()

    private val start: Point = startPosition

    private val position: Point
        get() = cubics.lastOrNull()?.let { Point(it.anchor1X, it.anchor1Y) } ?: start

    private var previousCommand: Command = Command('I', false, floatArrayOf(), 0)

    private val reflectedPreviousControlPoint: Point
        get() = position + (position - Point(cubics.last().control1X, cubics.last().control1Y))

    private fun parseCommand(command: Command) {
        if (command.isCloseCommand) {
            cubics.add(lineToCubic(position, start))
            return
        }

        // A single SVG command can contain multiple parameter pairs. Split them into atomics.
        for (i in 0..command.parameters.lastIndex step command.paramsCount) {
            val atomicCommand = command.chunk(i, position)

            parseAtomicCommand(atomicCommand)
        }
    }

    private fun parseAtomicCommand(atomicCommand: Command) {
        when {
            atomicCommand.isLineCommand -> parseLine(atomicCommand)
            atomicCommand.isCurveCommand -> parseCurve(atomicCommand)
            atomicCommand.isArcCommand -> parseArc(atomicCommand)
            else -> {
                debugLog(LOG_TAG) { "Ignoring unknown command: ${atomicCommand.letter}" }
            }
        }

        previousCommand = atomicCommand
    }

    private fun parseLine(command: Command) {
        val addLineTo = { endPoint: Point -> cubics.add(lineToCubic(position, endPoint)) }

        when (command.letter) {
            'l' -> addLineTo(command.xy(0, 1))
            'h' -> addLineTo(Point(command.x(0), command.start.y))
            'v' -> addLineTo(Point(command.start.x, command.y(0)))
        }
    }

    private fun parseCurve(command: Command) {
        val addCurveWith = { c0: Point, c1: Point, a1: Point ->
            cubics.add(curveToCubic(position, c0, c1, a1))
        }

        when (command.letter) {
            'c' ->
                addCurveWith(
                    command.xy(0, 1),
                    command.xy(2, 3),
                    command.xy(4, 5),
                )
            's' -> {
                val c0 =
                    if (previousCommand.isBezierCommand) reflectedPreviousControlPoint else position

                addCurveWith(
                    c0,
                    command.xy(0, 1),
                    command.xy(2, 3),
                )
            }
            'q' ->
                addCurveWith(
                    command.xy(0, 1),
                    command.xy(0, 1),
                    command.xy(2, 3),
                )
            't' -> {
                val c0 =
                    if (previousCommand.isQuadraticCurveCommand) reflectedPreviousControlPoint
                    else position

                addCurveWith(c0, c0, command.xy(0, 1))
            }
        }
    }

    private fun parseArc(command: Command) {
        val target = command.xy(5, 6)

        cubics.addAll(
            ArcConverter.arcToCubics(
                position.x,
                position.y,
                target.x,
                target.y,
                command[0],
                command[1],
                command[2],
                command[3] != 0f,
                command[4] != 0f,
            )
        )
    }

    private fun curveToCubic(a0: Point, c0: Point, c1: Point, a1: Point) =
        Cubic(floatArrayOf(a0.x, a0.y, c0.x, c0.y, c1.x, c1.y, a1.x, a1.y))

    private fun lineToCubic(start: Point, end: Point) =
        Cubic.straightLine(start.x, start.y, end.x, end.y)

    private data class Command(
        val letter: Char,
        val isRelative: Boolean,
        val parameters: FloatArray,
        val paramsCount: Int,
        val start: Point = Point(0f, 0f)
    ) {
        companion object Factory {
            private val commandToParamsCount =
                mapOf(
                    'm' to 2,
                    'l' to 2,
                    'h' to 1,
                    'v' to 1,
                    'c' to 6,
                    's' to 4,
                    'q' to 4,
                    't' to 2,
                    'a' to 7,
                )

            fun parse(input: String, currentPosition: Point): Command {
                val letter = input.first()
                val isRelative = letter.isLowerCase()
                val parameters =
                    input
                        .drop(1)
                        .split(" ", ",")
                        .filter { it.isNotBlank() }
                        .map { it.trim().toFloat() }
                        .toFloatArray()
                return Command(
                    letter.lowercaseChar(),
                    isRelative,
                    parameters,
                    commandToParamsCount[letter.lowercaseChar()] ?: 0,
                    if (isRelative) currentPosition else Point(0f, 0f)
                )
            }
        }

        val isLineCommand = letter in charArrayOf('l', 'h', 'v')
        val isBezierCommand = letter in charArrayOf('c', 's')
        val isQuadraticCurveCommand = letter in charArrayOf('q', 't')
        val isCurveCommand = letter in charArrayOf('c', 's', 'q', 't')
        val isArcCommand = letter == 'a'
        val isCloseCommand = letter == 'z'

        operator fun get(i: Int): Float = parameters[i]

        fun x(i: Int): Float {
            val coordinate = get(i)
            return if (isRelative) start.x + coordinate else coordinate
        }

        fun y(i: Int): Float {
            val coordinate = get(i)
            return if (isRelative) start.y + coordinate else coordinate
        }

        fun xy(i: Int, j: Int): Point {
            val coordinates = Point(get(i), get(j))
            return if (isRelative) start + coordinates else coordinates
        }

        fun chunk(index: Int, currentPosition: Point): Command =
            Command(
                letter,
                isRelative,
                parameters.sliceArray(index until index + paramsCount),
                paramsCount,
                currentPosition
            )

        fun asLine(newStart: Point): Command {
            val convertedParameters = parameters.drop(paramsCount).toFloatArray()
            return Command('l', isRelative, convertedParameters, 2, newStart)
        }

        // Even though equals and hashCode are not used, they are required by the linter.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Command

            if (letter != other.letter) return false
            if (!parameters.contentEquals(other.parameters)) return false
            if (paramsCount != other.paramsCount) return false

            return true
        }

        override fun hashCode(): Int {
            var result = letter.hashCode()
            result = 31 * result + parameters.contentHashCode()
            result = 31 * result + paramsCount
            return result
        }
    }
}

private const val LOG_TAG = "SVGParser"

/**
 * The following code has been copied and slightly adjusted from graphics/PathParser to remove the
 * Path dependency.
 *
 * TODO: b/370041761 Remove when multiplatform SVG parser with arc support exists
 */
private class ArcConverter {
    companion object {
        fun arcToCubics(
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            a: Float,
            b: Float,
            theta: Float,
            isMoreThanHalf: Boolean,
            isPositiveArc: Boolean
        ): List<Cubic> {
            /* Convert rotation angle from degrees to radians */
            val thetaD: Double = theta.toDouble() / 180 * PI
            /* Pre-compute rotation matrix entries */
            val cosTheta = cos(thetaD)
            val sinTheta = sin(thetaD)
            /* Transform (x0, y0) and (x1, y1) into unit space */
            /* using (inverse) rotation, followed by (inverse) scale */
            val x0p = (x0 * cosTheta + y0 * sinTheta) / a
            val y0p = (-x0 * sinTheta + y0 * cosTheta) / b
            val x1p = (x1 * cosTheta + y1 * sinTheta) / a
            val y1p = (-x1 * sinTheta + y1 * cosTheta) / b

            /* Compute differences and averages */
            val dx = x0p - x1p
            val dy = y0p - y1p
            val xm = (x0p + x1p) / 2
            val ym = (y0p + y1p) / 2
            /* Solve for intersecting unit circles */
            val dsq = dx * dx + dy * dy
            if (dsq == 0.0) {
                return listOf() /* Points are coincident */
            }
            val disc = 1.0 / dsq - 1.0 / 4.0
            if (disc < 0.0) {
                val adjust = (sqrt(dsq) / 1.99999).toFloat()
                /* Points are too far apart */
                return arcToCubics(
                    x0,
                    y0,
                    x1,
                    y1,
                    a * adjust,
                    b * adjust,
                    theta,
                    isMoreThanHalf,
                    isPositiveArc
                )
            }
            val s = sqrt(disc)
            val sdx = s * dx
            val sdy = s * dy
            var cx: Double
            var cy: Double
            if (isMoreThanHalf == isPositiveArc) {
                cx = xm - sdy
                cy = ym + sdx
            } else {
                cx = xm + sdy
                cy = ym - sdx
            }

            val eta0 = atan2((y0p - cy), (x0p - cx))

            val eta1 = atan2((y1p - cy), (x1p - cx))

            var sweep = (eta1 - eta0)
            if (isPositiveArc != (sweep >= 0)) {
                if (sweep > 0) {
                    sweep -= 2 * PI
                } else {
                    sweep += 2 * PI
                }
            }

            cx *= a.toDouble()
            cy *= b.toDouble()
            val tcx = cx
            cx = cx * cosTheta - cy * sinTheta
            cy = tcx * sinTheta + cy * cosTheta

            return arcToBezier(
                cx.toFloat(),
                cy.toFloat(),
                a,
                b,
                x0,
                y0,
                thetaD.toFloat(),
                eta0.toFloat(),
                sweep.toFloat()
            )
        }

        /**
         * Converts an arc to cubic Bezier segments and records them in p.
         *
         * @param cx The x coordinate center of the ellipse
         * @param cy The y coordinate center of the ellipse
         * @param rx The radius of the ellipse in the horizontal direction
         * @param ry The radius of the ellipse in the vertical direction
         * @param e1x E(eta1) x coordinate of the starting point of the arc
         * @param e1y E(eta2) y coordinate of the starting point of the arc
         * @param theta The angle that the ellipse bounding rectangle makes with horizontal plane
         * @param start The start angle of the arc on the ellipse
         * @param sweep The angle (positive or negative) of the sweep of the arc on the ellipse
         */
        private fun arcToBezier(
            cx: Float,
            cy: Float,
            rx: Float,
            ry: Float,
            e1x: Float,
            e1y: Float,
            theta: Float,
            start: Float,
            sweep: Float
        ): List<Cubic> {
            val cubics = mutableListOf<Cubic>()

            // Taken from equations at: http://spaceroots.org/documents/ellipse/node8.html
            // and http://www.spaceroots.org/documents/ellipse/node22.html
            // Maximum of 45 degrees per cubic Bezier segment
            var ce1x = e1x
            var ce1y = e1y
            val numSegments = (ceil(abs(sweep * 4 / PI))).toInt()

            var eta1 = start
            val cosTheta = cos(theta)
            val sinTheta = sin(theta)
            val cosEta1 = cos(eta1)
            val sinEta1 = sin(eta1)
            var ep1x = (-rx * cosTheta * sinEta1) - (ry * sinTheta * cosEta1)
            var ep1y = (-rx * sinTheta * sinEta1) + (ry * cosTheta * cosEta1)

            val anglePerSegment = sweep / numSegments
            for (i in 0 until numSegments) {
                val eta2 = eta1 + anglePerSegment
                val sinEta2 = sin(eta2)
                val cosEta2 = cos(eta2)
                val e2x = cx + (rx * cosTheta * cosEta2) - (ry * sinTheta * sinEta2)
                val e2y = cy + (rx * sinTheta * cosEta2) + (ry * cosTheta * sinEta2)
                val ep2x = -rx * cosTheta * sinEta2 - ry * sinTheta * cosEta2
                val ep2y = -rx * sinTheta * sinEta2 + ry * cosTheta * cosEta2
                val tanDiff2 = tan((eta2 - eta1) / 2)
                val alpha = sin(eta2 - eta1) * (sqrt(4 + (3 * tanDiff2 * tanDiff2)) - 1) / 3
                val q1x = ce1x + alpha * ep1x
                val q1y = ce1y + alpha * ep1y
                val q2x = e2x - alpha * ep2x
                val q2y = e2y - alpha * ep2y

                cubics.add(Cubic(ce1x, ce1y, q1x, q1y, q2x, q2y, e2x, e2y))
                eta1 = eta2
                ce1x = e2x
                ce1y = e2y
                ep1x = ep2x
                ep1y = ep2y
            }
            return cubics
        }
    }
}
