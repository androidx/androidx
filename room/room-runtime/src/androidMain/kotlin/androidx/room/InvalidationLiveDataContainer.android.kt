/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room

import androidx.lifecycle.LiveData
import androidx.sqlite.SQLiteConnection
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.Callable

/**
 * A helper class that maintains [RoomTrackingLiveData] instances for an [InvalidationTracker].
 *
 * We keep a strong reference to active LiveData instances to avoid garbage collection in case
 * developer does not hold onto the returned LiveData.
 */
internal class InvalidationLiveDataContainer(private val database: RoomDatabase) {
    internal val liveDataSet: MutableSet<LiveData<*>> = Collections.newSetFromMap(IdentityHashMap())

    fun <T> create(
        tableNames: Array<out String>,
        inTransaction: Boolean,
        callableFunction: Callable<T?>
    ): LiveData<T> {
        return RoomTrackingLiveData(
            database = database,
            container = this,
            inTransaction = inTransaction,
            callableFunction = callableFunction,
            lambdaFunction = null,
            tableNames = tableNames
        )
    }

    fun <T> create(
        tableNames: Array<out String>,
        inTransaction: Boolean,
        lambdaFunction: (SQLiteConnection) -> T?
    ): LiveData<T> {
        return RoomTrackingLiveData(
            database = database,
            container = this,
            inTransaction = inTransaction,
            callableFunction = null,
            lambdaFunction = lambdaFunction,
            tableNames = tableNames
        )
    }

    fun onActive(liveData: LiveData<*>) {
        liveDataSet.add(liveData)
    }

    fun onInactive(liveData: LiveData<*>) {
        liveDataSet.remove(liveData)
    }
}
