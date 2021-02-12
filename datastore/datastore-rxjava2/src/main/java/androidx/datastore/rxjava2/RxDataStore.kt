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
package androidx.datastore.rxjava2

import androidx.annotation.RestrictTo
import androidx.datastore.core.DataStore
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asSingle
import kotlinx.coroutines.rx2.await
import io.reactivex.functions.Function
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.rx2.rxCompletable

/**
 * A DataStore that supports RxJava operations on DataStore.
 */
public class RxDataStore<T : Any> private constructor(
    /**
     * The delegate DataStore.
     */
    private val delegateDs: DataStore<T>,
    /**
     * The CoroutineScope that the DataStore is created with. Must contain a Job to allow for
     * cancellation.
     */
    private val scope: CoroutineScope
) : Disposable {

    /**
     * @hide for datastore-preferences-rxjava2 artifact only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        public fun <T : Any> create(delegateDs: DataStore<T>, scope: CoroutineScope):
            RxDataStore<T> {
                return RxDataStore<T>(delegateDs, scope)
            }
    }

    /**
     * Dispose of the DataStore. Wait for the Completable returned by [shutdownComplete] to
     * confirm that the DataStore has been shut down.
     */
    override fun dispose() = scope.coroutineContext.job.cancel()

    /**
     * Returns whether this DataStore is closed
     */
    override fun isDisposed(): Boolean = scope.coroutineContext.job.isActive

    /**
     * Returns a completable that completes when the DataStore is completed. It is not safe to
     * create a new DataStore with the same file name until this has completed.
     */
    public fun shutdownComplete(): Completable =
        rxCompletable(scope.coroutineContext.minusKey(Job)) {
            scope.coroutineContext.job.join()
        }

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
    public fun data(): Flowable<T> {
        return delegateDs.data.asFlowable(scope.coroutineContext)
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
    public fun updateDataAsync(transform: Function<T, Single<T>>): Single<T> {
        return scope.async(SupervisorJob()) {
            delegateDs.updateData {
                transform.apply(it).await()
            }
        }.asSingle(scope.coroutineContext.minusKey(Job))
    }
}
