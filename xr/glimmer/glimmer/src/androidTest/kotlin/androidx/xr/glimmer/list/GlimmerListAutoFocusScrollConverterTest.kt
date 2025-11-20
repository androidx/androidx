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
    private fun ScrollConverterTestCase.pivotPoints(): List<Pair<Float, Float>> {
        return when {
            contentLength <= viewportSize ->
                listOf(
                    0f to 0f,
                    (contentLength / 2) to (contentLength / 2),
                    contentLength to contentLength,
                )
            contentLength <= scrollThreshold + viewportSize + scrollThreshold ->
                listOf(
                    0f to 0f,
                    (contentLength / 2) to (contentLength / 2 - viewportSize / 2),
                    contentLength to (contentLength - viewportSize),
                )
            else ->
                listOf(
                    0f to 0f,
                    (scrollThreshold + viewportSize / 2) to scrollThreshold,
                    (contentLength - scrollThreshold - viewportSize / 2) to
                        (contentLength - viewportSize - scrollThreshold),
                    contentLength to (contentLength - viewportSize),
                )
        }
    }

    private fun ScrollConverterTestCase.assertPoints(
        points: List<Pair<Float, Float>>,
        tolerance: Float = 0.1f,
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
        val scrollThreshold: Float, // d
        val viewportSize: Float, // h
        val contentLength: Float, // L
        val xy: List<Pair<Float, Float>>, // Su->Sc
    ) {
        override fun toString(): String = "(d=$scrollThreshold, h=$viewportSize, L=$contentLength)"
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}-{0}")
        internal fun parameters(): Array<ScrollConverterTestCase> =
            arrayOf(
                // Large list, L > 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 100f,
                    viewportSize = 100f,
                    contentLength = 500f,
                    xy =
                        listOf(
                            50f to 22.2f,
                            120f to 72f,
                            200f to 150f,
                            300f to 250f,
                            395f to 340.5f,
                            450f to 377.8f,
                        ),
                ),
                // Large list, L > 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 200f,
                    viewportSize = 300f,
                    contentLength = 1000f,
                    xy =
                        listOf(
                            100f to 26.53f,
                            200f to 77.55f,
                            300f to 153.06f,
                            400f to 250.00f,
                            500f to 350.00f,
                            700f to 546.94f,
                            800f to 622.45f,
                            900f to 673.47f,
                        ),
                ),
                // Short list, L == 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 100f,
                    viewportSize = 200f,
                    contentLength = 400f,
                    xy = listOf(50f to 25f, 150f to 75f, 250f to 125f, 350f to 175f),
                ),
                // Short list, L < 2d + h
                ScrollConverterTestCase(
                    scrollThreshold = 150f,
                    viewportSize = 300f,
                    contentLength = 500f,
                    xy = listOf(50f to 20f, 100f to 40f, 250f to 100f, 400f to 160f),
                ),
                // Non-scrollable list, L < h
                ScrollConverterTestCase(
                    scrollThreshold = 150f,
                    viewportSize = 300f,
                    contentLength = 200f,
                    xy = listOf(10f to 10f, 42f to 42f, 125f to 125f, 175f to 175f),
                ),
                // Edge case - empty list, L = 0
                ScrollConverterTestCase(
                    scrollThreshold = 150f,
                    viewportSize = 300f,
                    contentLength = 0f,
                    xy = listOf(0f to 0f),
                ),
            )
    }
}
