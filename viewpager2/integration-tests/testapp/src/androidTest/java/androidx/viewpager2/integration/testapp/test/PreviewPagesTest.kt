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

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.integration.testapp.PreviewPagesActivity
import androidx.viewpager2.integration.testapp.R
import androidx.viewpager2.integration.testapp.test.util.swipeNext
import androidx.viewpager2.widget.ViewPager2
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PreviewPagesTest : BaseTest<PreviewPagesActivity>(PreviewPagesActivity::class.java) {

    override val layoutId = R.id.view_pager

    @Ignore("b/276935528")
    @Test
    fun test() {
        verifyCurrentPage(0)
        verifyPageVisible(1)

        swipeToNextPageFrom(0)
        verifyPageVisible(0)
        verifyCurrentPage(1)
        verifyPageVisible(2)
    }

    @Suppress("SameParameterValue")
    private fun swipeToNextPageFrom(position: Int) {
        onPage(position).perform(swipeNext())
        idleWatcher.waitForIdle()
        Espresso.onIdle()
    }

    private fun verifyCurrentPage(position: Int) {
        onPage(position).check(matches(isCompletelyDisplayed()))
    }

    private fun verifyPageVisible(position: Int) {
        onPage(position).check(matches(isDisplayed()))
    }

    private fun onPage(position: Int): ViewInteraction {
        return onView(
            allOf(
                withParent(withParent(isAssignableFrom(ViewPager2::class.java))),
                withTagValue(equalTo(position))
            )
        )
    }
}
