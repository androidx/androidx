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

package androidx.camera.video.internal

import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture
import androidx.camera.core.impl.utils.futures.Futures.immediateFuture
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.verifyAcceptCallExt
import androidx.camera.video.internal.encoder.FakeInputBuffer
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

@RequiresApi(21)
class FakeBufferProvider(
    private var state: BufferProvider.State = BufferProvider.State.ACTIVE,
    private val bufferFactory: (Int) -> ListenableFuture<FakeInputBuffer>,
) : BufferProvider<FakeInputBuffer> {
    private val submittedBufferCalls = MockConsumer<FakeInputBuffer>()
    private var acquiredBufferNum = 0
    private val observers = mutableMapOf<Observable.Observer<in BufferProvider.State>, Executor>()

    override fun acquireBuffer(): ListenableFuture<FakeInputBuffer> {
        return if (state == BufferProvider.State.ACTIVE) {
            val bufferFuture = bufferFactory.invoke(acquiredBufferNum++)
            bufferFuture.addListener({
                try {
                    val inputBuffer = bufferFuture.get()
                    inputBuffer.terminationFuture.addListener({
                        if (inputBuffer.isSubmitted) {
                            submittedBufferCalls.accept(inputBuffer)
                        }
                    }, directExecutor())
                } catch (e: ExecutionException) {
                    // Ignored.
                }
            }, directExecutor())
            return bufferFuture
        } else {
            immediateFailedFuture(IllegalStateException("Not in ACTIVE state"))
        }
    }

    override fun fetchData(): ListenableFuture<BufferProvider.State> {
        return immediateFuture(state)
    }

    override fun addObserver(
        executor: Executor,
        observer: Observable.Observer<in BufferProvider.State>
    ) {
        observers[observer] = executor
        executor.execute { observer.onNewData(state) }
    }

    override fun removeObserver(observer: Observable.Observer<in BufferProvider.State>) {
        observers.remove(observer)
    }

    fun verifySubmittedBufferCall(
        callTimes: CallTimes,
        timeoutMs: Long = MockConsumer.NO_TIMEOUT,
        inOder: Boolean = false,
        onCompleteBuffers: ((List<FakeInputBuffer>) -> Unit)? = null,
    ) = submittedBufferCalls.verifyAcceptCallExt(
        FakeInputBuffer::class.java,
        inOder,
        timeoutMs,
        callTimes,
        onCompleteBuffers,
    )

    fun setState(newState: BufferProvider.State) {
        if (state == newState) {
            return
        }
        state = newState
        for ((observer, executor) in observers) {
            executor.execute { observer.onNewData(newState) }
        }
    }
}
