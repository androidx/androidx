/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes

import androidx.annotation.GuardedBy
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture
import androidx.camera.core.impl.utils.futures.Futures.immediateFuture
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.verifyAcceptCallExt
import androidx.camera.video.internal.BufferProvider
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

public class FakeBufferProvider(
    @GuardedBy("lock") private var state: BufferProvider.State = BufferProvider.State.ACTIVE,
    private val bufferFactory: (Int) -> ListenableFuture<FakeInputBuffer> = {
        immediateFuture(FakeInputBuffer())
    },
) : BufferProvider<FakeInputBuffer> {
    // The BufferProvider can be accessed from multiple threads, e.g. AudioSource adds its observer
    // on its own executor while an encoder updates the state on another thread.
    private val lock = Any()
    private val submittedBufferCalls = MockConsumer<FakeInputBuffer>()
    @GuardedBy("lock") private var acquiredBufferNum = 0
    @GuardedBy("lock")
    private val observers = mutableMapOf<Observable.Observer<in BufferProvider.State>, Executor>()

    override fun acquireBuffer(): ListenableFuture<FakeInputBuffer> {
        val bufferIndex =
            synchronized(lock) {
                if (state != BufferProvider.State.ACTIVE) {
                    return immediateFailedFuture(IllegalStateException("Not in ACTIVE state"))
                }
                acquiredBufferNum++
            }
        val bufferFuture = bufferFactory.invoke(bufferIndex)
        bufferFuture.addListener(
            {
                try {
                    val inputBuffer = bufferFuture.get()
                    inputBuffer.terminationFuture.addListener(
                        {
                            if (inputBuffer.isSubmitted) {
                                submittedBufferCalls.accept(inputBuffer)
                            }
                        },
                        directExecutor(),
                    )
                } catch (_: ExecutionException) {
                    // Ignored.
                }
            },
            directExecutor(),
        )
        return bufferFuture
    }

    override fun fetchData(): ListenableFuture<BufferProvider.State> {
        return immediateFuture(synchronized(lock) { state })
    }

    override fun addObserver(
        executor: Executor,
        observer: Observable.Observer<in BufferProvider.State>,
    ) {
        val currentState =
            synchronized(lock) {
                observers[observer] = executor
                state
            }
        executor.execute { observer.onNewData(currentState) }
    }

    override fun removeObserver(observer: Observable.Observer<in BufferProvider.State>) {
        synchronized(lock) { observers.remove(observer) }
    }

    public fun verifySubmittedBufferCall(
        callTimes: CallTimes,
        timeoutMs: Long = MockConsumer.NO_TIMEOUT,
        inOder: Boolean = false,
        onCompleteBuffers: ((List<FakeInputBuffer>) -> Unit)? = null,
    ): Unit =
        submittedBufferCalls.verifyAcceptCallExt(
            FakeInputBuffer::class.java,
            inOder,
            timeoutMs,
            callTimes,
            onCompleteBuffers,
        )

    public fun setState(newState: BufferProvider.State) {
        // Notify a snapshot of the observers outside the lock, so that an observer added or
        // removed concurrently doesn't break the iteration.
        val observersToNotify =
            synchronized(lock) {
                if (state == newState) {
                    return
                }
                state = newState
                observers.toList()
            }
        for ((observer, executor) in observersToNotify) {
            executor.execute { observer.onNewData(newState) }
        }
    }
}
