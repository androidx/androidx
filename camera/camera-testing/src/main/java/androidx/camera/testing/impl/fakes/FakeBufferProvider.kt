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

import androidx.camera.core.impl.MutableStateObservable
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
import kotlinx.atomicfu.atomic

public class FakeBufferProvider(
    state: BufferProvider.State = BufferProvider.State.ACTIVE,
    private val bufferFactory: (Int) -> ListenableFuture<FakeInputBuffer> = {
        immediateFuture(FakeInputBuffer())
    },
) : BufferProvider<FakeInputBuffer> {
    // Back state observation with MutableStateObservable so concurrent addObserver() and
    // setState() calls across threads (e.g. AudioSource on its own executor while an encoder
    // updates state on another thread) cannot enqueue a stale state after a newer one.
    private val stateObservable = MutableStateObservable.withInitialState(state)
    private val submittedBufferCalls = MockConsumer<FakeInputBuffer>()
    private val acquiredBufferNum = atomic(0)

    override fun acquireBuffer(): ListenableFuture<FakeInputBuffer> {
        if (stateObservable.fetchData().get() != BufferProvider.State.ACTIVE) {
            return immediateFailedFuture(IllegalStateException("Not in ACTIVE state"))
        }
        val bufferIndex = acquiredBufferNum.getAndIncrement()
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

    override fun fetchData(): ListenableFuture<BufferProvider.State> = stateObservable.fetchData()

    override fun addObserver(
        executor: Executor,
        observer: Observable.Observer<in BufferProvider.State>,
    ) {
        stateObservable.addObserver(executor, observer)
    }

    override fun removeObserver(observer: Observable.Observer<in BufferProvider.State>) {
        stateObservable.removeObserver(observer)
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
        stateObservable.setState(newState)
    }
}
