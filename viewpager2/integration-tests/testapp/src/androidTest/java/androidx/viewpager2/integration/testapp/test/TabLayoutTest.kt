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

import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.integration.testapp.CardViewTabLayoutActivity
import androidx.viewpager2.integration.testapp.R
import androidx.viewpager2.integration.testapp.test.util.onTab
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TabLayoutTest : BaseTest<CardViewTabLayoutActivity>(CardViewTabLayoutActivity::class.java) {
    private val nineOfHeartsTab = "9 ♥"
    private val tenOfHeartsTab = "10 ♥"
    private val nineOfHeartsPage = "9\n♥"
    private val tenOfHeartsPage = "10\n♥"

    override val layoutId get() = R.id.view_pager

    @Test
    fun testTabLayoutIntegration() {
        // test if ViewPager2 follows TabLayout when clicking a tab
        selectTab(tenOfHeartsTab)
        verifySelectedTab(tenOfHeartsTab)
        verifyCurrentPage(tenOfHeartsPage)

        // test if TabLayout follows ViewPager2 when swiping to a page
        swipeToPreviousPage()
        verifySelectedTab(nineOfHeartsTab)
        verifyCurrentPage(nineOfHeartsPage)
    }

    private fun selectTab(text: String) {
        onTab(text).perform(scrollTo(), click())
        idleWatcher.waitForIdle()
        onIdle()
    }

    private fun verifySelectedTab(text: String) {
        onTab(text).check(matches(isSelected()))
    }
}
