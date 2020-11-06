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

import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.CONFIGURED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PAUSED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_RELEASE;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_START;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_START_PAUSED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.RELEASED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.STARTED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.STOPPING;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * The encoder implementation.
 *
 * <p>An encoder could be either a video encoder or an audio encoder.
 */
public class EncoderImpl implements Encoder {
    private static final String TAG = "Encoder";

    enum InternalState {
        /**
         * The initial state.
         */
        CONFIGURED,

        /**
         * The state is when encoder is in {@link InternalState#CONFIGURED} state and {@link #start}
         * is called.
         */
        STARTED,

        /**
         * The state is when encoder is in {@link InternalState#STARTED} state and {@link #pause}
         * is called.
         */
        PAUSED,

        /**
         * The state is when encoder is in {@link InternalState#STARTED} state and {@link #stop} is
         * called.
         */
        STOPPING,

        /**
         * The state is when the encoder is in {@link InternalState#STOPPING} state and a
         * {@link #start} is called. It is an extension of {@link InternalState#STOPPING}.
         */
        PENDING_START,

        /**
         * The state is when the encoder is in {@link InternalState#STOPPING} state, then
         * {@link #start} and {@link #pause} is called. It is an extension of
         * {@link InternalState#STOPPING}.
         */
        PENDING_START_PAUSED,

        /**
         * The state is when the encoder is in {@link InternalState#STOPPING} state and a
         * {@link #release} is called. It is an extension of {@link InternalState#STOPPING}.
         */
        PENDING_RELEASE,

        /** The state is when the encoder is released. */
        RELEASED,
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mLock = new Object();
    private final MediaFormat mMediaFormat;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    final MediaCodec mMediaCodec;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final EncoderInput mEncoderInput;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    EncoderCallback mEncoderCallback = EncoderCallback.EMPTY;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    Executor mEncoderCallbackExecutor = CameraXExecutors.mainThreadExecutor();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    InternalState mState;

    /**
     * Creates the encoder with a {@link EncoderConfig}
     *
     * @param executor the executor suitable for background task
     * @param encoderConfig the encoder config
     * @throws InvalidConfigException when the encoder cannot be configured.
     */
    public EncoderImpl(@NonNull Executor executor, @NonNull EncoderConfig encoderConfig)
            throws InvalidConfigException {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(encoderConfig);

        mExecutor = CameraXExecutors.newSequentialExecutor(executor);

        if (encoderConfig instanceof AudioEncoderConfig) {
            mEncoderInput = new ByteBufferInput();
        } else if (encoderConfig instanceof VideoEncoderConfig) {
            mEncoderInput = new SurfaceInput();
        } else {
            throw new InvalidConfigException("Unknown encoder config type");
        }

        try {
            mMediaCodec = MediaCodec.createEncoderByType(encoderConfig.getMimeType());
        } catch (IOException e) {
            throw new InvalidConfigException(
                    "Unsupported mime type: " + encoderConfig.getMimeType(), e);
        }

        mMediaFormat = encoderConfig.toMediaFormat();

        try {
            reset();
        } catch (MediaCodec.CodecException e) {
            throw new InvalidConfigException(e);
        }

        setState(CONFIGURED);
    }

    @SuppressWarnings("GuardedBy")
    // It complains SurfaceInput#resetSurface and ByteBufferInput#clearFreeBuffers don't hold mLock
    @GuardedBy("mLock")
    private void reset() {
        mMediaCodec.reset();
        mMediaCodec.setCallback(new MediaCodecCallback());
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        if (mEncoderInput instanceof SurfaceInput) {
            ((SurfaceInput) mEncoderInput).resetSurface();
        } else if (mEncoderInput instanceof ByteBufferInput) {
            ((ByteBufferInput) mEncoderInput).clearFreeBuffers();
        }
    }

    /** Gets the {@link EncoderInput} of the encoder */
    @Override
    @NonNull
    public EncoderInput getInput() {
        return mEncoderInput;
    }

    /**
     * Starts the encoder.
     *
     * <p>If the encoder is not started yet, it will first trigger
     * {@link EncoderCallback#onEncodeStart}. Then continually invoke the
     * {@link EncoderCallback#onEncodedData} callback until the encoder is paused, stopped or
     * released. It can call {@link #pause} to pause the encoding after started. If the encoder is
     * in paused state, then calling this method will resume the encoding.
     */
    @Override
    public void start() {
        synchronized (mLock) {
            switch (mState) {
                case CONFIGURED:
                    try {
                        mMediaCodec.start();
                    } catch (MediaCodec.CodecException e) {
                        handleEncodeError(e);
                        return;
                    }
                    setState(STARTED);
                    break;
                case PAUSED:
                    if (mEncoderInput instanceof SurfaceInput) {
                        updatePauseToMediaCodec(false);
                    }
                    setState(STARTED);
                    break;
                case STARTED:
                case PENDING_START:
                    // Do nothing
                    break;
                case STOPPING:
                case PENDING_START_PAUSED:
                    setState(PENDING_START);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    throw new IllegalStateException("Encoder is released");
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        }
    }

    /**
     * Stops the encoder.
     *
     * <p>It will trigger {@link EncoderCallback#onEncodeStop} after the last encoded data. It can
     * call {@link #start} to start again.
     */
    @SuppressWarnings("GuardedBy")
    // It complains ByteBufferInput#signalEndOfInputStream doesn't hold mLock
    @Override
    public void stop() {
        synchronized (mLock) {
            switch (mState) {
                case CONFIGURED:
                case STOPPING:
                    // Do nothing
                    break;
                case STARTED:
                case PAUSED:
                    setState(STOPPING);
                    if (mEncoderInput instanceof ByteBufferInput) {
                        ((ByteBufferInput) mEncoderInput).signalEndOfInputStream();
                    } else if (mEncoderInput instanceof SurfaceInput) {
                        try {
                            mMediaCodec.signalEndOfInputStream();
                        } catch (MediaCodec.CodecException e) {
                            handleEncodeError(e);
                        }
                    }
                    break;
                case PENDING_START:
                case PENDING_START_PAUSED:
                    setState(CONFIGURED);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    throw new IllegalStateException("Encoder is released");
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        }
    }

    /**
     * Pauses the encoder.
     *
     * <p>{@link #pause} only work between {@link #start} and {@link #stop}. Once the encoder is
     * paused, it will drop the input data until {@link #start} is invoked again.
     */
    @Override
    public void pause() {
        synchronized (mLock) {
            switch (mState) {
                case CONFIGURED:
                case PAUSED:
                case STOPPING:
                case PENDING_START_PAUSED:
                    // Do nothing
                    break;
                case PENDING_START:
                    setState(PENDING_START_PAUSED);
                    break;
                case STARTED:
                    if (mEncoderInput instanceof SurfaceInput) {
                        updatePauseToMediaCodec(true);
                    }
                    setState(PAUSED);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    throw new IllegalStateException("Encoder is released");
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        }
    }

    /**
     * Releases the encoder.
     *
     * <p>Once the encoder is released, it cannot be used anymore. Any other method call after
     * the encoder is released will get {@link IllegalStateException}. If it is in encoding, make
     * sure call {@link #stop} before {@link #release} to normally end the stream, or it may get
     * uncertain result if call {@link #release} while encoding.
     */
    @Override
    public void release() {
        synchronized (mLock) {
            switch (mState) {
                case CONFIGURED:
                case STARTED:
                case PAUSED:
                    mMediaCodec.release();
                    setState(RELEASED);
                    break;
                case STOPPING:
                case PENDING_START:
                case PENDING_START_PAUSED:
                    setState(PENDING_RELEASE);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    // Do nothing
                    break;
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        }
    }

    /**
     * Sets callback to encoder.
     *
     * @param encoderCallback the encoder callback
     * @param executor the callback executor
     */
    @Override
    public void setEncoderCallback(
            @NonNull EncoderCallback encoderCallback,
            @NonNull Executor executor) {
        synchronized (mLock) {
            mEncoderCallback = encoderCallback;
            mEncoderCallbackExecutor = executor;
        }
    }

    @GuardedBy("mLock")
    private void setState(InternalState state) {
        Logger.d(TAG, "Transitioning encoder internal state: " + mState + " --> " + state);
        mState = state;
    }

    @GuardedBy("mLock")
    private void updatePauseToMediaCodec(boolean paused) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(MediaCodec.PARAMETER_KEY_SUSPEND, paused);
        mMediaCodec.setParameters(bundle);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    void handleEncodeError(@NonNull MediaCodec.CodecException e) {
        handleEncodeError(EncodeException.ERROR_CODEC, e.getMessage(), e);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    void handleEncodeError(@EncodeException.ErrorType int error, @Nullable String message,
            @Nullable Throwable throwable) {
        EncoderCallback encoderCallback = mEncoderCallback;
        try {
            mEncoderCallbackExecutor.execute(() -> encoderCallback.onEncodeError(
                    new EncodeException(error, message, throwable)));
        } catch (RejectedExecutionException re) {
            Logger.e(TAG, "Unable to post to the supplied executor.", re);
        }
        mMediaCodec.stop();
        handleStopped();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    void handleStopped() {
        if (mState == PENDING_RELEASE) {
            mMediaCodec.release();
            setState(RELEASED);
        } else {
            InternalState oldState = mState;
            reset();
            setState(CONFIGURED);
            if (oldState == PENDING_START || oldState == PENDING_START_PAUSED) {
                start();
                if (oldState == PENDING_START_PAUSED && mState == STARTED) {
                    pause();
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static long generatePresentationTimeUs() {
        return System.nanoTime() / 1000L;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class MediaCodecCallback extends MediaCodec.Callback {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        @GuardedBy("mLock")
        final Set<EncodedDataImpl> mEncodedDataSet = new HashSet<>();

        @GuardedBy("mLock")
        private boolean mHasFirstData = false;

        @SuppressWarnings("GuardedBy")
        // It complains ByteBufferInput#putFreeBufferIndex doesn't hold mLock
        @Override
        public void onInputBufferAvailable(MediaCodec mediaCodec, int index) {
            synchronized (mLock) {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        if (mEncoderInput instanceof ByteBufferInput) {
                            ((ByteBufferInput) mEncoderInput).putFreeBufferIndex(index);
                        }
                        break;
                    case CONFIGURED:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index,
                @NonNull MediaCodec.BufferInfo bufferInfo) {
            synchronized (mLock) {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        final EncoderCallback encoderCallback = mEncoderCallback;
                        final Executor executor = mEncoderCallbackExecutor;

                        // Handle start of stream
                        if (!mHasFirstData) {
                            mHasFirstData = true;
                            try {
                                executor.execute(encoderCallback::onEncodeStart);
                            } catch (RejectedExecutionException e) {
                                Logger.e(TAG, "Unable to post to the supplied executor.", e);
                            }
                        }

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was sent out by MediaFormat when getting
                            // onOutputFormatChanged(). Ignore it.
                            bufferInfo.size = 0;
                        }

                        if (bufferInfo.size > 0) {
                            if (mEncoderInput instanceof SurfaceInput) {
                                // TODO(b/171972677): Overriding the video presentation time here
                                //  may not be a right thing. It needs to do a translation to a
                                //  common clock in order to keep video/audio in sync.
                                bufferInfo.presentationTimeUs = generatePresentationTimeUs();
                            }
                            EncodedDataImpl encodedData;
                            try {
                                encodedData = new EncodedDataImpl(mediaCodec, index, bufferInfo);
                            } catch (MediaCodec.CodecException e) {
                                handleEncodeError(e);
                                return;
                            }
                            // Propagate data
                            mEncodedDataSet.add(encodedData);
                            Futures.addCallback(encodedData.getClosedFuture(),
                                    new FutureCallback<Void>() {
                                        @Override
                                        public void onSuccess(@Nullable Void result) {
                                            synchronized (mLock) {
                                                mEncodedDataSet.remove(encodedData);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            synchronized (mLock) {
                                                mEncodedDataSet.remove(encodedData);
                                                if (t instanceof MediaCodec.CodecException) {
                                                    handleEncodeError(
                                                            (MediaCodec.CodecException) t);
                                                } else {
                                                    handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                                            t.getMessage(), t);
                                                }
                                            }
                                        }
                                    }, CameraXExecutors.directExecutor());
                            try {
                                executor.execute(() -> encoderCallback.onEncodedData(encodedData));
                            } catch (RejectedExecutionException e) {
                                Logger.e(TAG, "Unable to post to the supplied executor.", e);
                                encodedData.close();
                            }
                        } else {
                            try {
                                mMediaCodec.releaseOutputBuffer(index, false);
                            } catch (MediaCodec.CodecException e) {
                                handleEncodeError(e);
                                return;
                            }
                        }

                        // Handle end of stream
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            // Wait for all data closed
                            List<ListenableFuture<Void>> waitForCloseFutures = new ArrayList<>();
                            for (EncodedDataImpl dataToClose : mEncodedDataSet) {
                                waitForCloseFutures.add(dataToClose.getClosedFuture());
                            }
                            Futures.addCallback(Futures.allAsList(waitForCloseFutures),
                                    new FutureCallback<List<Void>>() {
                                        @Override
                                        public void onSuccess(@Nullable List<Void> result) {
                                            synchronized (mLock) {
                                                mMediaCodec.stop();
                                                try {
                                                    executor.execute(encoderCallback::onEncodeStop);
                                                } catch (RejectedExecutionException e) {
                                                    Logger.e(TAG,
                                                            "Unable to post to the supplied "
                                                                    + "executor.", e);
                                                }
                                                handleStopped();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            synchronized (mLock) {
                                                if (t instanceof MediaCodec.CodecException) {
                                                    handleEncodeError(
                                                            (MediaCodec.CodecException) t);
                                                } else {
                                                    handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                                            t.getMessage(), t);
                                                }
                                            }
                                        }
                                    }, CameraXExecutors.directExecutor());
                        }
                        break;
                    case CONFIGURED:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            synchronized (mLock) {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        handleEncodeError(e);
                        break;
                    case CONFIGURED:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            }
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
                @NonNull MediaFormat mediaFormat) {
            synchronized (mLock) {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        EncoderCallback encoderCallback = mEncoderCallback;
                        try {
                            mEncoderCallbackExecutor.execute(
                                    () -> encoderCallback.onOutputConfigUpdate(() -> mediaFormat));
                        } catch (RejectedExecutionException e) {
                            Logger.e(TAG, "Unable to post to the supplied executor.", e);
                        }
                        break;
                    case CONFIGURED:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class SurfaceInput implements Encoder.SurfaceInput {

        @GuardedBy("mLock")
        private Surface mSurface;

        @GuardedBy("mLock")
        private OnSurfaceUpdateListener mSurfaceUpdateListener;

        @GuardedBy("mLock")
        private Executor mSurfaceUpdateExecutor;

        /**
         * Sets the surface update listener.
         *
         * @param executor the executor to invoke the listener
         * @param listener the surface update listener
         */
        @Override
        public void setOnSurfaceUpdateListener(@NonNull Executor executor,
                @NonNull OnSurfaceUpdateListener listener) {
            synchronized (mLock) {
                mSurfaceUpdateListener = Preconditions.checkNotNull(listener);
                mSurfaceUpdateExecutor = Preconditions.checkNotNull(executor);

                if (mSurface != null) {
                    notifySurfaceUpdate(mSurface);
                }

            }
        }

        @GuardedBy("mLock")
        @SuppressLint("UnsafeNewApiCall")
        void resetSurface() {
            if (Build.VERSION.SDK_INT >= 23) {
                if (mSurface == null) {
                    mSurface = MediaCodec.createPersistentInputSurface();
                    notifySurfaceUpdate(mSurface);
                }
                mMediaCodec.setInputSurface(mSurface);
            } else {
                mSurface = mMediaCodec.createInputSurface();
                notifySurfaceUpdate(mSurface);
            }
        }

        @GuardedBy("mLock")
        private void notifySurfaceUpdate(@NonNull Surface surface) {
            if (mSurfaceUpdateListener != null && mSurfaceUpdateExecutor != null) {
                OnSurfaceUpdateListener listener = mSurfaceUpdateListener;
                try {
                    mSurfaceUpdateExecutor.execute(() -> listener.onSurfaceUpdate(surface));
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Unable to post to the supplied executor.", e);
                    surface.release();
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class ByteBufferInput implements Encoder.ByteBufferInput {

        @GuardedBy("mLock")
        private final Queue<Consumer<Integer>> mListenerQueue = new ArrayDeque<>();

        @GuardedBy("mLock")
        private final Queue<Integer> mFreeBufferIndexQueue = new ArrayDeque<>();

        /** {@inheritDoc} */
        @Override
        public void putByteBuffer(@NonNull ByteBuffer byteBuffer) {
            synchronized (mLock) {
                switch (mState) {
                    case STARTED:
                        // Here it means the byteBuffer should definitely be queued into codec.
                        acquireFreeBufferIndex(freeBufferIndex -> {
                            ByteBuffer inputBuffer = null;
                            synchronized (mLock) {
                                if (mState == STARTED
                                        || mState == PAUSED
                                        || mState == STOPPING
                                        || mState == PENDING_START
                                        || mState == PENDING_START_PAUSED
                                        || mState == PENDING_RELEASE) {
                                    try {
                                        inputBuffer = mMediaCodec.getInputBuffer(freeBufferIndex);
                                    } catch (MediaCodec.CodecException e) {
                                        handleEncodeError(e);
                                        return;
                                    }
                                }
                            }

                            if (inputBuffer == null) {
                                return;
                            }
                            inputBuffer.put(byteBuffer);

                            synchronized (mLock) {
                                if (mState == STARTED
                                        || mState == PAUSED
                                        || mState == STOPPING
                                        || mState == PENDING_START
                                        || mState == PENDING_START_PAUSED
                                        || mState == PENDING_RELEASE) {
                                    try {
                                        mMediaCodec.queueInputBuffer(freeBufferIndex, 0,
                                                inputBuffer.position(),
                                                generatePresentationTimeUs(),
                                                0);
                                    } catch (MediaCodec.CodecException e) {
                                        handleEncodeError(e);
                                        return;
                                    }
                                }
                            }
                        });
                        break;
                    case PAUSED:
                        // Drop the data
                        break;
                    case CONFIGURED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_RELEASE:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            }
        }

        @GuardedBy("mLock")
        void signalEndOfInputStream() {
            acquireFreeBufferIndex(freeBufferIndex -> {
                synchronized (mLock) {
                    switch (mState) {
                        case STARTED:
                        case PAUSED:
                        case STOPPING:
                        case PENDING_START:
                        case PENDING_START_PAUSED:
                        case PENDING_RELEASE:
                            try {
                                mMediaCodec.queueInputBuffer(freeBufferIndex, 0, 0,
                                        generatePresentationTimeUs(),
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            } catch (MediaCodec.CodecException e) {
                                handleEncodeError(e);
                            }
                            break;
                        case CONFIGURED:
                        case RELEASED:
                            // Do nothing
                            break;
                        default:
                            throw new IllegalStateException("Unknown state: " + mState);
                    }
                }
            });
        }

        @GuardedBy("mLock")
        void putFreeBufferIndex(int index) {
            mFreeBufferIndexQueue.offer(index);
            match();
        }

        @GuardedBy("mLock")
        void clearFreeBuffers() {
            mListenerQueue.clear();
            mFreeBufferIndexQueue.clear();
        }

        @GuardedBy("mLock")
        private void acquireFreeBufferIndex(
                @NonNull Consumer<Integer> onFreeBufferIndexListener) {
            synchronized (mLock) {
                mListenerQueue.offer(onFreeBufferIndexListener);
                match();
            }
        }

        @GuardedBy("mLock")
        private void match() {
            if (!mListenerQueue.isEmpty() && !mFreeBufferIndexQueue.isEmpty()) {
                Consumer<Integer> listener = mListenerQueue.poll();
                Integer index = mFreeBufferIndexQueue.poll();
                try {
                    mExecutor.execute(() -> listener.accept(index));
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Unable to post to the supplied executor.", e);
                    putFreeBufferIndex(index);
                }
            }
        }
    }
}
