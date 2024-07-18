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

package androidx.viewpager2.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.LocaleTestUtils
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import java.util.concurrent.TimeUnit.SECONDS
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests [ViewPager2.canScrollHorizontally] and [ViewPager2.canScrollVertically]
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CanScrollTest : BaseTest() {
    @Test
    fun test_canScrollHorizontallyVertically_horizontal_ltr() {
        // given
        setUpTest(ORIENTATION_HORIZONTAL).apply {
            // when no pages
            setAdapterSync(viewAdapterProvider.provider(stringSequence(0)))

            // then can't scroll
            assertScrollNone()

            // when 1 page
            setAdapterSync(viewAdapterProvider.provider(stringSequence(1)))

            // then can't scroll
            assertScrollNone()

            // when 2 pages
            setAdapterSync(viewAdapterProvider.provider(stringSequence(2)))

            // then can scroll right
            assertScrollRight()

            // when peeking next page
            runOnUiThreadSync {
                viewPager.beginFakeDrag()
                viewPager.fakeDragBy(-2f)
            }

            // then can scroll left and right
            assertScrollLeftRight()

            // when stop peeking and go to last page
            runOnUiThreadSync {
                viewPager.endFakeDrag()
            }
            viewPager.setCurrentItemSync(1, false, 2, SECONDS)

            // then can scroll left
            assertScrollLeft()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 17)
    fun test_canScrollHorizontallyVertically_horizontal_rtl() {
        // given RTL locale
        localeUtil.resetLocale()
        localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        setUpTest(ORIENTATION_HORIZONTAL).apply {
            // when no pages
            setAdapterSync(viewAdapterProvider.provider(stringSequence(0)))

            // then can't scroll
            assertScrollNone()

            // when 1 page
            setAdapterSync(viewAdapterProvider.provider(stringSequence(1)))

            // then can't scroll
            assertScrollNone()

            // when 2 pages
            setAdapterSync(viewAdapterProvider.provider(stringSequence(2)))

            // then can scroll left
            assertScrollLeft()

            // when peeking next page
            runOnUiThreadSync {
                viewPager.beginFakeDrag()
                viewPager.fakeDragBy(2f)
            }

            // then can scroll left and right
            assertScrollLeftRight()

            // when stop peeking and go to last page
            runOnUiThreadSync {
                viewPager.endFakeDrag()
            }
            viewPager.setCurrentItemSync(1, false, 2, SECONDS)

            // then can scroll right
            assertScrollRight()
        }
    }

    @Test
    fun test_canScrollHorizontallyVertically_vertical() {
        // given
        setUpTest(ORIENTATION_VERTICAL).apply {
            // when no pages
            setAdapterSync(viewAdapterProvider.provider(stringSequence(0)))

            // then can't scroll
            assertScrollNone()

            // when 1 page
            setAdapterSync(viewAdapterProvider.provider(stringSequence(1)))

            // then can't scroll
            assertScrollNone()

            // when 2 pages
            setAdapterSync(viewAdapterProvider.provider(stringSequence(2)))

            // then can scroll down
            assertScrollDown()

            // when peeking next page
            runOnUiThreadSync {
                viewPager.beginFakeDrag()
                viewPager.fakeDragBy(-2f)
            }

            // then can scroll up and down
            assertScrollUpDown()

            // when stop peeking and go to last page
            runOnUiThreadSync {
                viewPager.endFakeDrag()
            }
            viewPager.setCurrentItemSync(1, false, 2, SECONDS)

            // then can scroll up
            assertScrollUp()
        }
    }

    private fun Context.assertScrollNone() = verifyCanScroll(false, false, false, false)
    private fun Context.assertScrollRight() = verifyCanScroll(true, false, false, false)
    private fun Context.assertScrollLeft() = verifyCanScroll(false, true, false, false)
    private fun Context.assertScrollLeftRight() = verifyCanScroll(true, true, false, false)
    private fun Context.assertScrollDown() = verifyCanScroll(false, false, true, false)
    private fun Context.assertScrollUp() = verifyCanScroll(false, false, false, true)
    private fun Context.assertScrollUpDown() = verifyCanScroll(false, false, true, true)

    private fun Context.verifyCanScroll(
        expectScrollRight: Boolean,
        expectScrollLeft: Boolean,
        expectScrollDown: Boolean,
        expectScrollUp: Boolean
    ) {
        assertThat(viewPager.canScrollHorizontally(1), equalTo(expectScrollRight))
        assertThat(viewPager.canScrollHorizontally(-1), equalTo(expectScrollLeft))
        assertThat(viewPager.canScrollVertically(1), equalTo(expectScrollDown))
        assertThat(viewPager.canScrollVertically(-1), equalTo(expectScrollUp))
    }
}
