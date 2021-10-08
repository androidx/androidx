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

@file:JvmName("FlowLiveDataConversions")

package androidx.lifecycle

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Creates a LiveData that has values collected from the origin [Flow].
 *
 * The upstream flow collection starts when the returned [LiveData] becomes active
 * ([LiveData.onActive]).
 * If the [LiveData] becomes inactive ([LiveData.onInactive]) while the flow has not completed,
 * the flow collection will be cancelled after [timeoutInMs] milliseconds unless the [LiveData]
 * becomes active again before that timeout (to gracefully handle cases like Activity rotation).
 *
 * After a cancellation, if the [LiveData] becomes active again, the upstream flow collection will
 * be re-executed.
 *
 * If the upstream flow completes successfully *or* is cancelled due to reasons other than
 * [LiveData] becoming inactive, it *will not* be re-collected even after [LiveData] goes through
 * active inactive cycle.
 *
 * If flow completes with an exception, then exception will be delivered to the
 * [CoroutineExceptionHandler][kotlinx.coroutines.CoroutineExceptionHandler] of provided [context].
 * By default [EmptyCoroutineContext] is used to so an exception will be delivered to main's
 * thread [UncaughtExceptionHandler][Thread.UncaughtExceptionHandler]. If your flow upstream is
 * expected to throw, you can use [catch operator][kotlinx.coroutines.flow.catch] on upstream flow
 * to emit a helpful error object.
 *
 * The [timeoutInMs] can be changed to fit different use cases better, for example increasing it
 * will give more time to flow to complete before being canceled and is good for finite flows
 * that are costly to restart. Otherwise if a flow is cheap to restart decreasing the [timeoutInMs]
 * value will allow to produce less values that aren't consumed by anything.
 *
 * @param context The CoroutineContext to collect the upstream flow in. Defaults to
 * [EmptyCoroutineContext] combined with
 * [Dispatchers.Main.immediate][kotlinx.coroutines.MainCoroutineDispatcher.immediate]
 * @param timeoutInMs The timeout in ms before cancelling the block if there are no active observers
 * ([LiveData.hasActiveObservers]. Defaults to [DEFAULT_TIMEOUT].
 */
@JvmOverloads
public fun <T> Flow<T>.asLiveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = DEFAULT_TIMEOUT
): LiveData<T> = liveData(context, timeoutInMs) {
    collect {
        emit(it)
    }
}

/**
 * Creates a [Flow] containing values dispatched by originating [LiveData]: at the start
 * a flow collector receives the latest value held by LiveData and then observes LiveData updates.
 *
 * When a collection of the returned flow starts the originating [LiveData] becomes
 * [active][LiveData.onActive]. Similarly, when a collection completes [LiveData] becomes
 * [inactive][LiveData.onInactive].
 *
 * BackPressure: the returned flow is conflated. There is no mechanism to suspend an emission by
 * LiveData due to a slow collector, so collector always gets the most recent value emitted.
 */
@OptIn(DelicateCoroutinesApi::class)
public fun <T> LiveData<T>.asFlow(): Flow<T> = flow {
    val channel = Channel<T>(Channel.CONFLATED)
    val observer = Observer<T> {
        channel.trySend(it)
    }
    withContext(Dispatchers.Main.immediate) {
        observeForever(observer)
    }
    try {
        for (value in channel) {
            emit(value)
        }
    } finally {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            removeObserver(observer)
        }
    }
}

/**
 * Creates a LiveData that has values collected from the origin [Flow].
 *
 * The upstream flow collection starts when the returned [LiveData] becomes active
 * ([LiveData.onActive]).
 * If the [LiveData] becomes inactive ([LiveData.onInactive]) while the flow has not completed,
 * the flow collection will be cancelled after [timeout] unless the [LiveData]
 * becomes active again before that timeout (to gracefully handle cases like Activity rotation).
 *
 * After a cancellation, if the [LiveData] becomes active again, the upstream flow collection will
 * be re-executed.
 *
 * If the upstream flow completes successfully *or* is cancelled due to reasons other than
 * [LiveData] becoming inactive, it *will not* be re-collected even after [LiveData] goes through
 * active inactive cycle.
 *
 * If flow completes with an exception, then exception will be delivered to the
 * [CoroutineExceptionHandler][kotlinx.coroutines.CoroutineExceptionHandler] of provided [context].
 * By default [EmptyCoroutineContext] is used to so an exception will be delivered to main's
 * thread [UncaughtExceptionHandler][Thread.UncaughtExceptionHandler]. If your flow upstream is
 * expected to throw, you can use [catch operator][kotlinx.coroutines.flow.catch] on upstream flow
 * to emit a helpful error object.
 *
 * The [timeout] can be changed to fit different use cases better, for example increasing it
 * will give more time to flow to complete before being canceled and is good for finite flows
 * that are costly to restart. Otherwise if a flow is cheap to restart decreasing the [timeout]
 * value will allow to produce less values that aren't consumed by anything.
 *
 * @param context The CoroutineContext to collect the upstream flow in. Defaults to
 * [EmptyCoroutineContext] combined with
 * [Dispatchers.Main.immediate][kotlinx.coroutines.MainCoroutineDispatcher.immediate]
 * @param timeout The timeout in ms before cancelling the block if there are no active observers
 * ([LiveData.hasActiveObservers]. Defaults to [DEFAULT_TIMEOUT].
 */
@RequiresApi(Build.VERSION_CODES.O)
public fun <T> Flow<T>.asLiveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration
): LiveData<T> = asLiveData(context, timeout.toMillis())