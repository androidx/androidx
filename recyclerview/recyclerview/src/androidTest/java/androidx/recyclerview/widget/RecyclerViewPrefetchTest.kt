/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewPrefetchTest : BaseRecyclerViewInstrumentationTest() {
    private inner class PrefetchLayoutManager : TestLayoutManager() {
        var prefetchLatch = CountDownLatch(1)

        override fun canScrollHorizontally(): Boolean = false

        override fun canScrollVertically(): Boolean = true

        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            super.onLayoutChildren(recycler, state)
            detachAndScrapAttachedViews(recycler)
            layoutRange(recycler, 0, 5)
        }

        override fun onLayoutCompleted(state: RecyclerView.State) {
            super.onLayoutCompleted(state)
            layoutLatch.countDown()
        }

        override fun collectAdjacentPrefetchPositions(
            dx: Int,
            dy: Int,
            state: RecyclerView.State,
            layoutPrefetchRegistry: LayoutPrefetchRegistry,
        ) {
            if (dy > 0) {
                // only a valid prefetch if it gets direction correct, since that's what drives
                // which item to load
                prefetchLatch.countDown()
            }
            layoutPrefetchRegistry.addPosition(6, 0)
        }

        @Throws(InterruptedException::class)
        fun waitForPrefetch(time: Int) {
            assertThat(prefetchLatch.await(time.toLong(), TimeUnit.SECONDS), `is`(true))
            instrumentation.runOnMainSync {}
        }
    }

    private fun cachedViews(): ArrayList<RecyclerView.ViewHolder> {
        return mRecyclerView.mRecycler.mCachedViews
    }

    @Test
    @Throws(Throwable::class)
    fun prefetchTest() = runBlocking {
        val layout: PrefetchLayoutManager
        withContext(Dispatchers.Main) {
            val recyclerView = RecyclerView(activity)
            recyclerView.adapter = TestAdapter(50)
            layout = PrefetchLayoutManager()
            recyclerView.layoutManager = layout

            layout.expectLayouts(1)
            setRecyclerView(recyclerView)
        }

        layout.waitForLayout(10)
        withContext(Dispatchers.Main) {
            assertThat(layout.prefetchLatch.count, `is`(1L)) // shouldn't have fired yet
            assertThat(cachedViews().size, `is`(0))
        }

        smoothScrollBy(50)
        layout.waitForPrefetch(10)
        withContext(Dispatchers.Main) {
            assertThat(cachedViews().size, `is`(1))
            assertThat(cachedViews()[0].absoluteAdapterPosition, `is`(6))
        }
    }
}
