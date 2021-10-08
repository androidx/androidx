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
package androidx.camera.video.internal

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.video.internal.encoder.InputBuffer
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import java.util.concurrent.Executor

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FakeBufferProvider(private val bufferFactory: Callable<InputBuffer>) :
    BufferProvider<InputBuffer> {

    private val lock = Object()
    @GuardedBy("lock")
    private val observers = mutableMapOf<Observable.Observer<in BufferProvider.State>, Executor>()
    @GuardedBy("lock")
    private var state = BufferProvider.State.ACTIVE

    override fun acquireBuffer(): ListenableFuture<InputBuffer> {
        synchronized(lock) {
            return if (state == BufferProvider.State.ACTIVE) {
                Futures.immediateFuture(bufferFactory.call())
            } else {
                Futures.immediateFailedFuture(IllegalStateException("Not in ACTIVE state"))
            }
        }
    }

    override fun fetchData(): ListenableFuture<BufferProvider.State> {
        synchronized(lock) {
            return Futures.immediateFuture(state)
        }
    }

    override fun addObserver(
        executor: Executor,
        observer: Observable.Observer<in BufferProvider.State>
    ) {
        synchronized(observers) {
            observers[observer] = executor
        }
        executor.execute { observer.onNewData(state) }
    }

    override fun removeObserver(observer: Observable.Observer<in BufferProvider.State>) {
        synchronized(lock) {
            observers.remove(observer)
        }
    }

    fun setActive(active: Boolean) {
        val newState = if (active) BufferProvider.State.ACTIVE else BufferProvider.State.INACTIVE
        val localObservers: Map<Observable.Observer<in BufferProvider.State>, Executor>
        synchronized(lock) {
            if (state == newState) {
                return
            }
            state = newState
            localObservers = observers
        }
        for ((observer, executor) in localObservers) {
            executor.execute { observer.onNewData(newState) }
        }
    }
}