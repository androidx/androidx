/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
internal class GlimmerListAutoFocusScrollConverterTest(
    private val testCase: ScrollConverterTestCase
) {

    init {
        assert(testCase.scrollThreshold >= testCase.viewportSize / 2) {
            """
                The scroll threshold (d) can't be less than half of the list viewport size (h/2).
                Check the correctness of the test cases.
            """
        }
    }

    @Test
    fun arbitraryPoints_converterTest() {
        testCase.assertPoints(testCase.xy)
    }

    @Test
    fun pivotPoints_converterTest() {
        val pivotPoints = testCase.pivotPoints()

        testCase.assertPoints(pivotPoints)
    }

    /** Some points on the graph must be true no matter what the list parameters are. */
    private fun ScrollConverterTestCase.pivotPoints(): List<Pair<Double, Double>> {
        return when {
            contentLength <= viewportSize ->
                listOf(
                    0.0 to 0.0,
                    (contentLength / 2) to (contentLength / 2),
                    contentLength to contentLength,
                )
            contentLength <= scrollThreshold + viewportSize + scrollThreshold ->
                listOf(
                    0.0 to 0.0,
                    (contentLength / 2) to (contentLength / 2 - viewportSize / 2),
                    contentLength to (contentLength - viewportSize),
                )
            else ->
                listOf(
                    0.0 to 0.0,
                    (scrollThreshold + viewportSize / 2) to scrollThreshold,
                    (contentLength - scrollThreshold - viewportSize / 2) to
                        (contentLength - viewportSize - scrollThreshold),
                    contentLength to (contentLength - viewportSize),
                )
        }
    }

    private fun ScrollConverterTestCase.assertPoints(
        points: List<Pair<Double, Double>>,
        tolerance: Double = 0.1,
    ) {
        for ((x, y) in points) {
            val actualY =
                AutoFocusScrollConverter.convertUserScrollToContentScroll(
                    userScroll = x,
                    scrollThreshold = scrollThreshold,
                    viewportSize = viewportSize,
                    contentLength = contentLength,
                )
            val actualX =
                AutoFocusScrollConverter.convertContentScrollToUserScroll(
                    contentScroll = y,
                    scrollThreshold = scrollThreshold,
                    viewportSize = viewportSize,
                    contentLength = contentLength,
                )

            assertWithMessage("Expected 'y(x)=y($x)=$y', but got 'y($x)=$actualY' - $testCase")
                .that(actualY)
                .isWithin(tolerance)
                .of(y)

            assertWithMessage("Expected 'x(y)=x($y)=$x', but got 'x($y)=$actualX' - $testCase")
                .that(actualX)
                .isWithin(tolerance)
                .of(x)
        }
    }

    internal data class ScrollConverterTestCase(
        val scrollThreshold: Double, // d
        val viewportSize: Double, // h
        val contentLength: Double, // L
        val xy: List<Pair<Double, Double>>, // Su->Sc
    ) {
        override fun toString(): String = "(d=$scrollThreshold, h=$viewportSize, L=$contentLength)"
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}-{0}")
        internal fun parameters(): Array<ScrollConverterTestCase> =
            arrayOf(
                // Extra large list, L > 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 300.0,
                    viewportSize = 500.0,
                    contentLength = 500_000_000.0,
                    xy =
                        listOf(
                            1.0 to 0.09,
                            2.0 to 0.18,
                            5.0 to 0.47,
                            10.0 to 0.99,
                            50.0 to 6.62,
                            350.0 to 133.06,
                            499_999_650.0 to 499_999_366.94,
                            499_999_950.0 to 499_999_493.38,
                            499_999_990.0 to 499_999_499.01,
                            499_999_995.0 to 499_999_499.52,
                            499_999_998.0 to 499_999_499.81,
                            499_999_999.0 to 499_999_499.91,
                        ),
                ),
                // Large list, L > 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 100.0,
                    viewportSize = 100.0,
                    contentLength = 500.0,
                    xy =
                        listOf(
                            50.0 to 22.2,
                            120.0 to 72.0,
                            150.0 to 100.0,
                            200.0 to 150.0,
                            300.0 to 250.0,
                            395.0 to 340.5,
                            450.0 to 377.8,
                        ),
                ),
                // Large list, L > 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 200.0,
                    viewportSize = 300.0,
                    contentLength = 1000.0,
                    xy =
                        listOf(
                            100.0 to 26.53,
                            200.0 to 77.55,
                            300.0 to 153.06,
                            400.0 to 250.00,
                            500.0 to 350.00,
                            700.0 to 546.94,
                            800.0 to 622.45,
                            900.0 to 673.47,
                        ),
                ),
                // Short list, L == 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 100.0,
                    viewportSize = 200.0,
                    contentLength = 400.0,
                    xy = listOf(50.0 to 25.0, 150.0 to 75.0, 250.0 to 125.0, 350.0 to 175.0),
                ),
                // Short list, L < 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 150.0,
                    viewportSize = 300.0,
                    contentLength = 500.0,
                    xy = listOf(50.0 to 20.0, 100.0 to 40.0, 250.0 to 100.0, 400.0 to 160.0),
                ),
                // Non-scrollable list, L < h
                ScrollConverterTestCase(
                    scrollThreshold = 150.0,
                    viewportSize = 300.0,
                    contentLength = 200.0,
                    xy = listOf(10.0 to 10.0, 42.0 to 42.0, 125.0 to 125.0, 175.0 to 175.0),
                ),
                // Edge case - empty list, L = 0
                ScrollConverterTestCase(
                    scrollThreshold = 150.0,
                    viewportSize = 300.0,
                    contentLength = 0.0,
                    xy = listOf(0.0 to 0.0),
                ),
            )
    }
}
