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

package androidx.paging.testing

import androidx.annotation.VisibleForTesting
import androidx.paging.CombinedLoadStates
import androidx.paging.DifferCallback
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.NullPaddedList
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
import androidx.paging.testing.ErrorRecovery.RETRY
import androidx.paging.testing.ErrorRecovery.RETURN_CURRENT_SNAPSHOT
import androidx.paging.testing.ErrorRecovery.THROW
import androidx.paging.testing.LoaderCallback.CallbackType.ON_CHANGED
import androidx.paging.testing.LoaderCallback.CallbackType.ON_INSERTED
import androidx.paging.testing.LoaderCallback.CallbackType.ON_REMOVED
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Runs the [SnapshotLoader] load operations that are passed in and returns a List of data
 * that would be presented to the UI after all load operations are complete.
 *
 * @param onError The error recovery strategy when PagingSource returns LoadResult.Error. A lambda
 * that returns an [ErrorRecovery] value. The default strategy is [ErrorRecovery.THROW].
 *
 * @param loadOperations The block containing [SnapshotLoader] load operations.
 */
@VisibleForTesting
public suspend fun <Value : Any> Flow<PagingData<Value>>.asSnapshot(
    onError: LoadErrorHandler = LoadErrorHandler { THROW },
    loadOperations: suspend SnapshotLoader<Value>.() -> @JvmSuppressWildcards Unit = { }
): @JvmSuppressWildcards List<Value> = coroutineScope {

    lateinit var loader: SnapshotLoader<Value>

    val callback = object : DifferCallback {
        override fun onChanged(position: Int, count: Int) {
            loader.onDataSetChanged(
                loader.generations.value,
                LoaderCallback(ON_CHANGED, position, count)
            )
        }
        override fun onInserted(position: Int, count: Int) {
            loader.onDataSetChanged(
                loader.generations.value,
                LoaderCallback(ON_INSERTED, position, count)
            )
        }
        override fun onRemoved(position: Int, count: Int) {
            loader.onDataSetChanged(
                loader.generations.value,
                LoaderCallback(ON_REMOVED, position, count)
            )
        }
    }

    // PagingDataDiffer will collect from coroutineContext instead of main dispatcher
    val differ = object : CompletablePagingDataDiffer<Value>(callback, coroutineContext) {
        override suspend fun presentNewList(
            previousList: NullPaddedList<Value>,
            newList: NullPaddedList<Value>,
            lastAccessedIndex: Int,
            onListPresentable: () -> Unit
        ): Int? {
            onListPresentable()
            /**
             * On new generation, SnapshotLoader needs the latest [ItemSnapshotList]
             * state so that it can initialize lastAccessedIndex to prepend/append from onwards.
             *
             * This initial lastAccessedIndex is necessary because initial load
             * key may not be 0, for example when [Pager].initialKey != 0. It is calculated
             * based on [ItemSnapshotList.placeholdersBefore] + [1/2 initial load size] to match
             * the initial ViewportHint that [PagingDataDiffer.presentNewList] sends on
             * first generation to auto-trigger prefetches on either direction.
             *
             * Any subsequent SnapshotLoader loads are based on the index tracked by
             * [SnapshotLoader] internally.
             */
            val lastLoadedIndex = snapshot().placeholdersBefore + (snapshot().items.size / 2)
            loader.generations.value.lastAccessedIndex.set(lastLoadedIndex)
            return null
        }
    }

    loader = SnapshotLoader(differ, onError)

    /**
     * Launches collection on this [Pager.flow].
     *
     * The collection job is cancelled automatically after [loadOperations] completes.
      */
    val collectPagingData = launch {
        this@asSnapshot.collectLatest {
            incrementGeneration(loader)
            differ.collectFrom(it)
        }
        differ.hasCompleted.value = true
    }

    /**
     * Runs the input [loadOperations].
     *
     * Awaits for initial refresh to complete before invoking [loadOperations]. Automatically
     * cancels the collection on this [Pager.flow] after [loadOperations] completes and Paging
     * is idle.
     */
    try {
        differ.awaitNotLoading(onError)
        loader.loadOperations()
        differ.awaitNotLoading(onError)
    } catch (stub: ReturnSnapshotStub) {
        // we just want to stub and return snapshot early
    } catch (throwable: Throwable) {
        throw throwable
    } finally {
        collectPagingData.cancelAndJoin()
    }

    differ.snapshot().items
}

internal abstract class CompletablePagingDataDiffer<Value : Any>(
    differCallback: DifferCallback,
    mainContext: CoroutineContext,
) : PagingDataDiffer<Value>(differCallback, mainContext) {
    /**
     * Marker that the underlying Flow<PagingData> has completed - e.g., every possible generation
     * of data has been loaded completely.
     */
    val hasCompleted = MutableStateFlow(false)

    val completableLoadStateFlow = loadStateFlow.combine(
        hasCompleted
    ) { loadStates, hasCompleted ->
        if (hasCompleted) {
            CombinedLoadStates(
                refresh = LoadState.NotLoading(true),
                prepend = LoadState.NotLoading(true),
                append = LoadState.NotLoading(true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(true),
                    prepend = LoadState.NotLoading(true),
                    append = LoadState.NotLoading(true)
                )
            )
        } else {
            loadStates
        }
    }
}

/**
 * Awaits until both source and mediator states are NotLoading. We do not care about the state of
 * endOfPaginationReached. Source and mediator states need to be checked individually because
 * the aggregated LoadStates can reflect `NotLoading` when source states are `Loading`.
 *
 * We debounce(1ms) to prevent returning too early if this collected a `NotLoading` from the
 * previous load. Without a way to determine whether the `NotLoading` it collected was from
 * a previous operation or current operation, we debounce 1ms to allow collection on a potential
 * incoming `Loading` state.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
internal suspend fun <Value : Any> CompletablePagingDataDiffer<Value>.awaitNotLoading(
    errorHandler: LoadErrorHandler
) {
    val state = completableLoadStateFlow.filterNotNull().debounce(1).filter {
        it.isIdle() || it.hasError()
    }.firstOrNull()

    if (state != null && state.hasError()) {
        handleLoadError(state, errorHandler)
    }
}

internal fun <Value : Any> PagingDataDiffer<Value>.handleLoadError(
    state: CombinedLoadStates,
    errorHandler: LoadErrorHandler
) {
    val recovery = errorHandler.onError(state)
    when (recovery) {
        THROW -> throw (state.getErrorState()).error
        RETRY -> retry()
        RETURN_CURRENT_SNAPSHOT -> throw ReturnSnapshotStub()
    }
}
private class ReturnSnapshotStub : Exception()

private fun CombinedLoadStates?.isIdle(): Boolean {
    if (this == null) return false
    return source.isIdle() && mediator?.isIdle() ?: true
}

private fun LoadStates.isIdle(): Boolean {
    return refresh is LoadState.NotLoading && append is LoadState.NotLoading &&
        prepend is LoadState.NotLoading
}

private fun CombinedLoadStates?.hasError(): Boolean {
    if (this == null) return false
    return source.hasError() || mediator?.hasError() ?: false
}

private fun LoadStates.hasError(): Boolean {
    return refresh is LoadState.Error || append is LoadState.Error ||
        prepend is LoadState.Error
}

private fun CombinedLoadStates.getErrorState(): LoadState.Error {
    return if (refresh is LoadState.Error) {
        refresh as LoadState.Error
    } else if (append is LoadState.Error) {
        append as LoadState.Error
    } else {
        prepend as LoadState.Error
    }
}

private fun <Value : Any> incrementGeneration(loader: SnapshotLoader<Value>) {
    val currGen = loader.generations.value
    if (currGen.id == loader.generations.value.id) {
        loader.generations.value = Generation(
            id = currGen.id + 1
        )
    }
}
