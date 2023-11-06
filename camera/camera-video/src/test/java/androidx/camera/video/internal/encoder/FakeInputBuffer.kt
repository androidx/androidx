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

package androidx.camera.video.internal.encoder

import androidx.annotation.RequiresApi
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(21)
class FakeInputBuffer(capacity: Int = 16) : InputBuffer {
    private val byteBuffer = ByteBuffer.allocate(capacity)
    private val terminationFuture = ResolvableFuture.create<Void>()
    private val isTerminated: AtomicBoolean = AtomicBoolean(false)
    private var presentationTimeUs = 0L
    private var isEndOfStream = false
    var isCanceled = false
        private set
    var isSubmitted = false
        private set

    override fun getByteBuffer(): ByteBuffer {
        return byteBuffer
    }

    override fun setPresentationTimeUs(presentationTimeUs: Long) {
        throwIfTerminated()
        this.presentationTimeUs = presentationTimeUs
    }

    fun getPresentationTimeUs() = presentationTimeUs

    override fun setEndOfStream(isEndOfStream: Boolean) {
        throwIfTerminated()
        this.isEndOfStream = isEndOfStream
    }

    fun isEndOfStream() = isEndOfStream

    override fun submit(): Boolean {
        if (!isTerminated.getAndSet(true)) {
            isSubmitted = true
            terminationFuture.set(null)
            return true
        }
        return false
    }

    override fun cancel(): Boolean {
        if (!isTerminated.getAndSet(true)) {
            isCanceled = true
            terminationFuture.set(null)
            return true
        }
        return false
    }

    override fun getTerminationFuture(): ListenableFuture<Void> {
        return terminationFuture
    }

    private fun throwIfTerminated() {
        check(!terminationFuture.isDone)
    }
}
