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

package androidx.paging

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class PagingDataDiffer<T : Any>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val collecting = AtomicBoolean(false)
    private var presenter: PagePresenter<T> = PagePresenter.initial()
    private var receiver: UiReceiver? = null

    @Volatile
    private var lastAccessedIndex: Int = 0

    /**
     * @return Transformed result of [lastAccessedIndex] as an index of [newList] using the diff
     * result between [previousList] and [newList]. Null if [newList] or [previousList] lists are
     * empty, where it does not make sense to transform [lastAccessedIndex].
     */
    abstract suspend fun performDiff(
        previousList: NullPaddedList<T>,
        newList: NullPaddedList<T>,
        newLoadStates: Map<LoadType, LoadState>,
        lastAccessedIndex: Int
    ): Int?

    open fun postEvents(): Boolean = false

    @UseExperimental(ExperimentalCoroutinesApi::class)
    suspend fun collectFrom(pagingData: PagingData<T>, callback: PresenterCallback) {
        check(collecting.compareAndSet(false, true)) {
            "Collecting from multiple PagingData concurrently is an illegal operation."
        }

        try {
            pagingData.flow
                .collect { event ->
                    withContext(mainDispatcher) {
                        if (event is PageEvent.Insert && event.loadType == LoadType.REFRESH) {
                            val newPresenter = PagePresenter(event)
                            val transformedLastAccessedIndex = performDiff(
                                previousList = presenter,
                                newList = newPresenter,
                                newLoadStates = event.loadStates,
                                lastAccessedIndex = lastAccessedIndex
                            )
                            presenter = newPresenter
                            receiver = pagingData.receiver

                            // Transform the last loadAround index from the old list to the new list
                            // by passing it through the DiffResult, and pass it forward as a
                            // ViewportHint within the new list to the next generation of Pager.
                            // This ensures prefetch distance for the last ViewportHint from the old
                            // list is respected in the new list, even if invalidation interrupts
                            // the prepend / append load that would have fulfilled it in the old
                            // list.
                            transformedLastAccessedIndex?.let { newIndex ->
                                lastAccessedIndex = newIndex
                                receiver?.addHint(presenter.loadAround(newIndex))
                            }
                        } else {
                            if (postEvents()) {
                                yield()
                            }
                            presenter.processEvent(event, callback)
                        }
                    }
                }
        } finally {
            collecting.set(false)
        }
    }

    operator fun get(index: Int): T? {
        lastAccessedIndex = index
        receiver?.addHint(presenter.loadAround(index))
        return presenter.get(index)
    }

    fun retry() {
        receiver?.retry()
    }

    fun refresh() {
        receiver?.refresh()
    }

    val size: Int
        get() = presenter.size
}
