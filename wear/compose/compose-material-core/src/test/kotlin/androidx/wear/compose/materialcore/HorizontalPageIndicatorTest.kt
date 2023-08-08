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

package androidx.wear.compose.materialcore

import androidx.compose.ui.util.lerp
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Pager might be represented as following ASCII Art:
 * *LS* o O O X O o _ RS
 * where:
 * LS - left spacer
 * o - small dot
 * O - big dot
 * _ - invisible dot
 * RS - right spacer
 * ** - size of the spacer
 */
@RunWith(JUnit4::class)
class HorizontalPageIndicatorTest {
    private val one: Pair<Float, Float> = 1f to 1f
    private val zero: Pair<Float, Float> = 0f to 0f

    @Test
    fun all_dots_full_size_when_total_less_than_pages_on_screen_selected_first() {
        val pagesState = PagesState(totalPages = 4, pagesOnScreen = 6)

        pagesState.recalculateState(selectedPage = 0, offset = 0f)
        pagesState.testAllDots(
            listOf(
                TestDot(selectedRatio = 1f),
                TestDot(),
                TestDot(),
                TestDot()
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )
    }

    @Test
    fun all_dots_full_size_when_total_less_than_pages_on_screen_selected_middle() {
        val pagesState = PagesState(totalPages = 4, pagesOnScreen = 6)

        pagesState.recalculateState(selectedPage = 2, offset = 0f)
        pagesState.testAllDots(
            listOf(
                TestDot(),
                TestDot(),
                TestDot(selectedRatio = 1f),
                TestDot()
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )
    }

    @Test
    fun all_dots_full_size_when_total_less_than_pages_on_screen_selected_last() {
        val pagesState = PagesState(totalPages = 4, pagesOnScreen = 6)

        pagesState.recalculateState(selectedPage = 3, offset = 0f)
        pagesState.testAllDots(
            listOf(
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(selectedRatio = 1f)
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )
    }

    @Test
    fun last_dot_half_size_when_total_more_than_pages_on_screen_selected_first() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)

        pagesState.recalculateState(selectedPage = 0, offset = 0f)

        pagesState.apply {
            testDot(0, TestDot(selectedRatio = 1f), 0f)
            testDot(pagesOnScreen - 1, TestDot(sizeRatio = 0.5f), 0f)
        }
    }

    @Test
    fun first_dot_half_size_when_total_more_than_pages_on_screen_selected_last() {
        val pagesCount = 12
        val pagesState = PagesState(pagesCount, pagesOnScreen = 6)

        pagesState.recalculateState(selectedPage = pagesCount - 1, offset = 0f)
        pagesState.apply {
            testDot(0, TestDot(sizeRatio = 0.5f), 0f)
            testDot(pagesOnScreen - 1, TestDot(selectedRatio = 1f), 0f)
        }
    }

    @Test
    fun first_and_last_dots_half_size_when_total_more_than_pages_on_screen_selected_mid() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)

        pagesState.recalculateState(selectedPage = 6, offset = 0f)
        pagesState.apply {
            testDot(0, TestDot(sizeRatio = 0.5f), 0f)
            testDot(5, TestDot(sizeRatio = 0.5f), 0f)
        }
    }

    @Test
    fun selected_ratio_changes_when_first_to_second_dot_shift() {
        val pagesState = PagesState(totalPages = 4, pagesOnScreen = 6)
        listOf(0.3f, 0.6f, 0.9f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 0, offset = offset)
            pagesState.apply {
                testDot(0, TestDot(one, one, 1f to 0f), offset)
                testDot(1, TestDot(one, one, 0f to 1f), offset)
                testDot(2, TestDot(), 0f)
            }
        }
    }

    /**
     * totalPages: 12 , selected page: 4, shifting right
     * Visually :
     *     *LS* O O O O X o _ RS
     * >>> LS _ o O O O X o *RS*
     */
    @Test
    fun shift_right_when_last_on_screen_dot_shifted_right_first_shift() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)
        listOf(0.3f, 0.6f, 0.9f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 4, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f to 0f, 1f to 0f, zero),
                    TestDot(one, 1f to 0.5f, zero),
                    TestDot(),
                    TestDot(),
                    TestDot(one, one, 1f to 0f),
                    TestDot(one, 0.5f to 1f, 0f to 1f),
                    TestDot(0f to 1f, 0f to 0.5f, zero),
                ),
                offset = offset,
                leftSpacerSize = 1 - offset,
                rightSpacerSize = offset
            )
        }
    }

    /**
     * totalPages: 12 , selected page: 8, shifting right
     * Visually :
     *     *LS* o O O O X o _ RS
     * >>> LS _ o O O O X o *RS*
     */
    @Test
    fun shift_right_when_last_on_screen_dot_shifted_right_mid_shift() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)

        listOf(0.3f, 0.6f, 0.9f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 8, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f to 0f, 0.5f to 0f, zero),
                    TestDot(one, 1f to 0.5f, zero),
                    TestDot(),
                    TestDot(),
                    TestDot(one, one, 1f to 0f),
                    TestDot(one, 0.5f to 1f, 0f to 1f),
                    TestDot(0f to 1f, 0f to 0.5f, zero),
                ),
                offset = offset,
                leftSpacerSize = 1 - offset,
                rightSpacerSize = offset
            )
        }
    }

    /**
     * totalPages: 12 , selected page: 9, shifting right
     * Visually :
     *     *LS* o O O O X o _ RS
     * >>> LS _ o O O O X O *RS*
     */
    @Test
    fun shift_right_when_last_on_screen_dot_shifted_right_last_shift() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)

        listOf(0.3f, 0.6f, 0.9f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 9, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f to 0f, 0.5f to 0f, zero),
                    TestDot(one, 1f to 0.5f, zero),
                    TestDot(),
                    TestDot(),
                    TestDot(one, one, 1f to 0f),
                    TestDot(one, 0.5f to 1.0f, 0f to 1f),
                    TestDot(0f to 1f, 0f to 1f, zero),
                ),
                offset = offset,
                leftSpacerSize = 1 - offset,
                rightSpacerSize = offset
            )
        }
    }

    /**
     * totalPages: 12 , selected page: 10, shifting right
     * Visually :
     *     *LS* o O O O X O _ RS
     * >>> *LS* o O O O O X _ RS
     */
    @Test
    fun dont_shift_right_when_last_but_one_dot_shifted_right_to_last_page() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)

        listOf(0.3f, 0.6f, 0.9f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 10, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f, 0.5f, 0f),
                    TestDot(),
                    TestDot(),
                    TestDot(),
                    TestDot(1f, 1f, 1 - offset),
                    TestDot(1f, 1f, offset),
                    TestDot(0f, 0f, 0f),
                ),
                offset = offset,
                leftSpacerSize = 1f,
                rightSpacerSize = 0f
            )
        }
    }

    /**
     * totalPages: 12 , selected page: 7, shifting left
     * Visually :
     *     *LS* o X O O O O _ RS
     * >>> LS _ x O O O O O *RS*
     * >>> *LS* o X O O O o _ RS
     */
    @Test
    fun shift_left_when_second_on_screen_dot_shifted_left_last_pages() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)
        // Shifting to the right-most
        pagesState.recalculateState(selectedPage = 11, offset = 0f)
        // Going to the left-most position before shift
        pagesState.recalculateState(selectedPage = 7, offset = 0f)

        // Test before shift
        pagesState.testAllDots(
            listOf(
                TestDot(1f, 0.5f, 0f),
                TestDot(1f, 1f, 1f),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(0f, 0f, 0f),
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )

        // Test during shift
        listOf(0.9f, 0.6f, 0.3f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 6, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f to 0f, 0.5f to 0f, zero),
                    TestDot(one, 1f to 0.5f, 1f to 0f),
                    TestDot(one, one, 0f to 1f),
                    TestDot(),
                    TestDot(),
                    TestDot(one, 0.5f to 1f, zero),
                    TestDot(0f to 1f, 0f to 1f, zero),
                ),
                offset = offset,
                leftSpacerSize = 1 - offset,
                rightSpacerSize = offset
            )
        }

        // Test after shift
        pagesState.recalculateState(selectedPage = 6, offset = 0f)

        pagesState.testAllDots(
            listOf(
                TestDot(1f, 0.5f, 0f),
                TestDot(1f, 1f, 1f),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(1f, 0.5f, 0f),
                TestDot(0f, 0f, 0f),
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )
    }

    /**
     * totalPages: 12 , selected page: 4, shifting left
     * Visually :
     *     *LS* o X O O O o _ RS
     * >>> LS _ x O O O O o *RS*
     * >>> *LS* o X O O O o _ RS
     */
    @Test
    fun shift_left_when_second_on_screen_dot_shifted_left_mid_pages() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)
        // Shifting to the right-most
        pagesState.recalculateState(selectedPage = 11, offset = 0f)
        // Going to the mid position so that selected item is on the left
        pagesState.recalculateState(selectedPage = 4, offset = 0f)

        // Test before shift
        pagesState.testAllDots(
            listOf(
                TestDot(1f, 0.5f, 0f),
                TestDot(1f, 1f, 1f),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(1f, 0.5f, 0f),
                TestDot(0f, 0f, 0f),
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )

        // Test during shift
        listOf(0.9f, 0.6f, 0.3f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 3, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f to 0f, 0.5f to 0f, zero),
                    TestDot(one, 1f to 0.5f, 1f to 0f),
                    TestDot(one, one, 0f to 1f),
                    TestDot(),
                    TestDot(),
                    TestDot(one, 0.5f to 1f, zero),
                    TestDot(0f to 1f, 0f to 0.5f, zero),
                ),
                offset = offset,
                leftSpacerSize = 1 - offset,
                rightSpacerSize = offset
            )
        }

        // Test after shift
        pagesState.recalculateState(selectedPage = 3, offset = 0f)

        pagesState.testAllDots(
            listOf(
                TestDot(1f, 0.5f, 0f),
                TestDot(1f, 1f, 1f),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(1f, 0.5f, 0f),
                TestDot(0f, 0f, 0f),
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )
    }

    /**
     * totalPages: 12 , selected page: 2, shifting left
     * Visually :
     *     *LS* o X O O O o _ RS
     * >>> LS _ x O O O O o *RS*
     * >>> *LS* O X O O O o _ RS
     */
    @Test
    fun shift_left_when_second_on_screen_dot_shifted_left_start_pages() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)
        // Shifting to the right-most
        pagesState.recalculateState(selectedPage = 11, offset = 0f)
        // Going to the mid position so that selected item is on the left
        pagesState.recalculateState(selectedPage = 2, offset = 0f)

        // Test before shift
        pagesState.testAllDots(
            listOf(
                TestDot(1f, 0.5f, 0f),
                TestDot(1f, 1f, 1f),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(1f, 0.5f, 0f),
                TestDot(0f, 0f, 0f),
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )

        // Test during shift
        listOf(0.9f, 0.6f, 0.3f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 1, offset = offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f to 0f, 1f to 0f, zero),
                    TestDot(one, 1f to 0.5f, 1f to 0f),
                    TestDot(one, one, 0f to 1f),
                    TestDot(),
                    TestDot(),
                    TestDot(one, 0.5f to 1f, zero),
                    TestDot(0f to 1f, 0f to 0.5f, zero),
                ),
                offset = offset,
                leftSpacerSize = 1 - offset,
                rightSpacerSize = offset
            )
        }

        // Test after shift
        pagesState.recalculateState(selectedPage = 1, offset = 0f)

        pagesState.testAllDots(
            listOf(
                TestDot(),
                TestDot(1f, 1f, 1f),
                TestDot(),
                TestDot(),
                TestDot(),
                TestDot(1f, 0.5f, 0f),
                TestDot(0f, 0f, 0f),
            ),
            offset = 0f,
            leftSpacerSize = 1f,
            rightSpacerSize = 0f
        )
    }

    /**
     * Pages count: 12 , selected page: 10, shifting right
     * Visually :
     *     *LS* o O O O X O _ RS
     * >>> *LS* o O O O O X _ RS
     */
    @Test
    fun dont_shift_left_when_first_dot_shifted_left_to_zero_page() {
        val pagesState = PagesState(totalPages = 12, pagesOnScreen = 6)
        // Shifting to the right-most
        pagesState.recalculateState(selectedPage = 11, offset = 0f)
        // Going to the first position so that selected item is on the left
        pagesState.recalculateState(selectedPage = 1, offset = 0f)

        listOf(0.9f, 0.6f, 0.3f).forEach { offset ->
            pagesState.recalculateState(selectedPage = 0, offset)

            pagesState.testAllDots(
                listOf(
                    TestDot(1f, 1f, 1 - offset),
                    TestDot(1f, 1f, offset),
                    TestDot(),
                    TestDot(),
                    TestDot(),
                    TestDot(1f, 0.5f, 0f),
                    TestDot(0f, 0f, 0f),
                ),
                offset = offset,
                leftSpacerSize = 1f,
                rightSpacerSize = 0f
            )
        }
    }

    private fun PagesState.testAllDots(
        testDots: List<TestDot>,
        offset: Float,
        leftSpacerSize: Float,
        rightSpacerSize: Float
    ) {

        testDots.forEachIndexed { index, testDot ->
            testDot(index, testDot, offset)
        }
        Assert.assertEquals("Left spacer:", leftSpacerSize, leftSpacerSizeRatio)
        Assert.assertEquals("Right spacer", rightSpacerSize, rightSpacerSizeRatio)
    }

    private fun PagesState.testDot(
        index: Int,
        testDot: TestDot,
        offset: Float
    ) {

        Assert.assertEquals("Page $index, alpha:", testDot.alpha lerp offset, alpha(index))
        Assert.assertEquals(
            "Page $index, size ratio:",
            testDot.sizeRatio lerp offset,
            sizeRatio(index)
        )
        Assert.assertEquals(
            "Page $index, select ratio:",
            testDot.selectedRatio lerp offset,
            calculateSelectedRatio(index, offset),
            0.002f
        )
    }

    private class TestDot(
        val alpha: Pair<Float, Float>,
        val sizeRatio: Pair<Float, Float>,
        val selectedRatio: Pair<Float, Float>
    ) {
        constructor(
            alpha: Float = 1f,
            sizeRatio: Float = 1f,
            selectedRatio: Float = 0f
        ) : this(
            alpha = alpha to alpha,
            sizeRatio = sizeRatio to sizeRatio,
            selectedRatio = selectedRatio to selectedRatio
        )
    }
}

private infix fun Pair<Float, Float>.lerp(fraction: Float): Float =
    lerp(first, second, fraction)
