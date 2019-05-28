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

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.viewpager2.integration.testapp.MutableCollectionBaseActivity
import androidx.viewpager2.integration.testapp.R
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test

/**
 * Generic test class for testing [MutableCollectionBaseActivity]s. Swipes from the first to the
 * eighth page, increases the count on the eighth page, inserts a new page at the beginning, swipes
 * back to the eighth page, swipes back to the first page and finally to the newly inserted page. On
 * each page, verifies the page number and the expected count. Implementations simply define the
 * Activity under test, see [MutableCollectionViewTest] and [MutableCollectionFragmentTest].
 */
abstract class MutableCollectionBaseTest<T : MutableCollectionBaseActivity>(clazz: Class<T>) :
    BaseTest<T>(clazz) {
    override val layoutId get() = R.id.viewPager

    @Test
    fun testKeepsState() {
        // increase count of page 1 to 1
        verifyPage(1)
        increaseCount()
        verifyCount(1)

        // increase count of page 8 to 3
        repeat(7) { swipeToNextPage() }
        verifyPage(8)
        repeat(3) { increaseCount() }
        verifyCount(3)

        // insert page at the beginning
        choosePage(1)
        insertPageBefore()
        // check that we're now looking at the page before page 8
        verifyPage(7)
        verifyCount(0)
        // swipe back to page 8
        swipeToNextPage()
        verifyPage(8)
        verifyCount(3)

        // swipe back to page 1
        repeat(7) { swipeToPreviousPage() }
        verifyPage(1)
        verifyCount(1)

        // check the newly inserted page
        swipeToPreviousPage()
        verifyPage(10)
        verifyCount(0)
    }

    private fun increaseCount() {
        onView(withId(R.id.buttonCountIncrease)).perform(click())
    }

    private fun verifyCount(count: Int) {
        onView(withId(R.id.textViewCount)).check(matches(withText("$count")))
    }

    private fun verifyPage(page: Int) {
        verifyCurrentPage(hasDescendant(allOf(
            withId(R.id.textViewItemText),
            withText("item#$page")
        )))
    }

    private fun choosePage(page: Int) {
        onView(withId(R.id.itemSpinner)).perform(click())
        onData(equalTo("item#$page")).perform(click())
    }

    private fun insertPageBefore() {
        onView(withId(R.id.buttonAddBefore)).perform(click())
    }
}
