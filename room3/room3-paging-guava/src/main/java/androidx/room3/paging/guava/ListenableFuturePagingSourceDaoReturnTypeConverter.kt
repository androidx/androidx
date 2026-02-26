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

package androidx.room3.paging.guava

import androidx.paging.ListenableFuturePagingSource
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery

/**
 * A [DaoReturnTypeConverter] that allows Room to return Guava-based [ListenableFuturePagingSource]
 * types from `@Dao` functions.
 *
 * You can register this converter via annotating a [Database] or [Dao] using the annotation
 * [DaoReturnTypeConverters]:
 * ```
 * @DaoReturnTypeConverters(
 *     ListenableFuturePagingSourceDaoReturnTypeConverter::class
 * )
 * ```
 */
public class ListenableFuturePagingSourceDaoReturnTypeConverter {

    /**
     * Converts a Room query into a [ListenableFuturePagingSource].
     *
     * @param database RoomDatabase instance.
     * @param tableNames List of names of the tables of the RoomDatabase to observe for
     *   invalidation.
     * @param roomRawQuery The initial [RoomRawQuery] to be executed.
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query. This function takes a [RoomRawQuery] (modified for
     *   limit/offset) and returns a [List] of [T].
     * @return A [ListenableFuturePagingSource] that emits pages of type [T].
     */
    @DaoReturnTypeConverter
    public fun <T : Any> convert(
        database: RoomDatabase,
        tableNames: Array<String>,
        roomRawQuery: RoomRawQuery,
        executeAndConvert: suspend (RoomRawQuery) -> List<T>,
    ): ListenableFuturePagingSource<Int, T> {
        return object :
            LimitOffsetListenableFuturePagingSource<T>(
                sourceQuery = roomRawQuery,
                db = database,
                tables = tableNames,
            ) {
            override suspend fun convertRows(
                limitOffsetQuery: RoomRawQuery,
                itemCount: Int,
            ): List<T> {
                return executeAndConvert(limitOffsetQuery)
            }
        }
    }
}
