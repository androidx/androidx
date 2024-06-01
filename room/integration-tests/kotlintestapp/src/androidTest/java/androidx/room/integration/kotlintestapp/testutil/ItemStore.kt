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

package androidx.room.integration.kotlintestapp.testutil

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.espresso.base.MainThread
import androidx.testutils.withTestTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** An item store that contains a mock differ for multi-generational pagination */
class ItemStore(private val coroutineScope: CoroutineScope) {
    // We get a new generation each time list changes. This is used to await certain events
    // happening. Each generation have an id that maps to a paging generation.
    // This value is modified only on the main thread.
    private val generation = MutableStateFlow(Generation(0))

    val currentGenerationId
        get() = generation.value.id

    private val asyncDiffer =
        AsyncPagingDataDiffer(
            diffCallback = PagingEntity.DIFF_CALLBACK,
            updateCallback =
                object : ListUpdateCallback {
                    override fun onInserted(position: Int, count: Int) {
                        onDataSetChanged(generation.value.id)
                    }

                    override fun onRemoved(position: Int, count: Int) {
                        onDataSetChanged(generation.value.id)
                    }

                    override fun onMoved(fromPosition: Int, toPosition: Int) {
                        onDataSetChanged(generation.value.id)
                    }

                    override fun onChanged(position: Int, count: Int, payload: Any?) {
                        onDataSetChanged(generation.value.id)
                    }
                }
        )

    init {
        coroutineScope.launch {
            asyncDiffer.loadStateFlow
                .distinctUntilChangedBy { it.source.refresh }
                .map { it.source.refresh }
                .filter { it is LoadState.NotLoading }
                .collect {
                    val current = generation.value
                    generation.value =
                        current.copy(
                            initialLoadCompleted = true,
                        )
                }
        }
    }

    private fun incrementGeneration() {
        val current = generation.value
        generation.value =
            current.copy(
                initialLoadCompleted = false,
                id = current.id + 1,
            )
    }

    fun peekItems() = (0 until asyncDiffer.itemCount).map { asyncDiffer.peek(it) }

    fun get(index: Int): PagingEntity? {
        return asyncDiffer.getItem(index)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun awaitItem(index: Int, timeOutDuration: Long = 3): PagingEntity =
        withTestTimeout(timeOutDuration) {
            generation.mapLatest { asyncDiffer.peek(index) }.filterNotNull().first()
        }

    suspend fun collectFrom(data: PagingData<PagingEntity>) {
        incrementGeneration()
        asyncDiffer.submitData(data)
    }

    @MainThread
    private fun onDataSetChanged(id: Int) {
        coroutineScope.launch(Dispatchers.Main) {
            // deferring this
            yield()
            val curGen = generation.value
            if (curGen.id == id) {
                generation.value =
                    curGen.copy(initialLoadCompleted = true, changeCount = curGen.changeCount + 1)
            }
        }
    }

    suspend fun awaitInitialLoad(timeOutDuration: Long = 3): ItemSnapshotList<PagingEntity> =
        withTestTimeout(timeOutDuration) {
            withContext(Dispatchers.Main) {
                generation.filter { it.initialLoadCompleted }.first()
                asyncDiffer.snapshot()
            }
        }

    suspend fun awaitGeneration(id: Int) = withTestTimeout {
        withContext(Dispatchers.Main) { generation.filter { it.id == id }.first() }
    }
}

/** Holds some metadata about the backing paging list */
data class Generation(
    /** Generation id, incremented each time data source is invalidated */
    val id: Int,
    /** True when the data source completes its initial load */
    val initialLoadCompleted: Boolean = false,
    /** Incremented each time we receive some update events. */
    val changeCount: Int = 0
)
