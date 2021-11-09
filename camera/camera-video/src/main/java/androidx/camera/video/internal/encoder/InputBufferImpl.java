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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class InputBufferImpl implements InputBuffer {
    private final MediaCodec mMediaCodec;
    private final int mBufferIndex;
    private final ByteBuffer mByteBuffer;
    private final ListenableFuture<Void> mTerminationFuture;
    private final CallbackToFutureAdapter.Completer<Void> mTerminationCompleter;
    private final AtomicBoolean mTerminated = new AtomicBoolean(false);
    private long mPresentationTimeUs = 0L;
    private boolean mIsEndOfStream = false;

    InputBufferImpl(@NonNull MediaCodec mediaCodec, @IntRange(from = 0) int bufferIndex)
            throws MediaCodec.CodecException {
        mMediaCodec = Preconditions.checkNotNull(mediaCodec);
        mBufferIndex = Preconditions.checkArgumentNonnegative(bufferIndex);
        mByteBuffer = mediaCodec.getInputBuffer(bufferIndex);
        AtomicReference<Completer<Void>> ref = new AtomicReference<>();
        mTerminationFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    ref.set(completer);
                    return "Terminate InputBuffer";
                });
        mTerminationCompleter = Preconditions.checkNotNull(ref.get());
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public ByteBuffer getByteBuffer() {
        throwIfTerminated();
        return mByteBuffer;
    }

    /** {@inheritDoc} */
    @Override
    public void setPresentationTimeUs(long presentationTimeUs) {
        throwIfTerminated();
        Preconditions.checkArgument(presentationTimeUs >= 0L);
        mPresentationTimeUs = presentationTimeUs;
    }

    /** {@inheritDoc} */
    @Override
    public void setEndOfStream(boolean endOfStream) {
        throwIfTerminated();
        mIsEndOfStream = endOfStream;
    }

    /** {@inheritDoc} */
    @Override
    public boolean submit() {
        if (mTerminated.getAndSet(true)) {
            return false;
        }
        try {
            mMediaCodec.queueInputBuffer(mBufferIndex,
                    mByteBuffer.position(),
                    mByteBuffer.limit(),
                    mPresentationTimeUs,
                    mIsEndOfStream ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            mTerminationCompleter.set(null);
            return true;
        } catch (IllegalStateException e) {
            mTerminationCompleter.setException(e);
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean cancel() {
        if (mTerminated.getAndSet(true)) {
            return false;
        }
        try {
            mMediaCodec.queueInputBuffer(mBufferIndex, 0, 0, 0, 0);
            mTerminationCompleter.set(null);
        } catch (IllegalStateException e) {
            mTerminationCompleter.setException(e);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public ListenableFuture<Void> getTerminationFuture() {
        return Futures.nonCancellationPropagating(mTerminationFuture);
    }

    private void throwIfTerminated() {
        if (mTerminated.get()) {
            throw new IllegalStateException("The buffer is submitted or canceled.");
        }
    }
}
