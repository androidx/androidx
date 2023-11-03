/*
 * Copyright 2021 The Android Open Source Project
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
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.customview.poolingcontainer.addPoolingContainerListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
/**
 * Note: this test's structure largely parallels AndroidComposeViewsRecyclerViewTest
 * (though there are notable implementation differences)
 *
 * Consider if new tests added here should also be added there.
 */
class PoolingContainerRecyclerViewTest : BaseRecyclerViewInstrumentationTest() {
    @get:Rule
    val animationRule = AnimationDurationScaleRule.create()

    @Before
    fun setup() {
        val rv = RecyclerView(activity)
        setRecyclerView(rv, false, false)
        setUpRecyclerView(rv)
    }

    private fun setUpRecyclerView(recyclerView: RecyclerView) {
        runOnUiThread {
            // Animators cause items to stick around and prevent clean rebinds, which we don't want,
            // since it makes testing this less straightforward.
            recyclerView.itemAnimator = null
            recyclerView.layoutManager =
                LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            recyclerView.layoutParams = TestedFrameLayout.FullControlLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100
            )
        }
    }

    @Test
    fun allItemsChanged_noDisposals() {
        val adapter = PoolingContainerTestAdapter(activity, 100)
        setAdapter(adapter)
        instrumentation.runOnMainSync { }

        // All items created and bound
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.binds).isEqualTo(100)

        instrumentation.runOnMainSync { adapter.notifyItemRangeChanged(0, 100) }
        instrumentation.runOnMainSync { }

        // All items changed: no new creations, but all items rebound
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)
        assertThat(adapter.binds).isEqualTo(200)
    }

    @Test
    fun viewDiscarded_allDisposed() {
        val adapter = PoolingContainerTestAdapter(activity, 100)
        setAdapter(adapter)
        instrumentation.runOnMainSync { }
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)
        instrumentation.runOnMainSync { activity.container.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
    }

    @Test
    fun reattachedAndDetached_disposedTwice() {
        val adapter = PoolingContainerTestAdapter(activity, 100)
        setAdapter(adapter)
        instrumentation.runOnMainSync { }

        // Initially added: all items created, no disposals
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)

        instrumentation.runOnMainSync { activity.container.removeAllViews() }

        // Removed: all items disposed
        assertThat(adapter.releases).isEqualTo(100)

        instrumentation.runOnMainSync { activity.container.addView(mRecyclerView) }

        // Re-added: no new disposals, no new creations
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(100)

        instrumentation.runOnMainSync { activity.container.removeAllViews() }

        // Removed again: all items disposed a second time
        assertThat(adapter.releases).isEqualTo(200)
    }

    @Test
    fun poolReplaced_allDisposed() = runBlocking {
        val adapter = PoolingContainerTestAdapter(activity, 100, 2)
        instrumentation.runOnMainSync {
            val pool = mRecyclerView.recycledViewPool
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
        }
        setAdapter(adapter)
        instrumentation.runOnMainSync { }
        assertThat(mRecyclerView.height).isEqualTo(100)
        assertThat(adapter.creations).isEqualTo(50)

        // Scroll to put some views into the shared pool
        instrumentation.runOnMainSync {
            mRecyclerView.smoothScrollBy(0, 100)
        }

        mRecyclerView.awaitScrollIdle()

        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)
        assertThat(mRecyclerView.recycledViewPool.mAttachCountForClearing).isEqualTo(1)
        assertThat(mRecyclerView.recycledViewPool.size())
            .isEqualTo(50 - mRecyclerView.mRecycler.mViewCacheMax)

        // Swap pool, confirm contents of old pool are disposed
        instrumentation.runOnMainSync {
            mRecyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())
        }
        instrumentation.runOnMainSync { recyclerViewContainer.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
    }

    @Test
    fun poolCleared_allDisposed() = runBlocking {
        val adapter = PoolingContainerTestAdapter(activity, 100, 2)
        instrumentation.runOnMainSync {
            val pool = mRecyclerView.recycledViewPool
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
        }
        setAdapter(adapter)
        instrumentation.runOnMainSync { }

        // Scroll to put some views into the shared pool
        instrumentation.runOnMainSync {
            mRecyclerView.smoothScrollBy(0, 100)
        }

        mRecyclerView.awaitScrollIdle()

        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)
        assertThat(mRecyclerView.recycledViewPool.mAttachCountForClearing).isEqualTo(1)
        assertThat(mRecyclerView.recycledViewPool.size())
            .isEqualTo(50 - mRecyclerView.mRecycler.mViewCacheMax)

        // Clear pool, confirm contents of pool are disposed
        instrumentation.runOnMainSync {
            mRecyclerView.recycledViewPool.clear()
        }
        instrumentation.runOnMainSync { recyclerViewContainer.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
    }

    @Test
    fun setAdapter_allDisposed() {
        // Replacing the adapter when it is the only adapter attached to the pool means that
        // the pool is cleared, so everything should be disposed.
        doSetOrSwapTest(expectedDisposalsAfterBlock = 100) {
            mRecyclerView.adapter = it
        }
    }

    @Test
    fun swapAdapter_noDisposals() {
        doSetOrSwapTest(expectedDisposalsAfterBlock = 0) {
            mRecyclerView.swapAdapter(it, false)
        }
    }

    @Test
    fun setAdapterToNull_allDisposed() {
        doSetOrSwapTest(expectedDisposalsAfterBlock = 100) {
            mRecyclerView.adapter = null
        }
    }

    private fun doSetOrSwapTest(
        expectedDisposalsAfterBlock: Int,
        setOrSwapBlock: (PoolingContainerTestAdapter) -> Unit,
    ) = runBlocking {
        val adapter = PoolingContainerTestAdapter(activity, 100, 2)
        val adapter2 = PoolingContainerTestAdapter(activity, 100, 2)
        instrumentation.runOnMainSync {
            val pool = mRecyclerView.recycledViewPool
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
        }
        setAdapter(adapter)
        instrumentation.runOnMainSync { }

        // Scroll to put some views into the shared pool
        instrumentation.runOnMainSync {
            mRecyclerView.smoothScrollBy(0, 100)
        }

        mRecyclerView.awaitScrollIdle()

        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)
        assertThat(mRecyclerView.recycledViewPool.mAttachCountForClearing).isEqualTo(1)
        assertThat(mRecyclerView.recycledViewPool.size())
            .isEqualTo(50 - mRecyclerView.mRecycler.mViewCacheMax)

        // Set or swap adapter, confirm contents of pool are correct
        instrumentation.runOnMainSync {
            setOrSwapBlock(adapter2)
        }
        assertThat(adapter.releases + adapter2.releases).isEqualTo(expectedDisposalsAfterBlock)

        // Remove the RecyclerView, confirm everything is disposed
        instrumentation.runOnMainSync { recyclerViewContainer.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
        assertThat(adapter2.creations).isEqualTo(adapter2.releases)
        // ...and that nothing unexpected happened
        assertThat(adapter.creations).isEqualTo(100)
    }

    @Test
    fun overflowingScrapTest() {
        lateinit var adapter: PoolingContainerTestAdapter
        instrumentation.runOnMainSync {
            adapter = PoolingContainerTestAdapter(activity, 100)
            setAdapter(adapter)
            val pool = mRecyclerView.recycledViewPool
            for (i in 0..9) {
                // We'll generate more scrap views of each type than this
                pool.setMaxRecycledViews(i, 3)
            }
        }

        instrumentation.runOnMainSync { }

        // All items created and bound
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.binds).isEqualTo(100)

        // Simulate removing and re-adding the first 100 items
        instrumentation.runOnMainSync {
            adapter.notifyItemRangeRemoved(0, 100)
            adapter.notifyItemRangeInserted(0, 100)
        }
        instrumentation.runOnMainSync { }

        assertThat(adapter.creations).isEqualTo(200)

        instrumentation.runOnMainSync { recyclerViewContainer.removeAllViews() }

        // Make sure that all views were disposed, including those that never made it to the pool
        assertThat(adapter.releases).isEqualTo(200)
    }

    @Test
    fun sharedViewPool() {
        val itemViewCacheSize = 2
        instrumentation.runOnMainSync {
            activity.container.removeAllViews()
        }
        val lp1 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        val lp2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        val rv1: RecyclerView = mRecyclerView.also { it.layoutParams = lp1 }
        lateinit var rv2: RecyclerView
        lateinit var container: LinearLayout
        val pool = RecyclerView.RecycledViewPool()
        val adapter1 = PoolingContainerTestAdapter(activity, 100, 10)
        val adapter2 = PoolingContainerTestAdapter(activity, 100, 10)
        instrumentation.runOnMainSync {
            rv2 = RecyclerView(activity).also { setUpRecyclerView(it); it.layoutParams = lp2 }
            container = LinearLayout(activity).also {
                it.layoutParams = TestedFrameLayout.FullControlLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
                )
                it.orientation = LinearLayout.VERTICAL
            }
            rv1.setItemViewCacheSize(itemViewCacheSize)
            rv2.setItemViewCacheSize(itemViewCacheSize)
            rv1.adapter = adapter1
            rv2.adapter = adapter2
            container.addView(rv1)
            container.addView(rv2)
            activity.container.addView(container)
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
            rv1.setRecycledViewPool(pool)
            rv2.setRecycledViewPool(pool)
        }

        instrumentation.runOnMainSync { }
        assertThat(adapter1.creations).isEqualTo(10)

        // Scroll to put some views into the shared pool
        repeat(10) {
            instrumentation.runOnMainSync {
                rv1.scrollBy(0, 10)
            }
        }

        // The RV keeps a couple items in its view cache before returning them to the pool
        val expectedRecycledItems = 10 - itemViewCacheSize
        assertThat(pool.getRecycledViewCount(0)).isEqualTo(expectedRecycledItems)

        // Nothing should have been disposed yet, everything should have gone to the pool
        assertThat(adapter1.releases + adapter2.releases).isEqualTo(0)

        val adapter1Creations = adapter1.creations
        // There were 10, we scrolled 10 more into view, plus maybe prefetching
        assertThat(adapter1Creations).isAtLeast(20)

        // Remove the first RecyclerView
        instrumentation.runOnMainSync {
            container.removeView(rv1)
        }
        instrumentation.runOnMainSync { } // get the relayout

        // After the first RecyclerView is removed, we expect everything it created to be disposed,
        // *except* for what's in the shared pool
        assertThat(adapter1.creations).isEqualTo(adapter1Creations) // just checking
        assertThat(pool.size()).isEqualTo(expectedRecycledItems)
        assertThat(adapter1.releases).isEqualTo(adapter1Creations - expectedRecycledItems)
        assertThat(adapter2.creations).isEqualTo(20) // it's twice as tall with rv1 gone
        assertThat(adapter2.releases).isEqualTo(0) // it hasn't scrolled

        instrumentation.runOnMainSync {
            container.removeView(rv2)
        }
        assertThat(adapter1.creations).isEqualTo(adapter1Creations) // just to be really sure...
        assertThat(adapter1.releases).isEqualTo(adapter1Creations) // at this point they're all off
        assertThat(adapter2.creations).isEqualTo(20) // again, just checking
        assertThat(adapter2.releases).isEqualTo(20) // all of these should be gone too
    }

    @Test
    fun animationTest() = runBlocking {
        animationRule.setAnimationDurationScale(1f)

        withContext(Dispatchers.Main) {
            mRecyclerView.itemAnimator = DefaultItemAnimator()
        }

        val adapter = PoolingContainerTestAdapter(activity, 100, itemHeightPx = 2)
        setAdapter(adapter)
        awaitFrame()

        // All this needs to be on the main thread so that the animation doesn't progress and lead
        // to race conditions.
        withContext(Dispatchers.Main) {
            // Remove all onscreen items
            adapter.items = 50
            adapter.notifyItemRangeRemoved(0, 50)

            // For some reason, one frame isn't enough
            awaitFrame()
            awaitFrame()

            // Animation started: 50 new items created, existing 50 animating out
            // and so they can't be released yet
            assertThat(adapter.releases).isEqualTo(0)
            assertThat(adapter.creations).isEqualTo(100)

            // After the animation, the original 50 are either disposed or in the pool
            mRecyclerView.awaitItemAnimationsComplete()
            // Assumption check: if they're *all* in the pool,
            // this test isn't very useful and we need to make the pool smaller for this test.
            assertThat(adapter.releases).isGreaterThan(0)
            assertThat(adapter.releases).isEqualTo(50 - mRecyclerView.recycledViewPool.size())
            assertThat(adapter.creations).isEqualTo(100)
        }
    }
}

class PoolingContainerTestAdapter(
    val context: Context,
    var items: Int,
    private val itemHeightPx: Int = 1
) :
    RecyclerView.Adapter<PoolingContainerTestAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var creations = 0
    var binds = 0
    var releases = 0

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View(context)
        view.layoutParams =
            RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeightPx)

        creations++
        view.addPoolingContainerListener {
            if (view.isAttachedToWindow) {
                // Enforce detached-from-window constraint
                assertThat(view.isAttachedToWindow).isFalse()
            }
            releases++
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        binds++
    }

    override fun getItemViewType(position: Int): Int {
        return position / 10
    }

    override fun getItemCount(): Int = items
}

private suspend fun RecyclerView.awaitScrollIdle() {
    val rv = this
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val listener = object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        continuation.resume(Unit)
                    }
                }
            }

            rv.addOnScrollListener(listener)

            continuation.invokeOnCancellation { rv.removeOnScrollListener(listener) }

            if (rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                continuation.resume(Unit)
            }
        }
    }
}

private suspend fun RecyclerView.awaitItemAnimationsComplete() {
    val rv = this
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val animator = rv.itemAnimator ?: throw IllegalStateException(
                "awaitItemAnimationsComplete() was called on a RecyclerView with no ItemAnimator." +
                    " This may have been unintended."
            )
            animator.isRunning { continuation.resume(Unit) }
        }
    }
}
