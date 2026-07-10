/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.map

/**
 * A [DaoReturnTypeConverter] container that allows Room to return [LiveData] from
 * [androidx.room3.Dao] functions.
 */
public class LiveDataDaoReturnTypeConverter {
    /**
     * This [convert] function will be called from Room generated code to convert a Room query
     * result to the return type of this function.
     *
     * The returned [LiveData] is backed by a [kotlinx.coroutines.flow.Flow] and created via the
     * [asLiveData] API on Room's [kotlinx.coroutines.flow.Flow] returned by the
     * [androidx.room3.InvalidationTracker.createFlow]. It inherits the default timeout behavior (5
     * seconds) where the upstream Flow is cancelled if the LiveData becomes inactive.
     *
     * This converter can be only be used for [OperationType.READ].
     *
     * @param database RoomDatabase instance
     * @param tableNames List of names of the tables of the RoomDatabase
     * @param executeAndConvert A suspend lambda function that invokes the part of the generated
     *   code that executes the query.
     */
    @DaoReturnTypeConverter(operations = [OperationType.READ])
    public fun <T> convert(
        database: RoomDatabase,
        tableNames: Array<String>,
        executeAndConvert: suspend () -> T,
    ): LiveData<T> {
        return database.invalidationTracker
            .createFlow(*tableNames)
            .map { _ -> executeAndConvert.invoke() }
            .asLiveData(database.getQueryContext())
    }
}
