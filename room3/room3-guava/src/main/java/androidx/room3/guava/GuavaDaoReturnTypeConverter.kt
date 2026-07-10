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

package androidx.room3.guava

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.future

/**
 * A [DaoReturnTypeConverter] that allows Room to return `ListenableFuture<T>` from `@Dao`
 * functions.
 */
public class GuavaDaoReturnTypeConverter {
    /**
     * This [convertAsync] function will be called from Room generated code to convert a Room query
     * result to the return type of this function.
     *
     * This converter can be used for both [OperationType.READ] and [OperationType.WRITE]. Note that
     * Room shortcut methods (@Insert, @Update, @Delete) are always treated as
     * [OperationType.WRITE].
     *
     * @param database RoomDatabase instance
     * @param inTransaction True if the DAO is to be executed in a database transaction
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
    public fun <T> convertAsync(
        database: RoomDatabase,
        inTransaction: Boolean,
        executeAndConvert: suspend () -> T,
    ): ListenableFuture<T> {
        return database.getCoroutineScope().future { executeAndConvert.invoke() }
    }
}
