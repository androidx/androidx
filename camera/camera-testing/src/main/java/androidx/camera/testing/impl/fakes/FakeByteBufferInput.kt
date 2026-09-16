/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.camera.core.impl.Observable
import androidx.camera.video.internal.BufferProvider
import androidx.camera.video.internal.encoder.Encoder
import androidx.camera.video.internal.encoder.InputBuffer
import com.google.common.util.concurrent.ListenableFuture

public class FakeByteBufferInput(
    private val delegate: FakeBufferProvider =
        FakeBufferProvider(state = BufferProvider.State.INACTIVE)
) : Encoder.ByteBufferInput, Observable<BufferProvider.State> by delegate {
    override fun acquireBuffer(): ListenableFuture<InputBuffer> {
        @Suppress("UNCHECKED_CAST")
        return delegate.acquireBuffer() as ListenableFuture<InputBuffer>
    }

    public fun setState(newState: BufferProvider.State) {
        delegate.setState(newState)
    }
}
