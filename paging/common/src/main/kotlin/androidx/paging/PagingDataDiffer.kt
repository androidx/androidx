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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class PagingDataDiffer<T : Any>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val collecting = AtomicBoolean(false)
    private var presenter: PagePresenter<T> = PagePresenter.initial()
    private var receiver: UiReceiver? = null

    abstract suspend fun performDiff(
        previousList: NullPaddedList<T>,
        newList: NullPaddedList<T>,
        newLoadStates: Map<LoadType, LoadState>
    )

    @UseExperimental(ExperimentalCoroutinesApi::class)
    suspend fun collectFrom(pagingData: PagingData<T>, callback: PresenterCallback) {
        check(collecting.compareAndSet(false, true)) {
            "Collecting from multiple PagingData concurrently is an illegal operation."
        }

        try {
            pagingData.flow
                .map { event -> Pair(pagingData, event) }
                .collect { pair ->
                    withContext(mainDispatcher) {
                        val event = pair.second
                        if (event is PageEvent.Insert && event.loadType == LoadType.REFRESH) {
                            val newPresenter = PagePresenter(event)
                            performDiff(
                                previousList = presenter,
                                newList = newPresenter,
                                newLoadStates = event.loadStates
                            )
                            presenter = newPresenter
                            receiver = pair.first.receiver
                        } else {
                            presenter.processEvent(event, callback)
                        }
                    }
                }
        } finally {
            collecting.set(false)
        }
    }

    operator fun get(index: Int): T? {
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