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

package androidx.camera.video.internal.encoder;

import android.media.MediaCodec;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class used to hold a copy of encoded data to prevent occupying the byte buffer of the media
 * codec.
 *
 * <p> To reduce the memory used, the capacity of the copied byte buffer may not equal to its
 * source, only the required size is allocated to put the copied data.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class BufferCopiedEncodedData implements EncodedData {

    private final ByteBuffer mByteBuffer;
    private final MediaCodec.BufferInfo mBufferInfo;
    private final ListenableFuture<Void> mClosedFuture;
    private final CallbackToFutureAdapter.Completer<Void> mClosedCompleter;

    public BufferCopiedEncodedData(@NonNull EncodedData encodedData) {
        mBufferInfo = generateCopiedByteInfo(encodedData);
        mByteBuffer = generateCopiedByteBuffer(encodedData);

        // Prepare close future and completer
        AtomicReference<CallbackToFutureAdapter.Completer<Void>> ref = new AtomicReference<>();
        mClosedFuture = CallbackToFutureAdapter.getFuture(completer -> {
            ref.set(completer);
            return "Data closed";
        });
        mClosedCompleter = Preconditions.checkNotNull(ref.get());
    }

    @NonNull
    private MediaCodec.BufferInfo generateCopiedByteInfo(@NonNull EncodedData encodedData) {
        MediaCodec.BufferInfo bufferInfo = encodedData.getBufferInfo();

        MediaCodec.BufferInfo copiedBufferInfo = new MediaCodec.BufferInfo();
        copiedBufferInfo.set(
                0,
                bufferInfo.size,
                bufferInfo.presentationTimeUs,
                bufferInfo.flags
        );

        return copiedBufferInfo;
    }

    @NonNull
    private ByteBuffer generateCopiedByteBuffer(@NonNull EncodedData encodedData) {
        ByteBuffer byteBuffer = encodedData.getByteBuffer();
        MediaCodec.BufferInfo bufferInfo = encodedData.getBufferInfo();
        byteBuffer.position(bufferInfo.offset);
        byteBuffer.limit(bufferInfo.offset + bufferInfo.size);

        // Copy only the part that contents data
        ByteBuffer copiedByteBuffer = ByteBuffer.allocate(bufferInfo.size);
        copiedByteBuffer.order(byteBuffer.order());
        copiedByteBuffer.put(byteBuffer);
        copiedByteBuffer.flip();

        return copiedByteBuffer;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public ByteBuffer getByteBuffer() {
        return mByteBuffer;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public MediaCodec.BufferInfo getBufferInfo() {
        return mBufferInfo;
    }

    /** {@inheritDoc} */
    @Override
    public long getPresentationTimeUs() {
        return mBufferInfo.presentationTimeUs;
    }

    /** {@inheritDoc} */
    @Override
    public long size() {
        return mBufferInfo.size;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKeyFrame() {
        return (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
    }

    /**
     * No need to call this function. Resources will be released by garbage collection.
     */
    @Override
    public void close() {
        mClosedCompleter.set(null);
    }

    /**
     * No need to call this function. Resources will be released by garbage collection.
     */
    @NonNull
    @Override
    public ListenableFuture<Void> getClosedFuture() {
        return Futures.nonCancellationPropagating(mClosedFuture);
    }
}
