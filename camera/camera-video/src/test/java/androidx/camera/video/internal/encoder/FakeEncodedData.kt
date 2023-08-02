/*
 * Copyright 2022 The Android Open Source Project
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

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FakeEncodedData(
    private val byteBuffer: ByteBuffer,
    private val bufferInfo: BufferInfo
) : EncodedData {
    private val terminationFuture = ResolvableFuture.create<Void>()

    override fun getByteBuffer(): ByteBuffer {
        return byteBuffer
    }

    override fun getBufferInfo(): BufferInfo {
        return bufferInfo
    }

    override fun getPresentationTimeUs(): Long {
        return bufferInfo.presentationTimeUs
    }

    override fun size(): Long {
        return bufferInfo.size.toLong()
    }

    override fun isKeyFrame(): Boolean {
        return (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
    }

    override fun close() {
        terminationFuture.set(null)
    }

    override fun getClosedFuture(): ListenableFuture<Void> {
        return terminationFuture
    }
}
