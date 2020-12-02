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
@file:JvmName("RxDataStore")

package androidx.datastore.rxjava2

import androidx.datastore.core.DataStore
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asSingle
import kotlinx.coroutines.rx2.await
import io.reactivex.functions.Function

/**
 * Gets a reactivex.Flowable of the data from DataStore. See [DataStore.data] for more information.
 *
 * Provides efficient, cached (when possible) access to the latest durably persisted state.
 * The flow will always either emit a value or throw an exception encountered when attempting
 * to read from disk. If an exception is encountered, collecting again will attempt to read the
 * data again.
 *
 * Do not layer a cache on top of this API: it will be be impossible to guarantee consistency.
 * Instead, use data.first() to access a single snapshot.
 *
 * The Flowable will complete with an IOException when an exception is encountered when reading
 * data.
 *
 * @return a flow representing the current state of the data
 */
@ExperimentalCoroutinesApi
public fun <T : Any> DataStore<T>.data(): Flowable<T> {
    return this.data.asFlowable()
}

/**
 * See [DataStore.updateData]
 *
 * Updates the data transactionally in an atomic read-modify-write operation. All operations
 * are serialized, and the transform itself is a async so it can perform heavy work
 * such as RPCs.
 *
 * The Single completes when the data has been persisted durably to disk (after which
 * [data] will reflect the update). If the transform or write to disk fails, the
 * transaction is aborted and the returned Single is completed with the error.
 *
 * The transform will be run on the scheduler that DataStore was constructed with.
 *
 * @return the snapshot returned by the transform
 * @throws Exception when thrown by the transform function
 */
@ExperimentalCoroutinesApi
public fun <T : Any> DataStore<T>.updateDataAsync(transform: Function<T, Single<T>>): Single<T> {
    return CoroutineScope(Dispatchers.Unconfined).async {
        this@updateDataAsync.updateData {
            transform.apply(it).await()
        }
    }.asSingle(Dispatchers.Unconfined)
}
