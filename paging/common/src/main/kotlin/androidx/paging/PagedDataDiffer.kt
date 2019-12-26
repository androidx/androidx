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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class PagedDataDiffer<T : Any>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var job: Job? = null
    private var presenter: PagePresenter<T> = PagePresenter.initial()
    private var hintReceiver: ((ViewportHint) -> Unit)? = null

    abstract suspend fun performDiff(previous: NullPaddedList<T>, new: NullPaddedList<T>)

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun connect(flow: Flow<PagedData<T>>, scope: CoroutineScope, callback: PagedList.Callback) {
        job?.cancel()
        job = scope.launch(workerDispatcher) {
            flow.flatMapLatest { pagedData ->
                pagedData.flow.map { event ->
                    Pair(pagedData, event)
                }
            }.collect { pair ->
                withContext(mainDispatcher) {
                    val event = pair.second
                    if (event is PageEvent.Insert && event.loadType == LoadType.REFRESH) {
                        val newPresenter = PagePresenter(event)
                        performDiff(previous = presenter, new = newPresenter)
                        presenter = newPresenter
                        hintReceiver = pair.first.hintReceiver
                    } else {
                        presenter.processEvent(event, callback)
                    }
                }
            }
        }
    }

    operator fun get(index: Int): T? {
        hintReceiver?.invoke(presenter.loadAround(index))
        return presenter.get(index)
    }

    val size: Int
        get() = presenter.size
}