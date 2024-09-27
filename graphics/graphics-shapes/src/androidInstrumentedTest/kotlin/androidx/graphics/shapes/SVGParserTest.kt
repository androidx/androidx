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

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class SVGParserTest {
    @Test
    fun handlesEmptyInput() {
        val path = ""
        val expected = listOf<Cubic>()
        parsingMatches(path, expected)
    }

    @Test
    fun parsesBothParameterSplitsEqually() {
        val commaPath = "M 10,10 L 30,10 L 20,30 z"
        val spacePath = "M 10 10 L 30 10 L 20 30 z"

        parsingIsEqualish(commaPath, spacePath)
    }

    @Test
    fun parsesWhitespacesEqually() {
        val spaced = "M 10,10  L    30,10    L 20,30 z"
        val touching = "M10,10L30,10L20,30z"

        parsingIsEqualish(spaced, touching)
    }

    @Test
    fun parsesAbsoluteMultiplePaths() {
        val path = "M 10,10 30,10 20,30 z M 20,20 50,10 40,30 z"
        val expected =
            listOf(
                Cubic.straightLine(10f, 10f, 30f, 10f),
                Cubic.straightLine(30f, 10f, 20f, 30f),
                Cubic.straightLine(20f, 30f, 10f, 10f),
                Cubic.straightLine(20f, 20f, 50f, 10f),
                Cubic.straightLine(50f, 10f, 40f, 30f),
                Cubic.straightLine(40f, 30f, 20f, 20f),
            )
        parsingMatches(path, expected)
    }

    @Test
    fun parsesRelativeMultiplePaths() {
        val absolute = "M 10,10 30,10 20,30 z M 20,20 50,10 40,30 z"
        val relative = "m 10,10 20,0 -10,20 z m 10,10, 30,-10, -10,20 z"

        parsingIsEqualish(absolute, relative)
    }

    @Test
    fun parsesMultipleAbsoluteMoveTos() {
        val path = "M 10,10 30,10 20,30 z"
        val expected =
            listOf(
                Cubic.straightLine(10f, 10f, 30f, 10f),
                Cubic.straightLine(30f, 10f, 20f, 30f),
                Cubic.straightLine(20f, 30f, 10f, 10f),
            )
        parsingMatches(path, expected)
    }

    @Test
    fun parsesMultipleRelativeMoveTosLikeAbsolute() {
        val absolute = "M 10,10 30,10 20,30 z"
        val relative = "m 10,10 20,0 -10,20 z"

        parsingIsEqualish(absolute, relative)
    }

    @Test
    fun parsesAbsoluteLine() {
        val path = "M 10,10 L 30,10 20,30 z"
        val expected =
            listOf(
                Cubic.straightLine(10f, 10f, 30f, 10f),
                Cubic.straightLine(30f, 10f, 20f, 30f),
                Cubic.straightLine(20f, 30f, 10f, 10f),
            )
        parsingMatches(path, expected)
    }

    @Test
    fun parsesRelativeLineLikeAbsolute() {
        val absolute = "M 10,10 L 30,10 20,30 z"
        val relative = "m 10,10 l 20,0 l -10,20 z"

        parsingIsEqualish(absolute, relative)
    }

    @Test
    fun parsesNegativeFloatingPointParameters() {
        val path = "M-10.5555,10.5L-30.5555,10.5L-20.5555,30z"
        val expected =
            listOf(
                Cubic.straightLine(-10.5555f, 10.5f, -30.5555f, 10.5f),
                Cubic.straightLine(-30.5555f, 10.5f, -20.5555f, 30f),
                Cubic.straightLine(-20.5555f, 30f, -10.5555f, 10.5f),
            )
        parsingMatches(path, expected)
    }

    @Test
    fun parsesAbsoluteHorizontalLikeLine() {
        val linePath = "M 10,10 L 30,10 L 40,10 z"
        val horizontalEquivalent = "M 10,10 H 30 40 z"

        parsingIsEqualish(linePath, horizontalEquivalent)
    }

    @Test
    fun parsesRelativeHorizontalLikeAbsolute() {
        val linePath = "M 10,10 L 30,10 L 40,10 z"
        val horizontalEquivalent = "M 10,10 h 20 10 z"

        parsingIsEqualish(linePath, horizontalEquivalent)
    }

    @Test
    fun parsesAbsoluteVerticalLikeLine() {
        val linePath = "M 10,10 L 10,30 L 10,40 z"
        val verticalEquivalent = "M 10,10 V 30 40 z"

        parsingIsEqualish(linePath, verticalEquivalent)
    }

    @Test
    fun parsesRelativeVerticalLikeAbsolute() {
        val linePath = "M 10,10 L 10,30 L 10,40 z"
        val verticalEquivalent = "M 10,10 v 20 10 z"

        parsingIsEqualish(linePath, verticalEquivalent)
    }

    @Test
    fun parsesAbsoluteCubics() {
        val path = "M 0,0 C 10,10 30,20, 10,10 C 23, 23 48,40 20,20 z"
        val expected =
            listOf(
                Cubic(floatArrayOf(0f, 0f, 10f, 10f, 30f, 20f, 10f, 10f)),
                Cubic(floatArrayOf(10f, 10f, 23f, 23f, 48f, 40f, 20f, 20f)),
                Cubic.straightLine(20f, 20f, 0f, 0f)
            )

        parsingMatches(path, expected)
    }

    @Test
    fun parsesRelativeCubicsLikeAbsolute() {
        val absolute = "M 0,0 C 10,10 30,20, 10,10 C 23, 23 48,40 20,20 z"
        val relativeEquivalent = "M 0,0 c 10,10 30,20, 10,10 c 13, 13 38,30 10,10 z"

        parsingIsEqualish(absolute, relativeEquivalent)
    }

    @Test
    fun parsesAbsoluteSmoothCurveLikeCubic() {
        val smooth = "M 0,0 C 0,10 10,10 10,0 S 20,-10 20,0 z"
        val cubicEquivalent = "M 0,0 C 0,10 10,10 10,0 C 10,-10 20,-10 20,0 z"

        parsingIsEqualish(smooth, cubicEquivalent)
    }

    @Test
    fun parsesRelativeSmoothCurveLikeAbsolute() {
        val absolute = "M 0,0 C 0,10 10,10 10,0 S 20,-10 20,0 z"
        val relativeEquivalent = "M 0,0 C 0,10 10,10 10,0 s 10,-10 10,0 z"

        parsingIsEqualish(absolute, relativeEquivalent)
    }

    @Test
    fun parsesSmoothCurveWithCurrentPositionIfNoPredecessor() {
        val path = "M 10,10 S 20,-10, 20,0"
        val expected =
            listOf(
                Cubic(floatArrayOf(10f, 10f, 10f, 10f, 20f, -10f, 20f, 0f)),
            )
        parsingMatches(path, expected)
    }

    @Test
    fun parsesAbsoluteQuadraticCurveLikeCubic() {
        val qPath = "M 0,0 Q 5,10 10,0 z"
        val curveEquivalent = "M 0,0 C 5,10 5,10 10,0 z"

        parsingIsEqualish(qPath, curveEquivalent)
    }

    @Test
    fun parsesRelativeQuadraticCurveLikeAbsolute() {
        val absolute = "M 10,10 Q 15,20 20,10 z"
        val relative = "M 10,10 q 5, 10 10,0 z"

        parsingIsEqualish(absolute, relative)
    }

    @Test
    fun parsesAbsoluteSmoothQuadraticLikeCubic() {
        val tPath = "M 0,0 Q 5,10 10,0 T 20,0 z"
        val curveEquivalent = "M 0,0 Q 5,10 10,0 C 15,-10 15,-10 20,0 z"

        parsingIsEqualish(tPath, curveEquivalent)
    }

    @Test
    fun parsesRelativeSmoothQuadraticLikeAbsolute() {
        val absolute = "M 0,0 Q 5,10 10,0 T 20,0 z"
        val relative = "M 0,0 Q 5,10 10,0 t 10,0 z"

        parsingIsEqualish(absolute, relative)
    }

    @Test
    fun parsesSmoothQuadraticWithCurrentPositionIfNoPredecessor() {
        val path = "M 10,10 T 20,0"
        val expected =
            listOf(
                Cubic(floatArrayOf(10f, 10f, 10f, 10f, 10f, 10f, 20f, 0f)),
            )
        parsingMatches(path, expected)
    }

    @Test
    fun parsesAbsoluteArc() {
        // A 1/4 segment of a pie chart
        val path = "M300,200 v-150 A150,150 0 0,0 150,200 z"
        val expected =
            listOf(
                Cubic.straightLine(300f, 200f, 300f, 50f),
                Cubic(
                    floatArrayOf(300f, 50f, 273.67145f, 50f, 247.80118f, 56.93192f, 225f, 70.09619f)
                ),
                Cubic(
                    floatArrayOf(
                        225f,
                        70.09619f,
                        202.19882f,
                        83.26046f,
                        183.26047f,
                        102.198814f,
                        170.09619f,
                        125f
                    )
                ),
                Cubic(
                    floatArrayOf(
                        170.09619f,
                        125f,
                        156.93192f,
                        147.80118f,
                        150f,
                        173.67145f,
                        150f,
                        200f
                    )
                ),
                Cubic.straightLine(150f, 200f, 300f, 200f)
            )

        parsingMatches(path, expected)
    }

    @Test
    fun parsesRelativeArcLikeAbsolute() {
        // A 1/4 segment of a pie chart
        val absolute = "M300,200 v-150 A150,150 0 0,0 150,200 z"
        val relative = "M300,200 v-150 a150,150 0 0,0 -150,150 z"

        parsingIsEqualish(absolute, relative)
    }

    @Test
    fun parsesMaterialThreeFavorite() {
        // https://fonts.google.com/icons?selected=Material+Symbols+Outlined:favorite:FILL
        val path =
            """m 480 -120
                |l -58 -52
                |q -101 -91 -167 -157
                |T 150 -447.5
                |Q 111 -500 95.5 -544
                |T 80 -634 q 0 -94 63 -157
                |t 157 -63
                |q 52 0 99 22
                |t 81 62
                |q 34 -40 81 -62
                |t 99 -22
                |q 94 0 157 63
                |t 63 157
                |q 0 46 -15.5 90
                |T 810 -447.5
                |Q 771 -395 705 -329
                |T 538 -172
                |l -58 52
                |Z"""
                .trimMargin()
        val result = SVGPathParser.parse(path)

        assertEquals(result.size, 19)
    }

    @Test
    fun parsesMaterialThreeEco() {
        // https://fonts.google.com/icons?selected=Material+Symbols+Outlined:eco:FILL
        val path =
            """M 450 -80
                |q -33 0 -66.5 -7.5
                |T 315 -109
                |q 12 -121 70 -226
                |t 149 -185
                |q -110 56 -190.5 148
                |T 231 -162
                |q -4 -3 -7.5 -6.5
                |L 216 -176
                |q -47 -47 -71.5 -105
                |T 120 -402
                |q 0 -68 27 -130
                |t 75 -110
                |q 81 -81 210 -105.5
                |t 362 -4.5
                |q 18 239 -6 364.5
                |T 684 -182
                |q -49 49 -109.5 75.5
                |T 450 -80
                |Z"""
                .trimMargin()

        val result = SVGPathParser.parse(path)

        assertEquals(result.size, 19)
    }

    private fun parsingIsEqualish(pathA: String, pathB: String) {
        val pathAResult = SVGPathParser.parse(pathA)
        val pathBResult = SVGPathParser.parse(pathB)

        assertCubicListsEqualish(pathAResult, pathBResult)
    }

    private fun parsingMatches(path: String, expected: List<Cubic>) {
        val actual = SVGPathParser.parse(path)

        assertCubicListsEqualish(expected, actual)
    }
}
