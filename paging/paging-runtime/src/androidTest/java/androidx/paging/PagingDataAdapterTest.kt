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

package androidx.paging

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestExecutor
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PagingDataAdapterTest {

    @Test
    fun hasStableIds() {
        val pagingDataAdapter =
            object :
                PagingDataAdapter<Int, ViewHolder>(
                    diffCallback =
                        object : DiffUtil.ItemCallback<Int>() {
                            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                                return oldItem == newItem
                            }

                            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                                return oldItem == newItem
                            }
                        }
                ) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    fail("Should never get here")
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    fail("Should never get here")
                }
            }

        assertFailsWith<UnsupportedOperationException> { pagingDataAdapter.setHasStableIds(true) }
    }

    @Test
    fun workerContext() = runTest {
        val workerExecutor = TestExecutor()
        val workerContext: CoroutineContext = workerExecutor.asCoroutineDispatcher()
        val adapter =
            object :
                PagingDataAdapter<Int, ViewHolder>(
                    object : DiffUtil.ItemCallback<Int>() {
                        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                            return oldItem == newItem
                        }

                        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                            return oldItem == newItem
                        }
                    },
                    coroutineContext,
                    workerContext,
                ) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    return object : ViewHolder(TextView(parent.context)) {}
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
            }

        val job = launch {
            adapter.submitData(PagingData.from(listOf(1)))
            adapter.submitData(PagingData.from(listOf(2)))
        }

        // Fast-forward to diff gets scheduled on workerExecutor
        advanceUntilIdle()

        // Check that some work was scheduled on workerExecutor and let everything else run to
        // completion after.
        workerExecutor.autoRun = true
        assertTrue { workerExecutor.executeAll() }
        advanceUntilIdle()

        // Make sure we actually did submit some data and fully present it.
        job.join()
        assertContentEquals(listOf(2), adapter.snapshot().items)
    }
}
