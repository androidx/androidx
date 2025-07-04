/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION") // b/220884819

package androidx.paging

import android.app.Application
import android.content.Context
import android.os.Parcelable
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.ALLOW
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * We are only capable of restoring state if one the two is valid: a) pager's flow is cached in the
 * view model (only for config change) b) data source is counted and placeholders are enabled (both
 * config change and app restart)
 *
 * Both of these cases actually work without using the initial key, except it is relatively slower
 * in option B because we need to load all items from initial key to the required position.
 *
 * This test validates those two cases for now. For more complicated cases, we need some helper as
 * developer needs to intervene to provide more information.
 */
@ExperimentalCoroutinesApi
@ExperimentalTime
@MediumTest
@RunWith(AndroidJUnit4::class)
class StateRestorationTest {
    /**
     * List of dispatchers we track in the test for idling + pushing execution. We have 3
     * dispatchers for more granular control: main, and background for pager. testScope for running
     * tests.
     */
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = StandardTestDispatcher(scheduler)
    private var backgroundDispatcher = StandardTestDispatcher(scheduler)

    /**
     * A fake lifecycle scope for collections that get cancelled when we recreate the recyclerview.
     */
    private lateinit var lifecycleScope: TestScope
    private lateinit var recyclerView: TestRecyclerView
    private lateinit var layoutManager: RestoreAwareLayoutManager
    private lateinit var adapter: TestAdapter

    @Before
    fun init() {
        createRecyclerView()
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun restoreState_withPlaceholders() {
        runTest {
            collectPagesAsync(createPager(pageSize = 100, enablePlaceholders = true).flow)
            measureAndLayout()
            val visible = recyclerView.captureSnapshot()
            assertThat(visible).isNotEmpty()
            scrollToPosition(50)
            val expected = recyclerView.captureSnapshot()
            saveAndRestore()
            // make sure state is not restored before items are loaded
            assertThat(layoutManager.restoredState).isFalse()
            // pause item loads
            val delayedJob =
                launch(start = CoroutineStart.LAZY) {
                    collectPagesAsync(createPager(pageSize = 10, enablePlaceholders = true).flow)
                }
            measureAndLayout()
            // item load is paused, still shouldn't restore state
            assertThat(layoutManager.restoredState).isFalse()

            // now load items
            delayedJob.start()

            measureAndLayout()
            assertThat(layoutManager.restoredState).isTrue()
            assertThat(recyclerView.captureSnapshot()).containsExactlyElementsIn(expected)
        }
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun restoreState_withoutPlaceholders_cachedIn() {
        runTest {
            val pager = createPager(pageSize = 60, enablePlaceholders = false)
            val cacheScope = TestScope(Job() + scheduler)
            val cachedFlow = pager.flow.cachedIn(cacheScope)
            collectPagesAsync(cachedFlow)
            measureAndLayout()
            // now scroll
            scrollToPosition(50)
            val snapshot = recyclerView.captureSnapshot()
            saveAndRestore()
            assertThat(layoutManager.restoredState).isFalse()
            collectPagesAsync(cachedFlow)
            measureAndLayout()
            assertThat(layoutManager.restoredState).isTrue()
            val restoredSnapshot = recyclerView.captureSnapshot()
            assertThat(restoredSnapshot).containsExactlyElementsIn(snapshot)
            cacheScope.cancel()
        }
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun emptyNewPage_allowRestoration() {
        // check that we don't block restoration indefinitely if new pager is empty.
        runTest {
            val pager = createPager(pageSize = 60, enablePlaceholders = true)
            collectPagesAsync(pager.flow)
            measureAndLayout()
            scrollToPosition(50)
            saveAndRestore()
            assertThat(layoutManager.restoredState).isFalse()

            val emptyPager = createPager(pageSize = 10, itemCount = 0, enablePlaceholders = true)
            collectPagesAsync(emptyPager.flow)
            measureAndLayout()
            assertThat(layoutManager.restoredState).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun userOverridesStateRestoration() {
        runTest {
            val pager = createPager(pageSize = 40, enablePlaceholders = true)
            collectPagesAsync(pager.flow)
            measureAndLayout()
            scrollToPosition(20)
            val snapshot = recyclerView.captureSnapshot()
            saveAndRestore()
            val pager2 = createPager(pageSize = 40, enablePlaceholders = true)
            // when user calls prevent, we should not trigger state restoration even after we
            // receive the first page
            adapter.stateRestorationPolicy = PREVENT
            collectPagesAsync(pager2.flow)
            measureAndLayout()
            assertThat(layoutManager.restoredState).isFalse()
            // make sure test did work as expected, that is, new items are loaded
            assertThat(adapter.itemCount).isGreaterThan(0)
            // now if user allows it, restoration should happen properly
            adapter.stateRestorationPolicy = ALLOW
            measureAndLayout()
            assertThat(layoutManager.restoredState).isTrue()
            assertThat(recyclerView.captureSnapshot()).isEqualTo(snapshot)
        }
    }

    private fun createRecyclerView() {
        // cancel previous lifecycle if it exists
        if (this::lifecycleScope.isInitialized) {
            this.lifecycleScope.cancel()
        }
        lifecycleScope = TestScope(SupervisorJob() + mainDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        recyclerView = TestRecyclerView(context)
        recyclerView.itemAnimator = null
        adapter = TestAdapter()
        recyclerView.adapter = adapter
        layoutManager = RestoreAwareLayoutManager(context)
        recyclerView.layoutManager = layoutManager
    }

    private fun scrollToPosition(pos: Int) {
        while (adapter.itemCount <= pos) {
            val prevSize = adapter.itemCount
            adapter.triggerItemLoad(prevSize - 1)
            scheduler.runCurrent()
            // this might be an issue with dropping but it is not the case here
            assertWithMessage("more items should be loaded")
                .that(adapter.itemCount)
                .isGreaterThan(prevSize)
        }
        scheduler.runCurrent()
        recyclerView.scrollToPosition(pos)
        measureAndLayout()
        val child = layoutManager.findViewByPosition(pos)
        assertWithMessage("scrolled child $pos exists").that(child).isNotNull()

        val vh = recyclerView.getChildViewHolder(child!!) as ItemViewHolder
        assertWithMessage("scrolled child should be fully loaded").that(vh.item).isNotNull()
    }

    private fun measureAndLayout() {
        scheduler.runCurrent()
        while (recyclerView.isLayoutRequested) {
            measure()
            layout()
            scheduler.runCurrent()
        }
    }

    private fun measure() {
        recyclerView.measure(EXACTLY or RV_WIDTH, EXACTLY or RV_HEIGHT)
    }

    private fun layout() {
        recyclerView.layout(0, 0, 100, 200)
    }

    private fun saveAndRestore() {
        val state = recyclerView.saveState()
        createRecyclerView()
        recyclerView.restoreState(state!!)
        measureAndLayout()
    }

    private fun runTest(block: TestScope.() -> Unit) =
        runTest(UnconfinedTestDispatcher(scheduler)) {
            try {
                block()
            } finally {
                scheduler.runCurrent()
                // always cancel the lifecycle scope to ensure any collection there ends
                if (this@StateRestorationTest::lifecycleScope.isInitialized) {
                    lifecycleScope.cancel()
                }
            }
        }

    /** collects pages in the lifecycle scope and sends them to the adapter */
    private fun collectPagesAsync(flow: Flow<PagingData<Item>>) {
        val targetAdapter = adapter
        lifecycleScope.launch { flow.collectLatest { targetAdapter.submitData(it) } }
    }

    private fun createPager(
        pageSize: Int,
        enablePlaceholders: Boolean,
        itemCount: Int = 100,
        initialKey: Int? = null,
    ): Pager<Int, Item> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = enablePlaceholders),
            initialKey = initialKey,
            pagingSourceFactory = {
                ItemPagingSource(
                    context = backgroundDispatcher,
                    items = (0 until itemCount).map { Item(it) },
                )
            },
        )
    }

    /** Returns the list of all visible items in the recyclerview including their locations. */
    private fun RecyclerView.captureSnapshot(): List<PositionSnapshot> {
        return (0 until childCount).mapNotNull {
            val child = getChildAt(it)
            // if child is not visible, ignore it as RV might have extra views around the visible
            // area.
            if (child.top >= height || child.bottom <= 0) {
                // not visible, ignore
                null
            } else {
                val vh = getChildViewHolder(child)
                (vh as ItemViewHolder).captureSnapshot()
            }
        }
    }

    class ItemView(context: Context) : View(context)

    class ItemViewHolder(context: Context) : RecyclerView.ViewHolder(ItemView(context)) {
        var item: Item? = null
            private set

        init {
            itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }

        fun captureSnapshot(): PositionSnapshot {
            val item = checkNotNull(item)
            return PositionSnapshot(item = item, top = itemView.top, bottom = itemView.bottom)
        }

        fun bindTo(item: Item?) {
            this.item = item
            // setting placeholder height to 0 creates a weird jumping bug, investigate
            itemView.layoutParams.height = item?.height ?: RV_HEIGHT / 10
        }
    }

    data class Item(val id: Int, val height: Int = (RV_HEIGHT / 10) + (1 + (id % 10))) {
        companion object {
            val DIFF_CALLBACK =
                object : DiffUtil.ItemCallback<Item>() {
                    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                        return oldItem.id == newItem.id
                    }

                    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                        return oldItem == newItem
                    }
                }
        }
    }

    inner class TestAdapter :
        PagingDataAdapter<Item, ItemViewHolder>(
            diffCallback = Item.DIFF_CALLBACK,
            mainDispatcher = mainDispatcher,
            workerDispatcher = backgroundDispatcher,
        ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(parent.context)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bindTo(getItem(position))
        }

        fun triggerItemLoad(pos: Int) = super.getItem(pos)
    }

    class ItemPagingSource(private val context: CoroutineContext, private val items: List<Item>) :
        PagingSource<Int, Item>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
            return withContext(context) {
                val key = params.key ?: 0
                val isPrepend = params is LoadParams.Prepend
                val start = if (isPrepend) key - params.loadSize + 1 else key
                val end = if (isPrepend) key + 1 else key + params.loadSize

                LoadResult.Page(
                    data = items.subList(maxOf(0, start), minOf(end, items.size)),
                    prevKey = if (start > 0) start - 1 else null,
                    nextKey = if (end < items.size) end else null,
                    itemsBefore = maxOf(0, start),
                    itemsAfter = maxOf(0, items.size - end),
                )
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Item>): Int? = null
    }

    /** Snapshot of an item in RecyclerView. */
    data class PositionSnapshot(val item: Item, val top: Int, val bottom: Int)

    /** RecyclerView class that allows saving and restoring state. */
    class TestRecyclerView(context: Context) : RecyclerView(context) {
        fun restoreState(state: Parcelable) {
            super.onRestoreInstanceState(state)
        }

        fun saveState(): Parcelable? {
            return super.onSaveInstanceState()
        }
    }

    /**
     * A layout manager that tracks whether state is restored or not so that we can assert on it.
     */
    class RestoreAwareLayoutManager(context: Context) : LinearLayoutManager(context) {
        var restoredState = false

        override fun onRestoreInstanceState(state: Parcelable) {
            super.onRestoreInstanceState(state)
            restoredState = true
        }
    }

    companion object {
        private const val RV_HEIGHT = 200
        private const val RV_WIDTH = 100
    }
}
