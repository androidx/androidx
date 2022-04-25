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
package androidx.camera.video.internal.encoder

import androidx.annotation.RequiresApi
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FakeInputBuffer : InputBuffer {
    private val byteBuffer = ByteBuffer.allocateDirect(1024)
    private val terminationFuture = ResolvableFuture.create<Void>()

    override fun getByteBuffer(): ByteBuffer {
        throwIfTerminated()
        return byteBuffer
    }

    override fun setPresentationTimeUs(presentationTimeUs: Long) {
        throwIfTerminated()
    }

    override fun setEndOfStream(isEndOfStream: Boolean) {
        throwIfTerminated()
    }

    override fun submit(): Boolean {
        return terminationFuture.set(null)
    }

    override fun cancel(): Boolean {
        return terminationFuture.set(null)
    }

    override fun getTerminationFuture(): ListenableFuture<Void> {
        return terminationFuture
    }

    private fun throwIfTerminated() {
        check(!terminationFuture.isDone)
    }
}