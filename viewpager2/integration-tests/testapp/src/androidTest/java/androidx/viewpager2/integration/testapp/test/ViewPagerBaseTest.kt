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

package androidx.viewpager2.integration.testapp.test

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.viewpager2.integration.testapp.BaseCardActivity
import androidx.viewpager2.integration.testapp.R
import androidx.viewpager2.integration.testapp.test.util.AnimationVerifier
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runners.Parameterized

/**
 * Generic test class for testing [BaseCardActivity]s. For each combination of orientation, rotation
 * animation, translation animation and scale animation, swipes from the first to the second page
 * and verifies if the expected pages are shown and if the expected animation was performed.
 * Implementations simply define the Activity under test, see [ViewPagerViewTest] and
 * [ViewPagerFragmentTest].
 */
abstract class ViewPagerBaseTest<T : BaseCardActivity>(
    clazz: Class<T>,
    private val config: TestConfig
) : BaseTest<T>(clazz) {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val animateRotate: Boolean,
        val animateTranslate: Boolean,
        val animateScale: Boolean
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> {
            return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
                listOf(false, true).flatMap { rotate ->
                    listOf(false, true).flatMap { translate ->
                        listOf(false, true).map { scale ->
                            TestConfig(orientation, rotate, translate, scale)
                        }
                    }
                }
            }
        }
    }

    override val layoutId get() = R.id.view_pager

    private val twoOfSpades = "2\n♣"
    private val threeOfSpades = "3\n♣"

    @Before
    override fun setUp() {
        super.setUp()
        selectOrientation(config.orientation)
        if (config.animateRotate) check(R.id.rotate_checkbox)
        if (config.animateTranslate) check(R.id.translate_checkbox)
        if (config.animateScale) check(R.id.scale_checkbox)
    }

    @Test
    fun testSwipe() {
        val animationVerifier = AnimationVerifier(viewPager)

        // Swipe to page 2
        animationVerifier.reset()
        swipeToNextPage()
        animationVerifier.verify()
        verifyCurrentPage(threeOfSpades)

        // Swipe back to page 1
        animationVerifier.reset()
        swipeToPreviousPage()
        animationVerifier.verify()
        verifyCurrentPage(twoOfSpades)
    }

    private fun check(id: Int) {
        onView(allOf(withId(id), isNotChecked())).perform(click())
    }

    private fun AnimationVerifier.verify() {
        awaitAnimation()
        verify(
            expectRotation = config.animateRotate,
            expectTranslation = config.animateTranslate,
            expectScale = config.animateScale
        )
    }
}
