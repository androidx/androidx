/*
 * Copyright 2022 The Android Open Source Project
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

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnchorTest : BaseRecyclerViewInstrumentationTest() {

    @Test
    fun noAnchoringWhenViewportNotFilled(): Unit = runBlocking(Dispatchers.Main) {
        val mainAdapter = AnchorTestAdapter(0)
        // This simulates a loading spinning added to the end via ConcatAdapter, such as Paging does
        val suffixAdapter = AnchorTestAdapter(1)
        val concatAdapter = ConcatAdapter(mainAdapter, suffixAdapter)

        val context = InstrumentationRegistry.getInstrumentation().context
        val layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
        mRecyclerView = RecyclerView(context)
        mRecyclerView.layoutParams = TestedFrameLayout.FullControlLayoutParams(100, 100)
        mRecyclerView.adapter = concatAdapter
        mRecyclerView.layoutManager = layoutManager
        activity.container.addView(mRecyclerView)
        mRecyclerView.awaitLayout()

        assertThat(layoutManager.findFirstVisibleItemPosition()).isEqualTo(0)
        assertThat(layoutManager.findLastVisibleItemPosition()).isEqualTo(0)

        mainAdapter.numItems = 20
        mRecyclerView.awaitLayout()
        // Before this was fixed, this would anchor to the "spinner" and end up at the
        // bottom of the list (first visible item would be 11)
        assertThat(layoutManager.findFirstVisibleItemPosition()).isEqualTo(0)
        assertThat(layoutManager.findLastVisibleItemPosition()).isEqualTo(9)
    }

    @Test
    fun noAnchoringWhenFillingWrapContentRecyclerView(): Unit = runBlocking(Dispatchers.Main) {
        val mainAdapter = AnchorTestAdapter(0)
        // This simulates a loading spinning added to the end via ConcatAdapter, such as Paging does
        val suffixAdapter = AnchorTestAdapter(1)
        val concatAdapter = ConcatAdapter(mainAdapter, suffixAdapter)

        val context = InstrumentationRegistry.getInstrumentation().context
        val layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        val frame = FrameLayout(context)
        frame.layoutParams = TestedFrameLayout.FullControlLayoutParams(100, 100)
        mRecyclerView = RecyclerView(context)
        mRecyclerView.layoutParams =
            FrameLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT)
        mRecyclerView.adapter = concatAdapter
        mRecyclerView.layoutManager = layoutManager
        frame.addView(mRecyclerView)
        activity.container.addView(frame)
        mRecyclerView.awaitLayout()

        assertThat(layoutManager.findFirstVisibleItemPosition()).isEqualTo(0)
        assertThat(layoutManager.findLastVisibleItemPosition()).isEqualTo(0)

        mainAdapter.numItems = 20
        mRecyclerView.awaitLayout()

        // Before this was fixed, this would anchor to the "spinner" and end up at the
        // bottom of the list (first visible item would be 11)
        assertThat(layoutManager.findFirstVisibleItemPosition()).isEqualTo(0)
        assertThat(layoutManager.findLastVisibleItemPosition()).isEqualTo(9)
    }

    private class AnchorTestAdapter(length: Int) :
        RecyclerView.Adapter<AnchorTestAdapter.ViewHolder>() {
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        var numItems: Int = length
            set(value) {
                val old = field
                val diff = value - old
                if (diff < 0) {
                    TODO("Length reduction not supported")
                }
                field = value
                notifyItemRangeInserted(old, diff)
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val lp = RecyclerView.LayoutParams(10, 10)
            val v = View(parent.context)
            v.layoutParams = lp
            return ViewHolder(v)
        }

        override fun getItemCount(): Int = numItems

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        }
    }
}

private suspend fun View.awaitLayout() {
    suspendCancellableCoroutine { continuation ->
        val runnable = Runnable {
            continuation.resume(Unit)
        }
        continuation.invokeOnCancellation {
            this.removeCallbacks(runnable)
        }
        this.post(runnable)
    }
}