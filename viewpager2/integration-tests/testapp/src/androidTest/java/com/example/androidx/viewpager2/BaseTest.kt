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
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.viewpager2.widget.ViewPager2
import com.example.androidx.viewpager2.test.ViewPagerIdleWatcher
import com.example.androidx.viewpager2.test.onCurrentPage
import com.example.androidx.viewpager2.test.onViewPager
import com.example.androidx.viewpager2.test.swipeNext
import com.example.androidx.viewpager2.test.swipePrevious
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base class for all tests. Contains common functionality, like finding the [ViewPager2] under
 * test, swiping back and forth on it and waiting for it to become idle.
 *
 * @see ViewPagerBaseTest
 * @see MutableCollectionBaseTest
 * @see TabLayoutTest
 */
abstract class BaseTest<T : FragmentActivity>(clazz: Class<T>) {
    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(clazz)

    @get:LayoutRes
    abstract val layoutId: Int

    lateinit var idleWatcher: ViewPagerIdleWatcher
    lateinit var viewPager: ViewPager2

    @Before
    open fun setUp() {
        viewPager = activityTestRule.activity.findViewById(layoutId)
        idleWatcher = ViewPagerIdleWatcher(viewPager)
    }

    @After
    open fun tearDown() {
        idleWatcher.unregister()
    }

    fun swipeToNextPage() {
        onViewPager().perform(swipeNext())
        idleWatcher.waitForIdle()
        onIdle()
    }

    fun swipeToPreviousPage() {
        onViewPager().perform(swipePrevious())
        idleWatcher.waitForIdle()
        onIdle()
    }

    fun verifyCurrentPage(pageText: String) {
        verifyCurrentPage(hasDescendant(withText(pageText)))
    }

    fun verifyCurrentPage(matcher: Matcher<View>) {
        onCurrentPage().check(matches(matcher))
    }
}
