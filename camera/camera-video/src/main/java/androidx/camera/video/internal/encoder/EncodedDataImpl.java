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

package androidx.camera.video.internal.encoder;

import android.media.MediaCodec;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** {@inheritDoc} */
public class EncodedDataImpl implements EncodedData {
    private final MediaCodec mMediaCodec;
    private final MediaCodec.BufferInfo mBufferInfo;
    private final int mBufferIndex;

    private final ByteBuffer mByteBuffer;
    private final ListenableFuture<Void> mClosedFuture;
    private final CallbackToFutureAdapter.Completer<Void> mClosedCompleter;
    private final AtomicBoolean mClosed = new AtomicBoolean(false);

    EncodedDataImpl(@NonNull MediaCodec mediaCodec, int bufferIndex,
            @NonNull MediaCodec.BufferInfo bufferInfo) throws MediaCodec.CodecException {
        mMediaCodec = Preconditions.checkNotNull(mediaCodec);
        mBufferIndex = bufferIndex;
        mByteBuffer = mediaCodec.getOutputBuffer(bufferIndex);
        mBufferInfo = Preconditions.checkNotNull(bufferInfo);
        AtomicReference<CallbackToFutureAdapter.Completer<Void>> ref = new AtomicReference<>();
        mClosedFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    ref.set(completer);
                    return "Data closed";
                });
        mClosedCompleter = Preconditions.checkNotNull(ref.get());
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public ByteBuffer getByteBuffer() {
        throwIfClosed();
        mByteBuffer.position(mBufferInfo.offset);
        mByteBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
        return mByteBuffer;
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public MediaCodec.BufferInfo getBufferInfo() {
        return mBufferInfo;
    }

    /** {@inheritDoc} */
    @Override
    public long getPresentationTimeUs() {
        return mBufferInfo.presentationTimeUs;
    }

    @Override
    public long size() {
        return mBufferInfo.size;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (mClosed.getAndSet(true)) {
            return;
        }
        try {
            mMediaCodec.releaseOutputBuffer(mBufferIndex, false);
        } catch (IllegalStateException e) {
            mClosedCompleter.setException(e);
            return;
        }
        mClosedCompleter.set(null);
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public ListenableFuture<Void> getClosedFuture() {
        return Futures.nonCancellationPropagating(mClosedFuture);
    }

    private void throwIfClosed() {
        if (mClosed.get()) {
            throw new IllegalStateException("encoded data is closed.");
        }
    }
}
