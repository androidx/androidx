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

package androidx.room3.paging.rxjava3

import androidx.paging.rxjava3.RxPagingSource
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery

/**
 * A [DaoReturnTypeConverter] that allows Room to return RxJava3-based [RxPagingSource] types from
 * `@Dao` functions.
 *
 * You can register this converter via annotating a [androidx.room3.Database] or
 * [androidx.room3.Dao] using the annotation [androidx.room3.DaoReturnTypeConverters]:
 * ```
 * @DaoReturnTypeConverters(
 *     RxPagingSourceDaoReturnTypeConverter::class
 * )
 * ```
 */
public class RxPagingSourceDaoReturnTypeConverter {

    /**
     * Converts a Room query into an [RxPagingSource].
     *
     * This converter can be only be used for [OperationType.READ].
     *
     * @param database RoomDatabase instance
     * @param tableNames List of names of the tables of the RoomDatabase
     * @param roomRawQuery The initial [RoomRawQuery] to be executed.
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query. This function takes a [RoomRawQuery] (modified for
     *   limit/offset) and returns a [List] of [T]. This ensures that each page load retrieves the
     *   expected list of items.
     * @return An [RxPagingSource] that emits pages of type [T].
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ])
    public fun <T : Any> convert(
        database: RoomDatabase,
        tableNames: Array<String>,
        roomRawQuery: RoomRawQuery,
        executeAndConvert: suspend (RoomRawQuery) -> List<T>,
    ): RxPagingSource<Int, T> {
        return object :
            LimitOffsetRxPagingSource<T>(
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
