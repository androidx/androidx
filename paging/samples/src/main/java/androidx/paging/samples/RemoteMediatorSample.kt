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
import retrofit2.HttpException
import java.io.IOException

private interface ItemDao {
    fun withTransaction(block: () -> Any)
    fun insertAll(items: List<Item>)
    fun removeAll()
}

private interface ItemKeyDao {
    fun withTransaction(block: () -> Any)
    fun queryKey(item: Item): String
    fun insertKey(item: Item, key: String): String
    fun removeAll()
}

@Sampled
fun remoteMediatorSample() {
    /**
     * Sample RemoteMediator for a DB + Network based PagingData stream, which triggers network
     * requests to fetch additional items when a user scrolls to the end of the list of items stored
     * in DB.
     *
     * This sample loads `Item`s via Retrofit from a network service using String tokens to load
     * pages (each response has a next/previous token), and inserts them into database.
     */
    @OptIn(ExperimentalPagingApi::class)
    class MyRemoteMediator(
        private val database: ItemDao,
        private val networkService: MyBackendService
    ) : RemoteMediator<String, Item>() {
        override suspend fun load(
            loadType: LoadType,
            state: PagingState<String, Item>
        ): MediatorResult {
            // Get the last accessed item from the current PagingState, which holds all loaded
            // pages from DB.
            val anchorItem = when (loadType) {
                LoadType.REFRESH -> state.anchorPosition?.let { state.closestItemToPosition(it) }
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.last()
            }

            return try {
                // Suspending network load via Retrofit. This doesn't need to be wrapped in a
                // withContext(Dispatcher.IO) { ... } block since Retrofit's Coroutine
                // CallAdapter dispatches on a worker thread.
                val response = networkService.itemsAfter(anchorItem?.id)

                // Insert items into database, which invalidates the current PagingData,
                // allowing Paging to present the updates in the DB.
                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.removeAll()
                    }

                    database.insertAll(response.items)
                }

                // Return a successful result, signaling that more items were fetched from
                // network and that Paging should expect to be invalidated.
                MediatorResult.Success(endOfPaginationReached = response.items.isEmpty())
            } catch (e: IOException) {
                MediatorResult.Error(e)
            } catch (e: HttpException) {
                MediatorResult.Error(e)
            }
        }
    }
}