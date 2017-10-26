/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.paging;

import android.arch.lifecycle.LiveData;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

/**
 * Provides a {@code LiveData<PagedList>}, given a means to construct a DataSource.
 * <p>
 * Return type for data-loading system of an application or library to produce a
 * {@code LiveData<PagedList>}, while leaving the details of the paging mechanism up to the
 * consumer.
 * <p>
 * If you're using Room, it can generate a LivePagedListProvider from a query:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
 *     public abstract LivePagedListProvider&lt;Integer, User> usersByLastName();
 * }</pre>
 * In the above sample, {@code Integer} is used because it is the {@code Key} type of
 * {@link TiledDataSource}. Currently, Room can only generate a {@code LIMIT}/{@code OFFSET},
 * position based loader that uses TiledDataSource under the hood, and specifying {@code Integer}
 * here lets you pass an initial loading position as an integer.
 * <p>
 * In the future, Room plans to offer other key types to support paging content with a
 * {@link KeyedDataSource}.
 *
 * @param <Key> Type of input valued used to load data from the DataSource. Must be integer if
 *             you're using TiledDataSource.
 * @param <Value> Data type produced by the DataSource, and held by the PagedLists.
 *
 * @see PagedListAdapter
 * @see DataSource
 * @see PagedList
 *
 * @deprecated To construct a {@code LiveData<PagedList>}, use {@link LivePagedListBuilder}, which
 * provides the same construction capability with more customization, and better defaults. The role
 * of DataSource construction has been separated out to {@link DataSource.Factory} to access or
 * provide a self-invalidating sequence of DataSources. If you were acquiring this from Room, you
 * can switch to having your Dao return a {@link DataSource.Factory} instead, and create a LiveData
 * of PagedList with a {@link LivePagedListBuilder}.
 */
// NOTE: Room 1.0 depends on this class, so it should not be removed
// until Room switches to using DataSource.Factory directly
@Deprecated
public abstract class LivePagedListProvider<Key, Value> implements DataSource.Factory<Key, Value> {

    @Override
    public DataSource<Key, Value> create() {
        return createDataSource();
    }

    /**
     * Construct a new data source to be wrapped in a new PagedList, which will be returned
     * through the LiveData.
     *
     * @return The data source.
     */
    @WorkerThread
    protected abstract DataSource<Key, Value> createDataSource();

    /**
     * Creates a LiveData of PagedLists, given the page size.
     * <p>
     * This LiveData can be passed to a {@link PagedListAdapter} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param initialLoadKey Initial key used to load initial data from the data source.
     * @param pageSize Page size defining how many items are loaded from a data source at a time.
     *                 Recommended to be multiple times the size of item displayed at once.
     *
     * @return The LiveData of PagedLists.
     */
    @AnyThread
    @NonNull
    public LiveData<PagedList<Value>> create(@Nullable Key initialLoadKey, int pageSize) {
        return new LivePagedListBuilder<Key, Value>()
                .setInitialLoadKey(initialLoadKey)
                .setPagingConfig(pageSize)
                .setDataSourceFactory(this)
                .build();
    }

    /**
     * Creates a LiveData of PagedLists, given the PagedList.Config.
     * <p>
     * This LiveData can be passed to a {@link PagedListAdapter} to be displayed with a
     * {@link android.support.v7.widget.RecyclerView}.
     *
     * @param initialLoadKey Initial key to pass to the data source to initialize data with.
     * @param config PagedList.Config to use with created PagedLists. This specifies how the
     *               lists will load data.
     *
     * @return The LiveData of PagedLists.
     */
    @AnyThread
    @NonNull
    public LiveData<PagedList<Value>> create(@Nullable Key initialLoadKey,
            @NonNull PagedList.Config config) {
        return new LivePagedListBuilder<Key, Value>()
                .setInitialLoadKey(initialLoadKey)
                .setPagingConfig(config)
                .setDataSourceFactory(this)
                .build();
    }
}
