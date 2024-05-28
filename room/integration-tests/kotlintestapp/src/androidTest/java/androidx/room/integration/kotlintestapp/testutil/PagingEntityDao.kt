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

import androidx.paging.ListenableFuturePagingSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface PagingEntityDao {
    @Insert fun insert(items: List<PagingEntity>)

    @Insert fun insert(vararg items: PagingEntity)

    @Query("DELETE FROM PagingEntity WHERE id IN (:ids)") fun deleteItems(ids: List<Int>)

    @Query("SELECT * FROM PagingEntity ORDER BY id ASC")
    fun loadItems(): PagingSource<Int, PagingEntity>

    @RawQuery(observedEntities = [PagingEntity::class])
    fun loadItemsRaw(query: SupportSQLiteQuery): PagingSource<Int, PagingEntity>

    @Query("SELECT * FROM PagingEntity ORDER BY id ASC")
    fun loadItemsListenableFuture(): ListenableFuturePagingSource<Int, PagingEntity>

    @RawQuery(observedEntities = [PagingEntity::class])
    fun loadItemsRawListenableFuture(
        query: SupportSQLiteQuery
    ): ListenableFuturePagingSource<Int, PagingEntity>

    @Query("SELECT * FROM PagingEntity ORDER BY id ASC")
    fun loadItemsRx2(): androidx.paging.rxjava2.RxPagingSource<Int, PagingEntity>

    @RawQuery(observedEntities = [PagingEntity::class])
    fun loadItemsRawRx2(
        query: SupportSQLiteQuery
    ): androidx.paging.rxjava2.RxPagingSource<Int, PagingEntity>

    @Query("SELECT * FROM PagingEntity ORDER BY id ASC")
    fun loadItemsRx3(): androidx.paging.rxjava3.RxPagingSource<Int, PagingEntity>

    @RawQuery(observedEntities = [PagingEntity::class])
    fun loadItemsRawRx3(
        query: SupportSQLiteQuery
    ): androidx.paging.rxjava3.RxPagingSource<Int, PagingEntity>
}
