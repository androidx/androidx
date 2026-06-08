/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.recyclerview.widget

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.test.R
import androidx.recyclerview.test.RecyclerViewTestActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FocusLoopingTest(private val layoutManagerType: LayoutManagerType) {

    enum class LayoutManagerType {
        LINEAR,
        GRID,
        STAGGERED,
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Array<LayoutManagerType> =
            arrayOf(LayoutManagerType.LINEAR, LayoutManagerType.GRID, LayoutManagerType.STAGGERED)
    }

    private fun createLayoutManager(context: Context): RecyclerView.LayoutManager {
        return when (layoutManagerType) {
            LayoutManagerType.LINEAR -> LinearLayoutManager(context)
            LayoutManagerType.GRID -> GridLayoutManager(context, 2)
            LayoutManagerType.STAGGERED ->
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
    }

    @get:Rule val rule = ActivityScenarioRule(RecyclerViewTestActivity::class.java)

    data class SetupResult(
        val recyclerView: RecyclerView,
        val before: TextView,
        val after: TextView,
    )

    private fun setupRecyclerviewWithSurroundingViews(
        itemCount: Int,
        scrollTo: Int,
        isItemFocusable: (Int) -> Boolean = { true },
        isFocusable: Boolean = true,
    ): SetupResult {
        lateinit var rv: RecyclerView
        lateinit var before: TextView
        lateinit var after: TextView
        rule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.focus_search_activity)
            before = activity.findViewById(R.id.before)
            after = activity.findViewById(R.id.after)
            rv = activity.findViewById(R.id.recycler_view)
            rv.layoutManager = createLayoutManager(activity)
            rv.adapter = TestAdapter(itemCount, isItemFocusable)
            rv.isFocusable = isFocusable
            rv.isFocusableInTouchMode = isFocusable

            rv.scrollToPosition(scrollTo)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return SetupResult(rv, before, after)
    }

    private fun setupStandaloneRecyclerview(
        itemCount: Int,
        scrollTo: Int,
        isItemFocusable: (Int) -> Boolean = { true },
    ): RecyclerView {
        lateinit var rv: RecyclerView
        rule.scenario.onActivity { activity ->
            val recyclerView = RecyclerView(activity)
            recyclerView.layoutManager = createLayoutManager(activity)
            recyclerView.adapter = TestAdapter(itemCount, isItemFocusable)
            activity.setContentView(recyclerView)
            rv = recyclerView

            rv.scrollToPosition(scrollTo)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return rv
    }

    private fun requestFocus(view: View) {
        rule.scenario.onActivity { view.requestFocus() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun findAndRequestFocusInRecyclerview(
        fromView: View,
        direction: Int,
        rv: RecyclerView,
    ): View? {
        var nextFocus: View? = null
        rule.scenario.onActivity {
            fromView.focusSearch(direction).requestFocus(direction)
            nextFocus = rv.findFocus()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return nextFocus
    }

    private fun sendTab(shift: Boolean = false) {
        val inst = InstrumentationRegistry.getInstrumentation()
        if (shift) {
            val now = android.os.SystemClock.uptimeMillis()
            inst.sendKeySync(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_TAB,
                    0,
                    KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON,
                )
            )
            inst.sendKeySync(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_TAB,
                    0,
                    KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON,
                )
            )
        } else {
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        }
        inst.waitForIdleSync()
    }

    private fun getViewHolderItemView(rv: RecyclerView, position: Int): View {
        lateinit var itemView: View
        rule.scenario.onActivity {
            val vh = rv.findViewHolderForAdapterPosition(position)
            assertWithMessage("ViewHolder for item $position should not be null")
                .that(vh)
                .isNotNull()
            itemView = vh!!.itemView
        }
        return itemView
    }

    private fun getFirstVisibleAdapterPosition(rv: RecyclerView): Int {
        var pos = RecyclerView.NO_POSITION
        rule.scenario.onActivity { pos = rv.getChildAdapterPosition(rv.getChildAt(0)) }
        return pos
    }

    private fun getLastVisibleAdapterPosition(rv: RecyclerView): Int {
        var pos = RecyclerView.NO_POSITION
        rule.scenario.onActivity {
            pos = rv.getChildAdapterPosition(rv.getChildAt(rv.childCount - 1))
        }
        return pos
    }

    private fun getFirstVisibleFocusableAdapterPosition(rv: RecyclerView): Int {
        var expectedFocusPosition = RecyclerView.NO_POSITION
        rule.scenario.onActivity {
            for (i in 0 until rv.childCount) {
                val child = rv.getChildAt(i)
                if (child.isFocusable) {
                    expectedFocusPosition = rv.getChildAdapterPosition(child)
                    break
                }
            }
        }
        return expectedFocusPosition
    }

    // Focus works different before API 26, so the fix doesn't consistently work there
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_entersAtStart() {
        val (rv, before, _) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(before)
        val nextFocus = findAndRequestFocusInRecyclerview(before, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus item 0").that(pos).isEqualTo(0)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_entersAtStart_oldBehavior() {
        val (rv, before, _) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(before)
        val expectedFocusPosition = getFirstVisibleAdapterPosition(rv)
        val nextFocus = findAndRequestFocusInRecyclerview(before, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus first visible item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchBackward_entersAtEnd() {
        val (rv, _, after) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(after)
        val nextFocus = findAndRequestFocusInRecyclerview(after, View.FOCUS_BACKWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus last item (99)").that(pos).isEqualTo(99)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchBackward_entersAtEnd_oldBehavior() {
        val (rv, _, after) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(after)
        val expectedFocusPosition = getLastVisibleAdapterPosition(rv)
        val nextFocus = findAndRequestFocusInRecyclerview(after, View.FOCUS_BACKWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus last visible item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun tabNavigationForward_entersAtStart() {
        val (rv, before, _) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(before)
        sendTab()
        var nextFocus: View? = null
        rule.scenario.onActivity { nextFocus = rv.findFocus() }

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus item 0").that(pos).isEqualTo(0)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun tabNavigationForward_entersAtStart_oldBehavior() {
        val (rv, before, _) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(before)
        val expectedFocusPosition = getFirstVisibleAdapterPosition(rv)
        sendTab()
        var nextFocus: View? = null
        rule.scenario.onActivity { nextFocus = rv.findFocus() }

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus first visible item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun tabNavigationBackward_entersAtEnd() {
        val (rv, _, after) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(after)
        sendTab(shift = true)
        var nextFocus: View? = null
        rule.scenario.onActivity { nextFocus = rv.findFocus() }

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus last item (99)").that(pos).isEqualTo(99)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun tabNavigationBackward_entersAtEnd_oldBehavior() {
        val (rv, _, after) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(after)
        val expectedFocusPosition = getLastVisibleAdapterPosition(rv)
        sendTab(shift = true)
        var nextFocus: View? = null
        rule.scenario.onActivity { nextFocus = rv.findFocus() }

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus last visible item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_fullLoopWithParent() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 99)
        val lastItem = getViewHolderItemView(rv, 99)
        requestFocus(lastItem)
        val nextFocus = findAndRequestFocusInRecyclerview(lastItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop to item 0").that(pos).isEqualTo(0)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_fullLoopWithParent_oldBehavior() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 99)
        val lastItem = getViewHolderItemView(rv, 99)
        requestFocus(lastItem)
        val expectedFocusPosition = getFirstVisibleAdapterPosition(rv)
        val nextFocus = findAndRequestFocusInRecyclerview(lastItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop to first visible item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchBackward_fullLoopWithParent() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 0)
        val firstItem = getViewHolderItemView(rv, 0)
        requestFocus(firstItem)
        val nextFocus = findAndRequestFocusInRecyclerview(firstItem, View.FOCUS_BACKWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop to last item").that(pos).isEqualTo(99)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchBackward_fullLoopWithParent_oldBehavior() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 0)
        val firstItem = getViewHolderItemView(rv, 0)
        requestFocus(firstItem)
        val expectedFocusPosition = getLastVisibleAdapterPosition(rv)
        val nextFocus = findAndRequestFocusInRecyclerview(firstItem, View.FOCUS_BACKWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop to last visible item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_loopsAtEnd_withNonFocusableItems() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 99) { i -> i >= 2 }
        val lastItem = getViewHolderItemView(rv, 99)
        requestFocus(lastItem)
        val nextFocus = findAndRequestFocusInRecyclerview(lastItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop to item 2").that(pos).isEqualTo(2)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_loopsAtEnd_withNonFocusableItems_oldBehavior() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 99) { i -> i >= 2 }
        val lastItem = getViewHolderItemView(rv, 99)
        requestFocus(lastItem)
        val expectedFocusPosition = getFirstVisibleFocusableAdapterPosition(rv)
        val nextFocus = findAndRequestFocusInRecyclerview(lastItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop to first visible focusable item")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_onlyOneFocusableItemInInitialWindow() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 5) { i -> i == 5 }
        val focusedItem = getViewHolderItemView(rv, 5)
        requestFocus(focusedItem)
        val nextFocus = findAndRequestFocusInRecyclerview(focusedItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop back to item 5").that(pos).isEqualTo(5)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_onlyOneFocusableItemInInitialWindow_oldBehavior() {
        val rv = setupStandaloneRecyclerview(itemCount = 100, scrollTo = 5) { i -> i == 5 }
        val focusedItem = getViewHolderItemView(rv, 5)
        requestFocus(focusedItem)
        val nextFocus = findAndRequestFocusInRecyclerview(focusedItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop back to item 5").that(pos).isEqualTo(5)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_onlyOneFocusableItemOutsideOfInitialWindow() {
        var firstNonVisibleAdapterPosition = 0
        val rv =
            setupStandaloneRecyclerview(itemCount = 100, scrollTo = 80) { i ->
                i == 80 || i == firstNonVisibleAdapterPosition + 3
            }
        rule.scenario.onActivity {
            while (rv.findViewHolderForAdapterPosition(firstNonVisibleAdapterPosition) != null) {
                firstNonVisibleAdapterPosition++
            }
        }
        val focusedItem = getViewHolderItemView(rv, 80)
        requestFocus(focusedItem)
        val nextFocus = findAndRequestFocusInRecyclerview(focusedItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop back to item ${firstNonVisibleAdapterPosition + 3}")
            .that(pos)
            .isEqualTo(firstNonVisibleAdapterPosition + 3)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_onlyOneFocusableItemOutsideOfInitialWindow_oldBehavior() {
        var firstNonVisibleAdapterPosition = 0
        val rv =
            setupStandaloneRecyclerview(itemCount = 100, scrollTo = 80) { i ->
                i == 80 || i == firstNonVisibleAdapterPosition + 3
            }
        rule.scenario.onActivity {
            while (rv.findViewHolderForAdapterPosition(firstNonVisibleAdapterPosition) != null) {
                firstNonVisibleAdapterPosition++
            }
        }
        val focusedItem = getViewHolderItemView(rv, 80)
        requestFocus(focusedItem)
        val nextFocus = findAndRequestFocusInRecyclerview(focusedItem, View.FOCUS_FORWARD, rv)

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should loop back to item 80 on older API levels").that(pos).isEqualTo(80)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_withUnfocusableItems_resultsInFocusedRecyclerView() {
        val (rv, before, _) =
            setupRecyclerviewWithSurroundingViews(
                itemCount = 100,
                scrollTo = 99,
                isItemFocusable = { false },
            )
        requestFocus(before)
        val nextFocus = findAndRequestFocusInRecyclerview(before, View.FOCUS_FORWARD, rv)
        var firstViewHolder: RecyclerView.ViewHolder? = null
        rule.scenario.onActivity { firstViewHolder = rv.findViewHolderForAdapterPosition(0) }

        assertThat(nextFocus).isEqualTo(rv)
        assertThat(firstViewHolder).isNotNull()
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_withUnfocusableItems_resultsInFocusedRecyclerView_oldBehavior() {
        val (rv, before, _) =
            setupRecyclerviewWithSurroundingViews(
                itemCount = 100,
                scrollTo = 99,
                isItemFocusable = { false },
            )
        requestFocus(before)
        val nextFocus = findAndRequestFocusInRecyclerview(before, View.FOCUS_FORWARD, rv)
        var firstViewHolder: RecyclerView.ViewHolder? = null
        rule.scenario.onActivity { firstViewHolder = rv.findViewHolderForAdapterPosition(0) }

        assertThat(nextFocus).isEqualTo(rv)
        assertThat(firstViewHolder).isNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_skipsUnfocusableRecyclerView() {
        val (rv, before, after) =
            setupRecyclerviewWithSurroundingViews(
                itemCount = 100,
                scrollTo = 99,
                isItemFocusable = { false },
                isFocusable = false,
            )
        requestFocus(before)
        val nextFocus = findAndRequestFocusInRecyclerview(before, View.FOCUS_FORWARD, rv)
        var firstViewHolder: RecyclerView.ViewHolder? = null
        rule.scenario.onActivity { firstViewHolder = rv.findViewHolderForAdapterPosition(0) }

        assertThat(nextFocus).isNull()
        assertThat(firstViewHolder).isNotNull()
        assertThat(after.isFocused).isTrue()
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_skipsUnfocusableRecyclerView_oldBehavior() {
        val (rv, before, after) =
            setupRecyclerviewWithSurroundingViews(
                itemCount = 100,
                scrollTo = 99,
                isItemFocusable = { false },
                isFocusable = false,
            )
        requestFocus(before)
        val nextFocus = findAndRequestFocusInRecyclerview(before, View.FOCUS_FORWARD, rv)
        var firstViewHolder: RecyclerView.ViewHolder? = null
        rule.scenario.onActivity { firstViewHolder = rv.findViewHolderForAdapterPosition(0) }

        assertThat(nextFocus).isNull()
        assertThat(firstViewHolder).isNull()
        assertThat(after.isFocused).isTrue()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun focusSearchForward_layoutSuppressed_doesNotEnterAtStart() {
        val (rv, before, _) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(before)
        var nextFocus: View? = null
        rule.scenario.onActivity {
            val target = before.focusSearch(View.FOCUS_FORWARD)
            assertThat(target).isEqualTo(rv)
            rv.suppressLayout(true)
            target.requestFocus(View.FOCUS_FORWARD)
            nextFocus = rv.findFocus()
        }

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should not focus item 0 when layout suppressed")
            .that(pos)
            .isNotEqualTo(0)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun focusSearchForward_layoutSuppressed_doesNotEnterAtStart_oldBehavior() {
        val (rv, before, _) = setupRecyclerviewWithSurroundingViews(itemCount = 100, scrollTo = 50)
        requestFocus(before)
        var expectedFocusPosition = RecyclerView.NO_POSITION
        var nextFocus: View? = null
        rule.scenario.onActivity {
            expectedFocusPosition = rv.getChildAdapterPosition(rv.getChildAt(0))
            val target = before.focusSearch(View.FOCUS_FORWARD)
            assertThat(target).isEqualTo(rv.getChildAt(0))
            rv.suppressLayout(true)
            target.requestFocus(View.FOCUS_FORWARD)
            nextFocus = rv.findFocus()
        }

        assertThat(nextFocus).isNotNull()
        assertThat(nextFocus).isNotEqualTo(rv)
        val pos = rv.getChildAdapterPosition(nextFocus!!)
        assertWithMessage("Should focus first visible item when layout suppressed")
            .that(pos)
            .isEqualTo(expectedFocusPosition)
    }

    @Test
    fun findFirstFocusableChild_nullChild_handledGracefully() {
        var lm: RecyclerView.LayoutManager? = null
        rule.scenario.onActivity { activity ->
            lm =
                when (layoutManagerType) {
                    LayoutManagerType.LINEAR ->
                        object : LinearLayoutManager(activity) {
                            override fun getChildCount(): Int = 1

                            override fun getChildAt(index: Int): View? = null
                        }
                    LayoutManagerType.GRID ->
                        object : GridLayoutManager(activity, 2) {
                            override fun getChildCount(): Int = 1

                            override fun getChildAt(index: Int): View? = null
                        }
                    LayoutManagerType.STAGGERED ->
                        object : StaggeredGridLayoutManager(2, VERTICAL) {
                            override fun getChildCount(): Int = 1

                            override fun getChildAt(index: Int): View? = null
                        }
                }
        }
        when (val layout = lm) {
            is LinearLayoutManager -> {
                assertThat(layout.findFirstFocusableChildClosestToStart()).isNull()
                assertThat(layout.findFirstFocusableChildClosestToEnd()).isNull()
            }
            is StaggeredGridLayoutManager -> {
                assertThat(layout.findFirstFocusableChildClosestToStart()).isNull()
                assertThat(layout.findFirstFocusableChildClosestToEnd()).isNull()
            }
        }
    }

    private class TestAdapter(
        private val itemCount: Int,
        private val isItemFocusable: (Int) -> Boolean = { true },
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
            view.minimumHeight = 100
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val isItemFocusable = isItemFocusable(position)
            holder.itemView.isFocusable = isItemFocusable
            holder.itemView.isFocusableInTouchMode = isItemFocusable
        }

        override fun getItemCount() = itemCount
    }
}
