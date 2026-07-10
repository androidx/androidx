/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.paging.compose.demos.room3

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.concurrent.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Based value type of all [KeyBasedPagingSource] */
internal interface KeyedValue {
    val key: String
}

/**
 * A Room [PagingSource] for values of type [KeyedValue] that uses a key column as pivot for loading
 * more pages. Instead of using the LIMIT / OFFSET strategy, it relies on a 'scrolling windows'
 * strategy as mentioned in
 * https://web.archive.org/web/20161204225522/http://www.sqlite.org/cvstrac/wiki?p=ScrollingCursor
 */
internal class KeyBasedPagingSource<T : KeyedValue>(
    private val db: RoomDatabase,
    private val tableNames: List<String>,
    private val sourceQuery: RoomRawQuery,
    private val executeAndConvert: suspend (RoomRawQuery) -> List<T>,
) : PagingSource<String, T>() {
    // Set after initialization due to b/492164283
    lateinit var keyColumnName: String

    private val refreshComplete = AtomicBoolean(false)
    private val collectorLatch = CompletableDeferred<Unit>()

    private val invalidationScope = CoroutineScope(Dispatchers.IO)

    init {
        invalidationScope.launch {
            db.invalidationTracker
                .createFlow(*tableNames.toTypedArray(), emitInitialState = true)
                .collect {
                    val initialState = collectorLatch.complete(Unit)
                    if (invalid) {
                        throw CancellationException("PagingSource is invalid")
                    }
                    if (initialState) return@collect
                    if (refreshComplete.get()) {
                        invalidate()
                    }
                }
        }
        registerInvalidatedCallback { invalidationScope.cancel() }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        check(::keyColumnName.isInitialized) { "The key column has not been set." }
        return try {
            collectorLatch.await()
            if (params is LoadParams.Refresh) {
                refreshComplete.compareAndSet(false, true)
            }
            doLoad(params)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun doLoad(params: LoadParams<String>): LoadResult<String, T> {
        val newSql =
            when (params) {
                is LoadParams.Refresh -> {
                    val key = params.key
                    if (key == null) {
                        "SELECT * FROM (${sourceQuery.sql}) " +
                            "ORDER BY $keyColumnName ASC LIMIT ${params.loadSize}"
                    } else {
                        "SELECT * FROM (${sourceQuery.sql}) " +
                            "WHERE $keyColumnName >= '$key' " +
                            "ORDER BY $keyColumnName ASC LIMIT ${params.loadSize}"
                    }
                }
                is LoadParams.Append -> {
                    "SELECT * FROM (${sourceQuery.sql}) " +
                        "WHERE $keyColumnName > '${params.key}' " +
                        "ORDER BY $keyColumnName ASC LIMIT ${params.loadSize}"
                }
                is LoadParams.Prepend -> {
                    "SELECT * FROM (${sourceQuery.sql}) " +
                        "WHERE $keyColumnName < '${params.key}' " +
                        "ORDER BY $keyColumnName DESC LIMIT ${params.loadSize}"
                }
            }
        val newQuery = RoomRawQuery(newSql, sourceQuery.getBindingFunction())
        val data =
            try {
                val queryResult = executeAndConvert(newQuery)
                if (params is LoadParams.Prepend) queryResult.reversed() else queryResult
            } catch (th: Throwable) {
                return LoadResult.Error(th)
            }
        return LoadResult.Page(
            data = data,
            prevKey = data.firstOrNull()?.key,
            nextKey = data.lastOrNull()?.key,
        )
    }

    override fun getRefreshKey(state: PagingState<String, T>): String? {
        val anchorPosition = state.anchorPosition ?: return null
        val indexToStartLoadingAt = maxOf(0, anchorPosition - (state.config.initialLoadSize / 2))
        val item = state.closestItemToPosition(indexToStartLoadingAt)
        return item?.key
    }

    override val jumpingSupported: Boolean
        get() = false
}

/**
 * DAO return type converter for [KeyBasedPagingSource].
 *
 * Note that data object (entity or projection object) must implement [KeyedValue].
 */
internal class KeyBasedPagingSourceDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ])
    fun <T : KeyedValue> convert(
        db: RoomDatabase,
        tableNames: List<String>,
        query: RoomRawQuery,
        executeAndConvert: suspend (RoomRawQuery) -> List<T>,
    ): KeyBasedPagingSource<T> {
        return KeyBasedPagingSource(
            db = db,
            tableNames = tableNames,
            sourceQuery = query,
            executeAndConvert = executeAndConvert,
        )
    }
}
