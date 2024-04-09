/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.gesture.scrollorientationlocking

import androidx.compose.ui.input.pointer.util.polyFitLeastSquares
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PolyFitLeastSquaresTest {

    @Test
    fun polyFitLeastSquares_linear2PointsSlopeOf0Intercept1_isCorrect() {
        val x = floatArrayOf(0f, 1f)
        val y = floatArrayOf(1f, 1f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(1f, 0f))
    }

    @Test
    fun polyFitLeastSquares_linear2PointsSlopeOf1Intercept0_isCorrect() {
        val x = floatArrayOf(0f, 1f)
        val y = floatArrayOf(0f, 1f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 1f))
    }

    @Test
    fun polyFitLeastSquares_linear2PointsSlopeOf1000000_isCorrect() {
        val x = floatArrayOf(0f, 1f)
        val y = floatArrayOf(0f, 1000000f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 1000000f))
    }

    @Test
    fun polyFitLeastSquares_linear2PointsSlopeOfNegative5_isCorrect() {
        val x = floatArrayOf(0f, 1f)
        val y = floatArrayOf(0f, -5f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(0f, -5f))
    }

    @Test
    fun polyFitLeastSquares_linear2PointsRandom1_isCorrect() {
        val x = floatArrayOf(-8f, -2f)
        val y = floatArrayOf(7f, 5f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(4.33333f, -.33333f))
    }

    @Test
    fun polyFitLeastSquares_linear2PointsRandom2_isCorrect() {
        val x = floatArrayOf(-7f, 4f)
        val y = floatArrayOf(4f, -2f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(.181818f, -.545454f))
    }

    @Test
    fun polyFitLeastSquares_linear2PointsRandom3_isCorrect() {
        val x = floatArrayOf(4f, -6f)
        val y = floatArrayOf(-6f, 0f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(-3.6f, -.6f))
    }

    @Test
    fun polyFitLeastSquares_linear4PointsImperfect_isCorrect() {
        val x = floatArrayOf(0f, 2f, 0f, 2f)
        val y = floatArrayOf(0f, 1f, 2f, 3f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(1f, .5f))
    }

    @Test
    fun polyFitLeastSquares_quadratic3PointsActuallyLinear_isCorrect() {
        val x = floatArrayOf(0f, 1f, 2f)
        val y = floatArrayOf(0f, 1f, 2f)

        val actual = polyFitLeastSquares(x, y, x.size, 2)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 1f, 0f))
    }

    @Test
    fun polyFitLeastSquares_quadratic3Points_isCorrect() {
        val x = floatArrayOf(0f, 1f, 2f)
        val y = floatArrayOf(0f, 1f, 0f)

        val actual = polyFitLeastSquares(x, y, x.size, 2)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 2f, -1f))
    }

    @Test
    fun polyFitLeastSquares_quadratic5Points_isCorrect() {
        val x = floatArrayOf(0f, 1f, 2f, 3f, 4f)
        val y = floatArrayOf(0f, 1f, 4f, 9f, 16f)

        val actual = polyFitLeastSquares(x, y, x.size, 2)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 0f, 1f))
    }

    @Test
    fun polyFitLeastSquares_quadratic4PointsImperfect_isCorrect() {
        val x = floatArrayOf(0f, 1f, 2f, 1f)
        val y = floatArrayOf(0f, -1f, 0f, -2f)

        val actual = polyFitLeastSquares(x, y, x.size, 2)

        assertIsCloseToEquals(actual, floatArrayOf(0f, -3f, 1.5f))
    }

    @Test
    fun polyFitLeastSquares_cubic4PointsActuallyLinear_isCorrect() {
        val x = floatArrayOf(0f, 1f, 2f, 3f)
        val y = floatArrayOf(0f, 1f, 2f, 3f)

        val actual = polyFitLeastSquares(x, y, x.size, 3)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 1f, 0f, 0f))
    }

    @Test
    fun polyFitLeastSquares_cubic4Points_isCorrect() {
        val x = floatArrayOf(-1f, 0f, 1f, 2f)
        val y = floatArrayOf(1f, 0f, 1f, 0f)

        val actual = polyFitLeastSquares(x, y, x.size, 3)

        assertIsCloseToEquals(actual, floatArrayOf(0f, .66666f, 1f, -.66666f))
    }

    @Test
    fun polyFitLeastSquares_cubic6PointsImperfect_isCorrect() {
        val x = floatArrayOf(-1f, 0f, 1f, 2f, 0f, 1f)
        val y = floatArrayOf(1f, 0f, 1f, 0f, 1f, 0f)

        val actual = polyFitLeastSquares(x, y, x.size, 3)

        assertIsCloseToEquals(actual, floatArrayOf(.5f, -.083333f, .25f, -.16666f))
    }

    @Test
    fun polyFitLeastSquares_1Point_isCorrect() {
        val x = floatArrayOf(0f)
        val y = floatArrayOf(13f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(13f, 0f))
    }

    @Test
    fun polyFitLeastSquares_degreeLargerThanData_isCorrect() {
        val x = floatArrayOf(0f, 1f)
        val y = floatArrayOf(0f, 1f)

        val actual = polyFitLeastSquares(x, y, x.size, 2)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 1f, 0f))
    }

    @Test
    fun polyFitLeastSquares_3Points2IdenticalDegree1_isCorrect() {
        val x = floatArrayOf(0f, 0f, 1f)
        val y = floatArrayOf(0f, 0f, 1f)

        val actual = polyFitLeastSquares(x, y, x.size, 1)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 1f))
    }

    @Test
    fun polyFitLeastSquares_degreeIsNegative_throwsIllegalArgumentException() {
        polyFitLeastSquares_degreeIsNegative_throwsIllegalArgumentException(0)
        polyFitLeastSquares_degreeIsNegative_throwsIllegalArgumentException(-1)
        polyFitLeastSquares_degreeIsNegative_throwsIllegalArgumentException(-5)
    }

    private fun polyFitLeastSquares_degreeIsNegative_throwsIllegalArgumentException(
        degree: Int
    ) {
        val x = floatArrayOf(0f, 1f)
        val y = floatArrayOf(0f, 1f)

        val throwable = catchThrowable {
            polyFitLeastSquares(x, y, x.size, degree)
        }

        assertThat(throwable is IllegalArgumentException).isTrue()
    }

    @Test
    fun polyFitLeastSquares_noData_throwsIllegalArgumentException() {
        val x = floatArrayOf()
        val y = floatArrayOf()

        val throwable = catchThrowable {
            polyFitLeastSquares(x, y, x.size, 1)
        }

        assertThat(throwable is IllegalArgumentException).isTrue()
    }

    @Test
    fun polyFitLeastSquares_3Points2IdenticalDegree2() {
        val x = floatArrayOf(0f, 0f, 1f)
        val y = floatArrayOf(0f, 0f, 1f)

        val actual = polyFitLeastSquares(x, y, x.size, 2)

        assertIsCloseToEquals(actual, floatArrayOf(0f, 0f))
    }

    private fun catchThrowable(lambda: () -> Unit): Throwable? {
        var exception: Throwable? = null

        try {
            lambda()
        } catch (theException: Throwable) {
            exception = theException
        }

        return exception
    }

    private fun assertIsCloseToEquals(
        actual: FloatArray,
        expected: FloatArray
    ) {
        assertThat(expected.size).isEqualTo(expected.size)
        expected.forEachIndexed() { index, value ->
            assertThat(actual[index]).isWithin(.00001f).of(value)
        }
    }
}
