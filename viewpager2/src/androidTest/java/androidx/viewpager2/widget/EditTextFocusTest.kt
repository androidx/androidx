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

import android.graphics.Color
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.SECONDS

/** Regression test for an issue when focusing on an EditText would cause a scroll to page 0. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EditTextFocusTest : BaseTest() {
    @Test
    fun test_currentPage_clickFocus_editTextIsPage() {
        test_currentPage_clickFocus(false)
    }

    @Test
    fun test_currentPage_clickFocus_editTextIsPageChild() {
        test_currentPage_clickFocus(true)
    }

    /** Verifies we stay on the current page if focus is requested on its element. */
    private fun test_currentPage_clickFocus(wrapEditTextInViewGroup: Boolean) {
        setUpTest(3, wrapEditTextInViewGroup).apply {
            listOf(1, 2, 1, 0, 1).forEach { targetPage ->
                // set a page
                closeSoftKeyboard() // setCurrentItem ignored otherwise; TODO: check if on purpose
                runOnUiThreadSync { viewPager.setCurrentItem(targetPage, false) }
                assertBasicState(targetPage, null)

                // when EditText gets focus
                currentEditText.perform(click())

                // then no page change as a result
                assertBasicState(targetPage, null)
            }
        }
    }

    @Test
    fun test_notCurrentPage_requestFocus_editTextIsPage() {
        test_notCurrentPage_requestFocus(false)
    }

    @Test
    fun test_notCurrentPage_requestFocus_editTextIsPageChild() {
        test_notCurrentPage_requestFocus(true)
    }

    /** Verifies we don't navigate to another page if focus is requested on its element. */
    private fun test_notCurrentPage_requestFocus(wrapEditTextInViewGroup: Boolean) {
        val pageCount = 3
        setUpTest(pageCount, wrapEditTextInViewGroup).apply {
            (0 until pageCount).forEach { targetPage ->
                // given
                viewPager.setCurrentItemSync(targetPage, false, 2, SECONDS)
                assertBasicState(targetPage, null)

                val otherPage = (targetPage + 1) % pageCount
                val editText =
                    editTextForPage(viewPager.linearLayoutManager.findViewByPosition(otherPage)!!)

                // when
                closeSoftKeyboard() // setCurrentItem ignored otherwise; TODO: check if on purpose
                val latch = viewPager.addWaitForFirstScrollEventLatch()
                runOnUiThreadSync { editText.requestFocus() }
                assertFalse(latch.await(1, SECONDS)) // TODO: replace with Robolectric

                // then
                assertBasicState(targetPage, null)
            }
        }
    }

    private fun setUpTest(
        @Suppress("SameParameterValue") pageCount: Int,
        wrapEditTextInViewGroup: Boolean
    ): Context {
        return setUpTest(ORIENTATION_HORIZONTAL).apply {
            runOnUiThreadSync {
                viewPager.adapter = createEditTextAdapter(pageCount, wrapEditTextInViewGroup)
                viewPager.offscreenPageLimit = pageCount
            }
            assertBasicState(0, null)
        }
    }

    private val currentEditText
        get() = onView(allOf(isCompletelyDisplayed(), isAssignableFrom(EditText::class.java)))

    private fun closeSoftKeyboard() {
        currentEditText.perform(ViewActions.closeSoftKeyboard())
    }

    /**
     * Creates an adapter with EditText elements reproducing b/139432498
     * @param wrapEditTextInViewGroup if false, [EditText] is the page root
     */
    private fun createEditTextAdapter(pageCount: Int, wrapEditTextInViewGroup: Boolean):
            RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount(): Int = pageCount

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val editText = EditText(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    gravity = Gravity.CENTER_VERTICAL or Gravity.END
                    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    setBackgroundColor(Color.WHITE)
                    setTextColor(Color.DKGRAY)
                }
                val pageView = pageForEditText(editText, wrapEditTextInViewGroup)
                return object : RecyclerView.ViewHolder(pageView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                editTextForPage(holder.itemView).text =
                    SpannableStringBuilder("position=$position")
            }
        }
    }

    private fun editTextForPage(page: View): EditText = page.let {
        when (it) {
            is EditText -> it
            is ViewGroup -> it.getChildAt(0) as EditText
            else -> throw IllegalArgumentException()
        }
    }

    private fun pageForEditText(editText: EditText, wrapInViewGroup: Boolean): View =
        when (wrapInViewGroup) {
            false -> editText
            true -> FrameLayout(editText.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                addView(editText)
            }
        }
}
