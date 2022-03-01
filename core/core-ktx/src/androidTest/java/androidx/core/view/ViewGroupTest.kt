/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.view

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class ViewGroupTest {
    private val context = ApplicationProvider.getApplicationContext() as android.content.Context
    private val viewGroup = LinearLayout(context)

    @Test fun get() {
        val view1 = View(context)
        viewGroup.addView(view1)
        val view2 = View(context)
        viewGroup.addView(view2)

        assertSame(view1, viewGroup[0])
        assertSame(view2, viewGroup[1])

        assertThrows<IndexOutOfBoundsException> {
            viewGroup[-1]
        }.hasMessageThat().isEqualTo("Index: -1, Size: 2")

        assertThrows<IndexOutOfBoundsException> {
            viewGroup[2]
        }.hasMessageThat().isEqualTo("Index: 2, Size: 2")
    }

    @Test fun contains() {
        val view1 = View(context)
        viewGroup.addView(view1)
        assertTrue(view1 in viewGroup)
        assertFalse(view1 !in viewGroup)

        val view2 = View(context)
        assertFalse(view2 in viewGroup)
        assertTrue(view2 !in viewGroup)
    }

    @Test fun plusAssign() {
        assertEquals(0, viewGroup.childCount)

        val view1 = View(context)
        viewGroup += view1
        assertEquals(1, viewGroup.childCount)
        assertSame(view1, viewGroup.getChildAt(0))

        val view2 = View(context)
        viewGroup += view2
        assertEquals(2, viewGroup.childCount)
        assertSame(view2, viewGroup.getChildAt(1))
    }

    @Test fun minusAssign() {
        val view1 = View(context)
        viewGroup.addView(view1)
        val view2 = View(context)
        viewGroup.addView(view2)

        assertEquals(2, viewGroup.childCount)

        viewGroup -= view2
        assertEquals(1, viewGroup.childCount)
        assertSame(view1, viewGroup.getChildAt(0))

        viewGroup -= view1
        assertEquals(0, viewGroup.childCount)
    }

    @Test fun size() {
        assertEquals(0, viewGroup.size)

        viewGroup.addView(View(context))
        assertEquals(1, viewGroup.size)

        viewGroup.addView(View(context))
        assertEquals(2, viewGroup.size)

        viewGroup.removeViewAt(0)
        assertEquals(1, viewGroup.size)
    }

    @Test fun isEmpty() {
        assertTrue(viewGroup.isEmpty())
        viewGroup.addView(View(context))
        assertFalse(viewGroup.isEmpty())
    }

    @Test fun isNotEmpty() {
        assertFalse(viewGroup.isNotEmpty())
        viewGroup.addView(View(context))
        assertTrue(viewGroup.isNotEmpty())
    }

    @Test fun forEach() {
        viewGroup.forEach {
            fail("Empty view group should not invoke lambda")
        }

        val view1 = View(context)
        viewGroup.addView(view1)
        val view2 = View(context)
        viewGroup.addView(view2)

        val views = mutableListOf<View>()
        viewGroup.forEach {
            views += it
        }
        assertThat(views).containsExactly(view1, view2)
    }

    @Test fun forEachIndexed() {
        viewGroup.forEachIndexed { _, _ ->
            fail("Empty view group should not invoke lambda")
        }

        val view1 = View(context)
        viewGroup.addView(view1)
        val view2 = View(context)
        viewGroup.addView(view2)

        val views = mutableListOf<View>()
        viewGroup.forEachIndexed { index, view ->
            assertEquals(index, views.size)
            views += view
        }
        assertThat(views).containsExactly(view1, view2)
    }

    @Test fun indices() {
        assertEquals(0 until 0, viewGroup.indices)

        viewGroup.addView(View(context))
        assertEquals(0 until 1, viewGroup.indices)

        viewGroup.addView(View(context))
        assertEquals(0 until 2, viewGroup.indices)
    }

    @Test fun iterator() {
        val view1 = View(context)
        viewGroup.addView(view1)
        val view2 = View(context)
        viewGroup.addView(view2)

        val iterator = viewGroup.iterator()
        assertTrue(iterator.hasNext())
        assertSame(view1, iterator.next())
        assertTrue(iterator.hasNext())
        assertSame(view2, iterator.next())
        assertFalse(iterator.hasNext())
        assertThrows<IndexOutOfBoundsException> {
            iterator.next()
        }
    }

    @Test fun iteratorRemoving() {
        val view1 = View(context)
        viewGroup.addView(view1)
        val view2 = View(context)
        viewGroup.addView(view2)

        val iterator = viewGroup.iterator()

        assertSame(view1, iterator.next())
        iterator.remove()
        assertFalse(view1 in viewGroup)
        assertEquals(1, viewGroup.childCount)

        assertSame(view2, iterator.next())
        iterator.remove()
        assertFalse(view2 in viewGroup)
        assertEquals(0, viewGroup.childCount)
    }

    @Test fun iteratorForEach() {
        val views = listOf(View(context), View(context))
        views.forEach(viewGroup::addView)

        var index = 0
        for (view in viewGroup) {
            assertSame(views[index++], view)
        }
    }

    @Test fun childrenEmpty() {
        viewGroup.children.forEach {
            fail()
        }
    }

    @Test fun children() {
        val views = listOf(View(context), View(context), View(context))
        views.forEach { viewGroup.addView(it) }

        val children = viewGroup.children

        var count = 0
        children.forEachIndexed { index, child ->
            count++
            assertSame(views[index], child)
        }
        assertEquals(3, count)

        // Ensure the Sequence can be consumed twice.
        assertEquals(3, children.count())
    }

    @Test fun descendantsEmpty() {
        viewGroup.descendants.forEach {
            fail()
        }
    }

    @Test fun descendants() {
        val view1 = LinearLayout(context)
        val view2 = View(context)
        val view3 = LinearLayout(context)
        val view4 = View(context)
        val view5 = View(context)

        //   viewGroup
        //    /     \
        // view1    view3
        //   |      /   \
        // view2 view4 view5
        viewGroup.addView(view1)
        viewGroup.addView(view3)
        view1.addView(view2)
        view3.addView(view4)
        view3.addView(view5)

        val views = listOf(view1, view2, view3, view4, view5)
        val descendants = viewGroup.descendants

        var count = 0
        descendants.forEachIndexed { index, descendant ->
            count++
            assertSame(views[index], descendant)
        }
        assertEquals(5, count)

        // Ensure the Sequence can be consumed twice.
        assertEquals(5, descendants.count())
    }

    @Test fun allViewsEmpty() {
        val allViews = viewGroup.allViews

        var count = 0
        allViews.forEach { childView ->
            count++
            assertSame(viewGroup, childView)
        }
        assertEquals(1, count)

        // Ensure the Sequence can be consumed twice.
        assertEquals(1, allViews.count())
    }

    @Test fun allViews() {
        val view1 = LinearLayout(context)
        val view2 = View(context)
        val view3 = LinearLayout(context)
        val view4 = View(context)
        val view5 = View(context)

        //   viewGroup
        //    /     \
        // view1    view3
        //   |      /   \
        // view2 view4 view5
        viewGroup.addView(view1)
        viewGroup.addView(view3)
        view1.addView(view2)
        view3.addView(view4)
        view3.addView(view5)

        val views = listOf(viewGroup, view1, view2, view3, view4, view5)
        val allViews = viewGroup.allViews

        var count = 0
        allViews.forEachIndexed { index, descendant ->
            count++
            assertSame(views[index], descendant)
        }
        assertEquals(6, count)

        // Ensure the Sequence can be consumed twice.
        assertEquals(6, allViews.count())
    }

    @Test fun setMargins() {
        val layoutParams = ViewGroup.MarginLayoutParams(100, 200)
        layoutParams.setMargins(42)
        assertEquals(42, layoutParams.leftMargin)
        assertEquals(42, layoutParams.topMargin)
        assertEquals(42, layoutParams.rightMargin)
        assertEquals(42, layoutParams.bottomMargin)
    }

    @Test fun updateMargins() {
        val layoutParams = ViewGroup.MarginLayoutParams(100, 200)
        layoutParams.updateMargins(top = 10, right = 20)
        assertEquals(0, layoutParams.leftMargin)
        assertEquals(10, layoutParams.topMargin)
        assertEquals(20, layoutParams.rightMargin)
        assertEquals(0, layoutParams.bottomMargin)
    }

    @Test fun updateMarginsNoOp() {
        val layoutParams = ViewGroup.MarginLayoutParams(100, 200)
        layoutParams.setMargins(10, 20, 30, 40)
        layoutParams.updateMargins()
        assertEquals(10, layoutParams.leftMargin)
        assertEquals(20, layoutParams.topMargin)
        assertEquals(30, layoutParams.rightMargin)
        assertEquals(40, layoutParams.bottomMargin)
    }

    @SdkSuppress(minSdkVersion = 17)
    @Test fun updateMarginsRelative() {
        val layoutParams = ViewGroup.MarginLayoutParams(100, 200)
        layoutParams.updateMarginsRelative(start = 10, end = 20)
        assertEquals(0, layoutParams.leftMargin)
        assertEquals(0, layoutParams.topMargin)
        assertEquals(0, layoutParams.rightMargin)
        assertEquals(0, layoutParams.bottomMargin)
        assertEquals(10, layoutParams.marginStart)
        assertEquals(20, layoutParams.marginEnd)
        assertTrue(layoutParams.isMarginRelative)
    }

    @SdkSuppress(minSdkVersion = 17)
    @Test fun updateMarginsRelativeNoOp() {
        val layoutParams = ViewGroup.MarginLayoutParams(100, 200)
        layoutParams.setMargins(10, 20, 30, 40)
        layoutParams.updateMarginsRelative()
        assertEquals(10, layoutParams.leftMargin)
        assertEquals(20, layoutParams.topMargin)
        assertEquals(30, layoutParams.rightMargin)
        assertEquals(40, layoutParams.bottomMargin)
        assertEquals(10, layoutParams.marginStart)
        assertEquals(30, layoutParams.marginEnd)
    }
}
