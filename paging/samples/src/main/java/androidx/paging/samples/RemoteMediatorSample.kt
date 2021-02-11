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

@file:Suppress("unused")

package androidx.paging.samples

import androidx.annotation.Sampled
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.samples.shared.ExampleBackendService
import androidx.paging.samples.shared.RemoteKey
import androidx.paging.samples.shared.RoomDb
import androidx.paging.samples.shared.User
import androidx.room.withTransaction
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

private interface ItemDao {
    fun withTransaction(block: () -> Any)
    fun insertAll(items: List<Item>)
    fun removeAll()
    fun lastUpdated(): Long
}

private interface ItemKeyDao {
    fun withTransaction(block: () -> Any)
    fun queryKey(item: Item): String
    fun insertKey(item: Item, key: String): String
    fun removeAll()
}

@Sampled
fun remoteMediatorItemKeyedSample() {
    /**
     * Sample RemoteMediator for a DB + Network based PagingData stream, which triggers network
     * requests to fetch additional items when a user scrolls to the end of the list of items stored
     * in DB.
     *
     * This sample loads a list of [User] items from an item-keyed Retrofit paginated source. This
     * source is "item-keyed" because we're loading the next page using information from the items
     * themselves (the ID param) as a key to fetch more data.
     */
    @OptIn(ExperimentalPagingApi::class)
    class ExampleRemoteMediator(
        private val query: String,
        private val database: RoomDb,
        private val networkService: ExampleBackendService
    ) : RemoteMediator<Int, User>() {
        val userDao = database.userDao()

        override suspend fun initialize(): InitializeAction {
            val cacheTimeout = TimeUnit.HOURS.convert(1, TimeUnit.MILLISECONDS)
            return if (System.currentTimeMillis() - userDao.lastUpdated() >= cacheTimeout) {
                // Cached data is up-to-date, so there is no need to re-fetch from network.
                InitializeAction.SKIP_INITIAL_REFRESH
            } else {
                // Need to refresh cached data from network; returning LAUNCH_INITIAL_REFRESH here
                // will also block RemoteMediator's APPEND and PREPEND from running until REFRESH
                // succeeds.
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
        }

        override suspend fun load(
            loadType: LoadType,
            state: PagingState<Int, User>
        ): MediatorResult {
            return try {
                // The network load method takes an optional `after=<user.id>` parameter. For every
                // page after the first, we pass the last user ID to let it continue from where it
                // left off. For REFRESH, pass `null` to load the first page.
                val loadKey = when (loadType) {
                    LoadType.REFRESH -> null
                    // In this example, we never need to prepend, since REFRESH will always load the
                    // first page in the list. Immediately return, reporting end of pagination.
                    LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                    LoadType.APPEND -> {
                        val lastItem = state.lastItemOrNull()

                        // We must explicitly check if the last item is `null` when appending,
                        // since passing `null` to networkService is only valid for initial load.
                        // If lastItem is `null` it means no items were loaded after the initial
                        // REFRESH and there are no more items to load.
                        if (lastItem == null) {
                            return MediatorResult.Success(endOfPaginationReached = true)
                        }

                        lastItem.id
                    }
                }

                // Suspending network load via Retrofit. This doesn't need to be wrapped in a
                // withContext(Dispatcher.IO) { ... } block since Retrofit's Coroutine CallAdapter
                // dispatches on a worker thread.
                val response = networkService.searchUsers(query = query, after = loadKey)

                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        userDao.deleteByQuery(query)
                    }

                    // Insert new users into database, which invalidates the current
                    // PagingData, allowing Paging to present the updates in the DB.
                    userDao.insertAll(response.users)
                }

                MediatorResult.Success(endOfPaginationReached = response.nextKey == null)
            } catch (e: IOException) {
                MediatorResult.Error(e)
            } catch (e: HttpException) {
                MediatorResult.Error(e)
            }
        }
    }
}

@Sampled
fun remoteMediatorPageKeyedSample() {
    /**
     * Sample RemoteMediator for a DB + Network based PagingData stream, which triggers network
     * requests to fetch additional items when a user scrolls to the end of the list of items stored
     * in DB.
     *
     * This sample loads a list of [User] via Retrofit from a page-keyed network service using
     * [String] tokens to load pages (each response has a next/previous token), and inserts them
     * into database.
     */
    @OptIn(ExperimentalPagingApi::class)
    class ExampleRemoteMediator(
        private val query: String,
        private val database: RoomDb,
        private val networkService: ExampleBackendService
    ) : RemoteMediator<Int, User>() {
        val userDao = database.userDao()
        val remoteKeyDao = database.remoteKeyDao()

        override suspend fun initialize(): InitializeAction {
            val cacheTimeout = TimeUnit.HOURS.convert(1, TimeUnit.MILLISECONDS)
            return if (System.currentTimeMillis() - userDao.lastUpdated() >= cacheTimeout) {
                // Cached data is up-to-date, so there is no need to re-fetch from network.
                InitializeAction.SKIP_INITIAL_REFRESH
            } else {
                // Need to refresh cached data from network; returning LAUNCH_INITIAL_REFRESH here
                // will also block RemoteMediator's APPEND and PREPEND from running until REFRESH
                // succeeds.
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
        }

        override suspend fun load(
            loadType: LoadType,
            state: PagingState<Int, User>
        ): MediatorResult {
            return try {
                // The network load method takes an optional [String] parameter. For every page
                // after the first, we pass the [String] token returned from the previous page to
                // let it continue from where it left off. For REFRESH, pass `null` to load the
                // first page.
                val loadKey = when (loadType) {
                    LoadType.REFRESH -> null
                    // In this example, we never need to prepend, since REFRESH will always load the
                    // first page in the list. Immediately return, reporting end of pagination.
                    LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                    // Query remoteKeyDao for the next RemoteKey.
                    LoadType.APPEND -> {
                        val remoteKey = database.withTransaction {
                            remoteKeyDao.remoteKeyByQuery(query)
                        }

                        // We must explicitly check if the page key is `null` when appending,
                        // since `null` is only valid for initial load. If we receive `null`
                        // for APPEND, that means we have reached the end of pagination and
                        // there are no more items to load.
                        if (remoteKey.nextKey == null) {
                            return MediatorResult.Success(endOfPaginationReached = true)
                        }

                        remoteKey.nextKey
                    }
                }

                // Suspending network load via Retrofit. This doesn't need to be wrapped in a
                // withContext(Dispatcher.IO) { ... } block since Retrofit's Coroutine CallAdapter
                // dispatches on a worker thread.
                val response = networkService.searchUsers(query, loadKey)

                // Store loaded data, and next key in transaction, so that they're always consistent
                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        remoteKeyDao.deleteByQuery(query)
                        userDao.deleteByQuery(query)
                    }

                    // Update RemoteKey for this query.
                    remoteKeyDao.insertOrReplace(RemoteKey(query, response.nextKey))

                    // Insert new users into database, which invalidates the current
                    // PagingData, allowing Paging to present the updates in the DB.
                    userDao.insertAll(response.users)
                }

                MediatorResult.Success(endOfPaginationReached = response.nextKey == null)
            } catch (e: IOException) {
                MediatorResult.Error(e)
            } catch (e: HttpException) {
                MediatorResult.Error(e)
            }
        }
    }
}
