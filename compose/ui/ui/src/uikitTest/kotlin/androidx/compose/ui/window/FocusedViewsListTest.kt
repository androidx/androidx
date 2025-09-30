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

package androidx.compose.ui.window

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.runBlocking
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIView

class FocusedViewsListTest {
    @Test
    fun testAddingViews() {
        val list = FocusedViewsList()
        val view1 = FocusableView()
        val view2 = FocusableView()
        val view3 = FocusableView()

        list.addAndFocus(view1)
        assertTrue(view1.isFirstResponder())

        list.addAndFocus(view2)
        list.addAndFocus(view3)
        assertFalse(view1.isFirstResponder())
        assertFalse(view2.isFirstResponder())
        assertTrue(view3.isFirstResponder())
    }

    @Test
    fun testRemovingFocus() {
        val list = FocusedViewsList()
        val view1 = FocusableView()
        val view2 = FocusableView()
        val view3 = FocusableView()

        list.addAndFocus(view1)
        list.addAndFocus(view2)
        list.addAndFocus(view3)
        assertTrue(view3.isFirstResponder())

        list.remove(view3)
        performRunLoopCycle()
        assertTrue(view2.isFirstResponder())

        list.remove(view1)
        performRunLoopCycle()
        assertTrue(view2.isFirstResponder())

        list.remove(view2)
        performRunLoopCycle()
        assertFalse(view1.isFirstResponder())
        assertFalse(view2.isFirstResponder())
        assertFalse(view3.isFirstResponder())
    }

    @Test
    fun testRemovingFocusWithDelay() {
        val list = FocusedViewsList()
        val view1 = FocusableView()
        val view2 = FocusableView()
        val view3 = FocusableView()

        list.addAndFocus(view1)
        list.addAndFocus(view2)
        list.addAndFocus(view3)
        assertTrue(view3.isFirstResponder())

        list.remove(view3, delayMillis = 50)
        assertTrue(view3.isFirstResponder())

        performRunLoopCycle(delayMills = 200)
        assertTrue(view2.isFirstResponder())
    }

    @Test
    fun testChildFocusedViewsList() {
        val list = FocusedViewsList()
        val view = FocusableView()
        val childView = FocusableView()

        list.addAndFocus(view)
        val childList = list.childFocusedViewsList()
        performRunLoopCycle()
        assertTrue(view.isFirstResponder())

        childList.addAndFocus(childView)
        performRunLoopCycle()
        assertTrue(childView.isFirstResponder())

        childList.remove(childView)
        performRunLoopCycle()
        assertTrue(view.isFirstResponder())
    }

    @Test
    fun testChildFocusedViewsListDisposal() {
        val view1 = FocusableView()
        val list = FocusedViewsList()
        list.addAndFocus(view1)
        val childList = list.childFocusedViewsList()

        val view2 = FocusableView()
        childList.addAndFocus(view2)
        performRunLoopCycle()
        assertTrue(view2.isFirstResponder())

        childList.disposeChild()
        performRunLoopCycle()
        assertTrue(view1.isFirstResponder())
    }

    @Test
    fun testChildFocusedViewsListParentAddAndFocus() {
        val view1 = FocusableView()
        val childView1 = FocusableView()
        val list = FocusedViewsList()
        list.addAndFocus(view1)
        val childList = list.childFocusedViewsList()
        childList.addAndFocus(childView1)

        val view2 = FocusableView()
        list.addAndFocus(view2)
        performRunLoopCycle()
        assertTrue(childView1.isFirstResponder())
    }

    @Test
    fun testFocusWithMultipleChildren() {
        val view = FocusableView()
        val childView1 = FocusableView()
        val childView2 = FocusableView()

        val list = FocusedViewsList()
        val childList1 = list.childFocusedViewsList()
        val childList2 = list.childFocusedViewsList()

        list.addAndFocus(view)
        childList1.addAndFocus(childView1)
        childList2.addAndFocus(childView2)

        performRunLoopCycle()
        assertTrue(childView2.isFirstResponder())

        childList1.addAndFocus(FocusableView())
        performRunLoopCycle()
        assertTrue(childView2.isFirstResponder())

        childList1.disposeChild()
        performRunLoopCycle()
        assertTrue(childView2.isFirstResponder())

        childList2.disposeChild()
        performRunLoopCycle()
        assertTrue(view.isFirstResponder())
    }

    @Test
    fun testChildSubtree() {
        val view = FocusableView()
        val grandChild1View = FocusableView()
        val grandChild2View = FocusableView()

        val list = FocusedViewsList()
        val childList1 = list.childFocusedViewsList()
        val grandChildList1 = childList1.childFocusedViewsList()
        val childList2 = list.childFocusedViewsList()
        val grandChildList2 = childList2.childFocusedViewsList()
        grandChildList1.addAndFocus(grandChild1View)
        grandChildList2.addAndFocus(grandChild2View)
        list.addAndFocus(view)
        childList1.addAndFocus(FocusableView())
        childList2.addAndFocus(FocusableView())

        performRunLoopCycle()
        assertTrue(grandChild2View.isFirstResponder())

        childList2.disposeChild()
        performRunLoopCycle()
        assertTrue(grandChild1View.isFirstResponder())

        childList1.disposeChild()
        performRunLoopCycle()
        assertTrue(view.isFirstResponder())
    }

    private fun performRunLoopCycle(delayMills: Long = 10) {
        NSRunLoop.currentRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(delayMills.toDouble() / 1000.0))
    }

    var focusedView: UIView? = null

    @OptIn(ExperimentalForeignApi::class)
    private inner class FocusableView: UIView(frame = CGRectZero.readValue()) {
        override fun isFirstResponder(): Boolean {
            return focusedView == this
        }

        override fun becomeFirstResponder(): Boolean {
            focusedView = this
            return true
        }

        override fun resignFirstResponder(): Boolean {
            if (focusedView == this) {
                focusedView = null
            }
            return true
        }
    }
}