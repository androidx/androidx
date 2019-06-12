/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference

// this function resides in a separate file to avoid parsing API 26 parameter (Duration) in older
// versions of the platform
/**
 * Builds a LiveData that has values yielded from the given [block] that executes on a
 * [LiveDataScope].
 *
 * The [block] starts executing when the returned [LiveData] becomes active ([LiveData.onActive]).
 * If the [LiveData] becomes inactive ([LiveData.onInactive]) while the [block] is executing, it
 * will be cancelled after the [timeout] duration unless the [LiveData] becomes active again
 * before that timeout (to gracefully handle cases like Activity rotation). Any value
 * [LiveDataScope.emit]ed from a cancelled [block] will be ignored.
 *
 * After a cancellation, if the [LiveData] becomes active again, the [block] will be re-executed
 * from the beginning. If you would like to continue the operations based on where it was stopped
 * last, you can use the [LiveDataScope.latestValue] function to get the last
 * [LiveDataScope.emit]ed value.

 * If the [block] completes successfully *or* is cancelled due to reasons other than [LiveData]
 * becoming inactive, it *will not* be re-executed even after [LiveData] goes through active
 * inactive cycle.
 *
 * As a best practice, it is important for the [block] to cooperate in cancellation. See kotlin
 * coroutines documentation for details
 * https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html.
 *
 * ```
 * // a simple LiveData that receives value 3, 3 seconds after being observed for the first time.
 * val data : LiveData<Int> = liveData {
 *     delay(3000)
 *     emit(3)
 * }
 *
 *
 * // a LiveData that fetches a `User` object based on a `userId` and refreshes it every 30 seconds
 * // as long as it is observed
 * val userId : LiveData<String> = ...
 * val user = userId.switchMap { id ->
 *     liveData {
 *       while(true) {
 *         // note that `while(true)` is fine because the `delay(30_000)` below will cooperate in
 *         // cancellation if LiveData is not actively observed anymore
 *         val data = api.fetch(id) // errors are ignored for brevity
 *         emit(data)
 *         delay(30_000)
 *       }
 *     }
 * }
 *
 * // A retrying data fetcher with doubling back-off
 * val user = liveData {
 *     var backOffTime = 1_000
 *     var succeeded = false
 *     while(!succeeded) {
 *         try {
 *             emit(api.fetch(id))
 *             succeeded = true
 *         } catch(ioError : IOException) {
 *             delay(backOffTime)
 *             backOffTime *= minOf(backOffTime * 2, 60_000)
 *         }
 *     }
 * }
 *
 * // a LiveData that tries to load the `User` from local cache first and then tries to fetch
 * // from the server and also yields the updated value
 * val user = liveData {
 *     // dispatch loading first
 *     emit(LOADING(id))
 *     // check local storage
 *     val cached = cache.loadUser(id)
 *     if (cached != null) {
 *         emit(cached)
 *     }
 *     if (cached == null || cached.isStale()) {
 *         val fresh = api.fetch(id) // errors are ignored for brevity
 *         cache.save(fresh)
 *         emit(fresh)
 *     }
 * }
 *
 * // a LiveData that immediately receives a LiveData<User> from the database and yields it as a
 * // source but also tries to back-fill the database from the server
 * val user = liveData {
 *     val fromDb: LiveData<User> = roomDatabase.loadUser(id)
 *     emitSource(fromDb)
 *     val updated = api.fetch(id) // errors are ignored for brevity
 *     // Since we are using Room here, updating the database will update the `fromDb` LiveData
 *     // that was obtained above. See Room's documentation for more details.
 *     // https://developer.android.com/training/data-storage/room/accessing-data#query-observable
 *     roomDatabase.insert(updated)
 * }
 * ```
 *
 * * @param context The CoroutineContext to run the given block in. Defaults to
 * [EmptyCoroutineContext] combined with [Dispatchers.Main].
 * @param timeout The timeout duration before cancelling the block if there are no active observers
 * ([LiveData.hasActiveObservers].
 * @param block The block to run when the [LiveData] has active observers.
 */
@RequiresApi(Build.VERSION_CODES.O)
@UseExperimental(ExperimentalTypeInference::class)
fun <T> liveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration,
    @BuilderInference block: suspend LiveDataScope<T>.() -> Unit
): LiveData<T> = CoroutineLiveData(context, timeout.toMillis(), block)
