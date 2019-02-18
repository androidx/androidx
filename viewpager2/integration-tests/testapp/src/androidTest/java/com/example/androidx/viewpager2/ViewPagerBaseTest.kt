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

package com.example.androidx.viewpager2

import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import com.example.androidx.viewpager2.test.onCurrentPage
import com.example.androidx.viewpager2.test.withRotation
import com.example.androidx.viewpager2.test.withScale
import com.example.androidx.viewpager2.test.withTranslation
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runners.Parameterized

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
        selectOrientation()
        if (config.animateRotate) check(R.id.rotate_checkbox)
        if (config.animateTranslate) check(R.id.translate_checkbox)
        if (config.animateScale) check(R.id.scale_checkbox)
    }

    @Test
    fun testSwipe() {
        // Swipe to page 2
        swipeToNextPage { verifyAnimation() }
        verifyCurrentPage(threeOfSpades)

        // Swipe back to page 1
        swipeToPreviousPage { verifyAnimation() }
        verifyCurrentPage(twoOfSpades)
    }

    private fun selectOrientation() {
        onView(withId(R.id.orientation_spinner)).perform(click())
        onData(equalTo(
            when (config.orientation) {
                ORIENTATION_HORIZONTAL -> "horizontal"
                ORIENTATION_VERTICAL -> "vertical"
                else -> "unknown"
            }
        )).perform(click())
    }

    private fun check(id: Int) {
        onView(allOf(withId(id), isNotChecked())).perform(click())
    }

    private fun verifyAnimation() {
        val animationVerifiers = mutableListOf<Matcher<View>>()
        if (config.animateRotate) animationVerifiers.add(withRotation())
        if (config.animateTranslate) animationVerifiers.add(withTranslation())
        if (config.animateScale) animationVerifiers.add(withScale())
        onCurrentPage().check(matches(allOf(animationVerifiers)))
    }
}
