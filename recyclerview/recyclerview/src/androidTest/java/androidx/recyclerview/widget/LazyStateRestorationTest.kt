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

package androidx.recyclerview.widget

import android.content.Context
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.ViewGroup
import androidx.recyclerview.widget.BaseRecyclerViewInstrumentationTest.Item
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.ALLOW
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
class LazyStateRestorationTest(
    private val layoutManagerFactory: LayoutManagerFactory,
    /**
     * Views are not designed to restore state multiple times yet there is a common patter of doing
     * it that used to work so we add support for it.
     */
    private val reuseRecyclerView: Boolean,
) {
    private lateinit var recyclerView: RecyclerView
    private val items = (0..99).map { Item(it, "text $it") }

    @Before
    fun init() {
        recyclerView = RecyclerView(getApplicationContext())
        recyclerView.itemAnimator = null
        recyclerView.adapter = LazyStateAdapter(items)
        recyclerView.layoutManager = createLayoutManager()
    }

    private fun createLayoutManager(): RecyclerView.LayoutManager {
        return layoutManagerFactory.create(getApplicationContext())
    }

    private fun measureAndLayout() {
        measure()
        layout()
    }

    private fun measure() {
        recyclerView.measure(AT_MOST or 320, AT_MOST or 240)
    }

    private fun layout() {
        recyclerView.layout(0, 0, 320, 320)
    }

    private fun restore(setAdapter: Boolean): Parcelable {
        val prevAdapter =
            checkNotNull(recyclerView.adapter as? LazyStateAdapter) {
                "Previous RecyclerView should have a LazyStateAdapter for the test"
            }
        val savedState = saveState()
        if (!reuseRecyclerView) {
            recyclerView = RecyclerView(getApplicationContext())
            recyclerView.layoutManager = createLayoutManager()
        }
        recyclerView.onRestoreInstanceState(savedState)
        if (setAdapter) {
            recyclerView.adapter = LazyStateAdapter(prevAdapter.items)
        }
        return savedState
    }

    private fun saveState(): Parcelable {
        val parcel = Parcel.obtain()
        val parcelSuffix = UUID.randomUUID().toString()
        val savedState = recyclerView.onSaveInstanceState()
        savedState!!.writeToParcel(parcel, 0)
        parcel.writeString(parcelSuffix)
        // reset position for reading
        parcel.setDataPosition(0)
        return savedState
    }

    @Test
    @UiThreadTest
    fun default() {
        measureAndLayout()
        recyclerView.scrollBy(0, 113)
        val coordinates = recyclerView.collectChildCoordinates()
        restore(setAdapter = true)
        measureAndLayout()
        val restoredCoordinates = recyclerView.collectChildCoordinates()
        assertThat(restoredCoordinates).isEqualTo(coordinates)
    }

    @Test
    @UiThreadTest
    fun countBased() {
        measureAndLayout()
        recyclerView.scrollBy(0, 113)
        val coordinates = recyclerView.collectChildCoordinates()
        restore(setAdapter = false)
        val adapter = LazyStateAdapter(emptyList())
        adapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        recyclerView.adapter = adapter
        measureAndLayout()
        // Assumption check
        assertThat(recyclerView.collectChildCoordinates()).isEmpty()
        adapter.items = items
        adapter.notifyDataSetChanged()
        measureAndLayout()
        val restored = recyclerView.collectChildCoordinates()
        assertThat(restored).isEqualTo(coordinates)
    }

    @Test
    @UiThreadTest
    fun manual() {
        measureAndLayout()
        recyclerView.scrollBy(0, 113)
        val coordinates = recyclerView.collectChildCoordinates()
        restore(setAdapter = true)
        val adapter = recyclerView.adapter as LazyStateAdapter
        adapter.stateRestorationPolicy = PREVENT
        measureAndLayout()
        // Assumption check, we should layout whatever is available
        assertThat(recyclerView.collectChildCoordinates()).isNotEmpty()
        // make sure we didn't restore
        assertThat(recyclerView.collectChildCoordinates()).isNotEqualTo(coordinates)

        // notifying item change does not matter
        adapter.items = adapter.items.subList(0, 90)
        adapter.notifyDataSetChanged()
        measureAndLayout()
        // still not restored
        assertThat(recyclerView.collectChildCoordinates()).isNotEqualTo(coordinates)
        adapter.stateRestorationPolicy = ALLOW
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout()
        // now we should restore
        val restored = recyclerView.collectChildCoordinates()
        assertThat(restored).isEqualTo(coordinates)
    }

    @Test
    @UiThreadTest
    fun scrollToPositionOverridesState() {
        measureAndLayout()
        recyclerView.scrollBy(0, 113)
        measureAndLayout()
        val savedStateCoordinates = recyclerView.collectChildCoordinates()
        restore(setAdapter = true)
        val adapter = recyclerView.adapter as LazyStateAdapter
        adapter.stateRestorationPolicy = PREVENT
        measureAndLayout()
        // state is not restored yet, trigger a scroll but also restore in the same layout
        recyclerView.scrollToPosition(40)
        adapter.stateRestorationPolicy = ALLOW
        measureAndLayout()
        // relayout so that SGLM can settle
        recyclerView.requestLayout()
        measureAndLayout()
        val coordinates = recyclerView.collectChildCoordinates()
        assertThat(coordinates).isNotEqualTo(savedStateCoordinates)
    }

    @Test
    @UiThreadTest
    fun scrollToPositionDoesNotPreventFutureRestorations_restoreViaRecyclerView() {
        scrollToPositionDoesNotPreventFutureRestorations(restoreViaLayoutManager = false)
    }

    @Test
    @UiThreadTest
    fun scrollToPositionDoesNotPreventFutureRestorations_restoreViaLayoutManager() {
        scrollToPositionDoesNotPreventFutureRestorations(restoreViaLayoutManager = true)
    }

    private fun scrollToPositionDoesNotPreventFutureRestorations(restoreViaLayoutManager: Boolean) {
        measureAndLayout()
        recyclerView.scrollBy(0, 113)
        val savedStateCoordinates = recyclerView.collectChildCoordinates()
        val savedState = restore(setAdapter = true)
        measureAndLayout()
        recyclerView.scrollToPosition(40)
        measureAndLayout()
        assertThat(recyclerView.findViewHolderForAdapterPosition(40)).isNotNull()
        // relayout so that SGLM can settle
        recyclerView.requestLayout()
        measureAndLayout()
        // now give LM another state, it should be used.
        // it is not usually kosher to do this yet there is precedence in existing app code so
        // we have a test not to break it in the future.
        if (restoreViaLayoutManager) {
            val layoutState = (savedState as RecyclerView.SavedState).mLayoutState
            recyclerView.layoutManager?.onRestoreInstanceState(layoutState)
        } else {
            recyclerView.onRestoreInstanceState(savedState)
        }
        measureAndLayout()
        // let SGLM settle
        measureAndLayout()
        val coordinates = recyclerView.collectChildCoordinates()
        assertThat(coordinates).isEqualTo(savedStateCoordinates)
    }

    companion object {
        @Parameterized.Parameters(name = "{0}_reUseRV_{1}")
        @JvmStatic
        fun params(): List<Array<Any>> =
            listOf(
                    LinearLayoutManagerFactory(),
                    GridLayoutManagerFactory(),
                    StaggeredGridLayoutManagerFactory(),
                )
                .flatMap { listOf(arrayOf(it, true), arrayOf(it, false)) }
    }

    abstract class LayoutManagerFactory {
        abstract fun create(context: Context): RecyclerView.LayoutManager

        abstract fun describe(): String

        override fun toString() = describe()
    }

    private class LinearLayoutManagerFactory : LayoutManagerFactory() {
        override fun create(context: Context): RecyclerView.LayoutManager {
            return LinearLayoutManager(context)
        }

        override fun describe() = "LinearLayoutManager"
    }

    private class GridLayoutManagerFactory : LayoutManagerFactory() {
        override fun create(context: Context): RecyclerView.LayoutManager {
            return GridLayoutManager(context, 3)
        }

        override fun describe() = "GridLayoutManager"
    }

    private class StaggeredGridLayoutManagerFactory : LayoutManagerFactory() {
        override fun create(context: Context): RecyclerView.LayoutManager {
            return StaggeredGridLayoutManager(3, VERTICAL).also {
                it.gapStrategy = GAP_HANDLING_NONE
            }
        }

        override fun describe() = "StaggeredGridLayoutManager"
    }

    private class LazyStateAdapter(var items: List<Item>) :
        RecyclerView.Adapter<LazyStateViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LazyStateViewHolder {
            return LazyStateViewHolder(parent.context)
        }

        override fun onBindViewHolder(holder: LazyStateViewHolder, position: Int) {
            holder.bindTo(items[position])
        }

        override fun getItemCount() = items.size
    }

    private class LazyStateViewHolder(context: Context) : RecyclerView.ViewHolder(View(context)) {
        var item: Item? = null

        fun bindTo(item: Item) {
            this.item = item
            itemView.layoutParams =
                RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    25 + (item.mId % 10),
                )
        }
    }

    private fun RecyclerView.collectChildCoordinates(): Map<Item, Rect> {
        val items = LinkedHashMap<Item, Rect>()
        val layoutBounds = Rect(0, 0, width, height)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child!!.layoutParams as RecyclerView.LayoutParams
            val vh = lp.mViewHolder as LazyStateViewHolder
            val rect = Rect(child.left, child.top, child.right, child.bottom)
            if (rect.intersect(layoutBounds)) {
                items[vh.item!!] = rect
            }
        }
        return items
    }
}
