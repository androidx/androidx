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

import androidx.annotation.AnyThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.arch.core.util.Function
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for loading pages of snapshot data into a [PagedList].
 *
 * DataSource is queried to load pages of content into a [PagedList]. A PagedList can grow as
 * it loads more data, but the data loaded cannot be updated. If the underlying data set is
 * modified, a new PagedList / DataSource pair must be created to represent the new data.
 *
 * ### Loading Pages
 *
 * PagedList queries data from its DataSource in response to loading hints. PagedListAdapter
 * calls [PagedList.loadAround] to load content as the user scrolls in a RecyclerView.
 *
 * To control how and when a PagedList queries data from its DataSource, see
 * [PagedList.Config]. The Config object defines things like load sizes and prefetch distance.
 *
 * ### Updating Paged Data
 *
 * A PagedList / DataSource pair are a snapshot of the data set. A new pair of
 * PagedList / DataSource must be created if an update occurs, such as a reorder, insert, delete, or
 * content update occurs. A DataSource must detect that it cannot continue loading its
 * snapshot (for instance, when Database query notices a table being invalidated), and call
 * [invalidate]. Then a new PagedList / DataSource pair would be created to load data from the new
 * state of the Database query.
 *
 * To page in data that doesn't update, you can create a single DataSource, and pass it to a single
 * PagedList. For example, loading from network when the network's paging API doesn't provide
 * updates.
 *
 * To page in data from a source that does provide updates, you can create a [DataSource.Factory],
 * where each DataSource created is invalidated when an update to the data set occurs that makes the
 * current snapshot invalid. For example, when paging a query from the Database, and the table being
 * queried inserts or removes items. You can also use a DataSource.Factory to provide multiple
 * versions of network-paged lists. If reloading all content (e.g. in response to an action like
 * swipe-to-refresh) is required to get a new version of data, you can connect an explicit refresh
 * signal to call [invalidate] on the current [DataSource].
 *
 * If you have more granular update signals, such as a network API signaling an update to a single
 * item in the list, it's recommended to load data from network into memory. Then present that
 * data to the PagedList via a DataSource that wraps an in-memory snapshot. Each time the in-memory
 * copy changes, invalidate the previous DataSource, and a new one wrapping the new state of the
 * snapshot can be created.
 *
 * ### Implementing a DataSource
 *
 * To implement, extend one of the subclasses: [PageKeyedDataSource], [ItemKeyedDataSource], or
 * [PositionalDataSource].
 *
 * Use [PageKeyedDataSource] if pages you load embed keys for loading adjacent pages. For example a
 * network response that returns some items, and a next/previous page links.
 *
 * Use [ItemKeyedDataSource] if you need to use data from item `N-1` to load item
 * `N`. For example, if requesting the backend for the next comments in the list
 * requires the ID or timestamp of the most recent loaded comment, or if querying the next users
 * from a name-sorted database query requires the name and unique ID of the previous.
 *
 * Use [PositionalDataSource] if you can load pages of a requested size at arbitrary
 * positions, and provide a fixed item count. PositionalDataSource supports querying pages at
 * arbitrary positions, so can provide data to PagedLists in arbitrary order. Note that
 * PositionalDataSource is required to respect page size for efficient tiling. If you want to
 * override page size (e.g. when network page size constraints are only known at runtime), use one
 * of the other DataSource classes.
 *
 * Because a `null` item indicates a placeholder in [PagedList], DataSource may not
 * return `null` items in lists that it loads. This is so that users of the PagedList
 * can differentiate unloaded placeholder items from content that has been paged in.
 *
 * @param Key Unique identifier for item loaded from DataSource. Often an integer to represent
 * position in data set. Note - this is distinct from e.g. Room's `<Value>` Value type
 * loaded by the DataSource.
 */
public abstract class DataSource<Key : Any, Value : Any>
// Since we currently rely on implementation details of two implementations, prevent external
// subclassing, except through exposed subclasses.
internal constructor(internal val type: KeyType) {

    @VisibleForTesting
    internal val onInvalidatedCallbacks = CopyOnWriteArrayList<InvalidatedCallback>()

    private val _invalid = AtomicBoolean(false)

    /**
     * @return `true` if the data source is invalid, and can no longer be queried for data.
     */
    public open val isInvalid: Boolean
        @WorkerThread
        get() = _invalid.get()

    /**
     * Factory for DataSources.
     *
     * Data-loading systems of an application or library can implement this interface to allow
     * `LiveData<PagedList>`s to be created. For example, Room can provide a
     * [DataSource.Factory] for a given SQL query:
     *
     * ```
     * @Dao
     * interface UserDao {
     *     @Query("SELECT * FROM user ORDER BY lastName ASC")
     *     public abstract DataSource.Factory<Integer, User> usersByLastName();
     * }
     * ```
     *
     * In the above sample, `Integer` is used because it is the `Key` type of
     * PositionalDataSource. Currently, Room uses the `LIMIT`/`OFFSET` SQL keywords to
     * page a large query with a PositionalDataSource.
     *
     * @param Key Key identifying items in DataSource.
     * @param Value Type of items in the list loaded by the DataSources.
     */
    public abstract class Factory<Key : Any, Value : Any> {
        /**
         * Create a [DataSource].
         *
         * The [DataSource] should invalidate itself if the snapshot is no longer valid. If a
         * [DataSource] becomes invalid, the only way to query more data is to create a new
         * [DataSource] from the Factory.
         *
         * [androidx.paging.LivePagedListBuilder] for example will construct a new PagedList and
         * DataSource when the current DataSource is invalidated, and pass the new PagedList through
         * the `LiveData<PagedList>` to observers.
         *
         * @return the new DataSource.
         */
        public abstract fun create(): DataSource<Key, Value>

        /**
         * Applies the given function to each value emitted by DataSources produced by this Factory.
         *
         * Same as [mapByPage], but operates on individual items.
         *
         * @param function Function that runs on each loaded item, returning items of a potentially
         * new type.
         * @param ToValue Type of items produced by the new [DataSource], from the passed function.
         * @return A new [DataSource.Factory], which transforms items using the given function.
         *
         * @see mapByPage
         * @see DataSource.map
         * @see DataSource.mapByPage
         */
        public open fun <ToValue : Any> map(
            function: Function<Value, ToValue>
        ): Factory<Key, ToValue> {
            return mapByPage(Function { list -> list.map { function.apply(it) } })
        }

        /**
         * Applies the given function to each value emitted by DataSources produced by this Factory.
         *
         * An overload of [map] that accepts a kotlin function type.
         *
         * Same as [mapByPage], but operates on individual items.
         *
         * @param function Function that runs on each loaded item, returning items of a potentially
         * new type.
         * @param ToValue Type of items produced by the new [DataSource], from the passed function.
         * @return A new [DataSource.Factory], which transforms items using the given function.
         *
         * @see mapByPage
         * @see DataSource.map
         * @see DataSource.mapByPage
         */
        @JvmSynthetic // hidden to preserve Java source compat with arch.core.util.Function variant
        public open fun <ToValue : Any> map(function: (Value) -> ToValue): Factory<Key, ToValue> {
            return mapByPage(Function { list -> list.map(function) })
        }

        /**
         * Applies the given function to each value emitted by DataSources produced by this Factory.
         *
         * Same as [map], but allows for batch conversions.
         *
         * @param function Function that runs on each loaded page, returning items of a potentially
         * new type.
         * @param ToValue Type of items produced by the new [DataSource], from the passed function.
         * @return A new [DataSource.Factory], which transforms items using the given function.
         *
         * @see map
         * @see DataSource.map
         * @see DataSource.mapByPage
         */
        public open fun <ToValue : Any> mapByPage(
            function: Function<List<Value>, List<ToValue>>
        ): Factory<Key, ToValue> = object : Factory<Key, ToValue>() {
            override fun create(): DataSource<Key, ToValue> =
                this@Factory.create().mapByPage(function)
        }

        /**
         * Applies the given function to each value emitted by DataSources produced by this Factory.
         *
         * An overload of [mapByPage] that accepts a kotlin function type.
         *
         * Same as [map], but allows for batch conversions.
         *
         * @param function Function that runs on each loaded page, returning items of a potentially
         * new type.
         * @param ToValue Type of items produced by the new [DataSource], from the passed function.
         * @return A new [DataSource.Factory], which transforms items using the given function.
         *
         * @see map
         * @see DataSource.map
         * @see DataSource.mapByPage
         */
        @JvmSynthetic // hidden to preserve Java source compat with arch.core.util.Function variant
        public open fun <ToValue : Any> mapByPage(
            function: (List<Value>) -> List<ToValue>
        ): Factory<Key, ToValue> = mapByPage(Function { function(it) })

        @JvmOverloads
        public fun asPagingSourceFactory(
            fetchDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): () -> PagingSource<Key, Value> = SuspendingPagingSourceFactory(
            delegate = {
                LegacyPagingSource(fetchDispatcher, create())
            },
            dispatcher = fetchDispatcher
        )
    }

    /**
     * Applies the given function to each value emitted by the DataSource.
     *
     * Same as [map], but allows for batch conversions.
     *
     * @param function Function that runs on each loaded page, returning items of a potentially
     * new type.
     * @param ToValue Type of items produced by the new DataSource, from the passed function.
     * @return A new DataSource, which transforms items using the given function.
     *
     * @see map
     * @see DataSource.Factory.map
     * @see DataSource.Factory.mapByPage
     */
    public open fun <ToValue : Any> mapByPage(
        function: Function<List<Value>, List<ToValue>>
    ): DataSource<Key, ToValue> = WrapperDataSource(this, function)

    /**
     * Applies the given function to each value emitted by the DataSource.
     *
     * An overload of [mapByPage] that accepts a kotlin function type.
     *
     * Same as [map], but allows for batch conversions.
     *
     * @param function Function that runs on each loaded page, returning items of a potentially
     * new type.
     * @param ToValue Type of items produced by the new DataSource, from the passed function.
     * @return A new [DataSource], which transforms items using the given function.
     *
     * @see map
     * @see DataSource.Factory.map
     * @see DataSource.Factory.mapByPage
     */
    @JvmSynthetic // hidden to preserve Java source compat with arch.core.util.Function variant
    public open fun <ToValue : Any> mapByPage(
        function: (List<Value>) -> List<ToValue>
    ): DataSource<Key, ToValue> = mapByPage(Function { function(it) })

    /**
     * Applies the given function to each value emitted by the DataSource.
     *
     * Same as [mapByPage], but operates on individual items.
     *
     * @param function Function that runs on each loaded item, returning items of a potentially
     * new type.
     * @param ToValue Type of items produced by the new DataSource, from the passed function.
     * @return A new DataSource, which transforms items using the given function.
     *
     * @see mapByPage
     * @see DataSource.Factory.map
     * @see DataSource.Factory.mapByPage
     */
    public open fun <ToValue : Any> map(
        function: Function<Value, ToValue>
    ): DataSource<Key, ToValue> {
        return mapByPage { list -> list.map { function.apply(it) } }
    }

    /**
     * Applies the given function to each value emitted by the DataSource.
     *
     * An overload of [map] that accepts a kotlin function type.
     *
     * Same as [mapByPage], but operates on individual items.
     *
     * @param function Function that runs on each loaded item, returning items of a potentially
     * new type.
     * @param ToValue Type of items produced by the new DataSource, from the passed function.
     * @return A new DataSource, which transforms items using the given function.
     *
     * @see mapByPage
     * @see DataSource.Factory.map
     *
     */
    @JvmSynthetic // hidden to preserve Java source compat with arch.core.util.Function variant
    public open fun <ToValue : Any> map(
        function: (Value) -> ToValue
    ): DataSource<Key, ToValue> = map(Function { function(it) })

    /**
     * Returns true if the data source guaranteed to produce a contiguous set of items, never
     * producing gaps.
     */
    internal open val isContiguous = true

    internal open val supportsPageDropping = true

    /**
     * Invalidation callback for [DataSource].
     *
     * Used to signal when a [DataSource] a data source has become invalid, and that a new data
     * source is needed to continue loading data.
     */
    public fun interface InvalidatedCallback {
        /**
         * Called when the data backing the list has become invalid. This callback is typically used
         * to signal that a new data source is needed.
         *
         * This callback will be invoked on the thread that calls [invalidate]. It is valid for the
         * data source to invalidate itself during its load methods, or for an outside source to
         * invalidate it.
         */
        @AnyThread
        public fun onInvalidated()
    }

    /**
     * Add a callback to invoke when the DataSource is first invalidated.
     *
     * Once invalidated, a data source will not become valid again.
     *
     * A data source will only invoke its callbacks once - the first time [invalidate] is called, on
     * that thread.
     *
     * @param onInvalidatedCallback The callback, will be invoked on thread that invalidates the
     * [DataSource].
     */
    @AnyThread
    @Suppress("RegistrationName")
    public open fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        onInvalidatedCallbacks.add(onInvalidatedCallback)
    }

    /**
     * Remove a previously added invalidate callback.
     *
     * @param onInvalidatedCallback The previously added callback.
     */
    @AnyThread
    @Suppress("RegistrationName")
    public open fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        onInvalidatedCallbacks.remove(onInvalidatedCallback)
    }

    /**
     * Signal the data source to stop loading, and notify its callback.
     *
     * If invalidate has already been called, this method does nothing.
     */
    @AnyThread
    public open fun invalidate() {
        if (_invalid.compareAndSet(false, true)) {
            onInvalidatedCallbacks.forEach { it.onInvalidated() }
        }
    }

    /**
     * @param K Type of the key used to query the [DataSource].
     * @property key Can be `null` for init, otherwise non-null
     */
    internal class Params<K : Any> internal constructor(
        internal val type: LoadType,
        val key: K?,
        val initialLoadSize: Int,
        val placeholdersEnabled: Boolean,
        val pageSize: Int
    ) {
        init {
            if (type != LoadType.REFRESH && key == null) {
                throw IllegalArgumentException("Key must be non-null for prepend/append")
            }
        }
    }

    /**
     * @param Value Type of the data produced by a [DataSource].
     */
    internal class BaseResult<Value : Any> internal constructor(
        @JvmField
        val data: List<Value>,
        val prevKey: Any?,
        val nextKey: Any?,
        val itemsBefore: Int = COUNT_UNDEFINED,
        val itemsAfter: Int = COUNT_UNDEFINED
    ) {
        init {
            if (itemsBefore < 0 && itemsBefore != COUNT_UNDEFINED) {
                throw IllegalArgumentException("Position must be non-negative")
            }
            if (data.isEmpty() && (itemsBefore > 0 || itemsAfter > 0)) {
                // If non-initial, itemsBefore, itemsAfter are COUNT_UNDEFINED
                throw IllegalArgumentException(
                    "Initial result cannot be empty if items are present in data set."
                )
            }
            if (itemsAfter < 0 && itemsAfter != COUNT_UNDEFINED) {
                throw IllegalArgumentException(
                    "List size + position too large, last item in list beyond totalCount."
                )
            }
        }

        /**
         * While it may seem unnecessary to do this validation now that tiling is gone, we do
         * this to ensure consistency with 2.1, and to ensure all loadRanges have the same page
         * size.
         */
        internal fun validateForInitialTiling(pageSize: Int) {
            if (itemsBefore == COUNT_UNDEFINED || itemsAfter == COUNT_UNDEFINED) {
                throw IllegalStateException(
                    "Placeholders requested, but totalCount not provided. Please call the" +
                        " three-parameter onResult method, or disable placeholders in the" +
                        " PagedList.Config"
                )
            }

            if (itemsAfter > 0 && data.size % pageSize != 0) {
                val totalCount = itemsBefore + data.size + itemsAfter
                throw IllegalArgumentException(
                    "PositionalDataSource requires initial load size to be a multiple of page" +
                        " size to support internal tiling. loadSize ${data.size}, position" +
                        " $itemsBefore, totalCount $totalCount, pageSize $pageSize"
                )
            }
            if (itemsBefore % pageSize != 0) {
                throw IllegalArgumentException(
                    "Initial load must be pageSize aligned.Position = $itemsBefore, pageSize =" +
                        " $pageSize"
                )
            }
        }

        override fun equals(other: Any?) = when (other) {
            is BaseResult<*> ->
                data == other.data &&
                    prevKey == other.prevKey &&
                    nextKey == other.nextKey &&
                    itemsBefore == other.itemsBefore &&
                    itemsAfter == other.itemsAfter
            else -> false
        }

        internal companion object {
            internal fun <T : Any> empty() = BaseResult(emptyList<T>(), null, null, 0, 0)

            internal fun <ToValue : Any, Value : Any> convert(
                result: BaseResult<ToValue>,
                function: Function<List<ToValue>, List<Value>>
            ) = BaseResult(
                data = convert(function, result.data),
                prevKey = result.prevKey,
                nextKey = result.nextKey,
                itemsBefore = result.itemsBefore,
                itemsAfter = result.itemsAfter
            )
        }
    }

    internal enum class KeyType {
        POSITIONAL,
        PAGE_KEYED,
        ITEM_KEYED
    }

    internal abstract suspend fun load(params: Params<Key>): BaseResult<Value>

    internal abstract fun getKeyInternal(item: Value): Key

    internal companion object {
        internal fun <A, B> convert(
            function: Function<List<A>, List<B>>,
            source: List<A>
        ): List<B> {
            val dest = function.apply(source)
            if (dest.size != source.size) {
                throw IllegalStateException(
                    "Invalid Function $function changed return size. This is not supported."
                )
            }
            return dest
        }
    }
}
