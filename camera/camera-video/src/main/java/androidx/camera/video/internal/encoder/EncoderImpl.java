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
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.ERROR;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PAUSED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_RELEASE;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_START;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_START_PAUSED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.RELEASED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.STARTED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.STOPPING;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.internal.workaround.EncoderFinder;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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

        /**
         * Then state is when the encoder encounter error. Error state is a transitional state
         * where encoder user is supposed to wait for {@link EncoderCallback#onEncodeStop} or
         * {@link EncoderCallback#onEncodeError}. Any method call during this state should be
         * ignore except {@link #release}.
         */
        ERROR,

        /** The state is when the encoder is released. */
        RELEASED,
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mLock = new Object();
    private final InternalStateObservable mStateObservable = new InternalStateObservable();
    private final MediaFormat mMediaFormat;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final MediaCodec mMediaCodec;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final EncoderInput mEncoderInput;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mEncoderExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Queue<Integer> mFreeInputBufferIndexQueue = new ArrayDeque<>();
    private final Queue<Completer<InputBuffer>> mAcquisitionQueue = new ArrayDeque<>();
    private final Set<InputBuffer> mInputBufferSet = new HashSet<>();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Set<EncodedDataImpl> mEncodedDataSet = new HashSet<>();

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    EncoderCallback mEncoderCallback = EncoderCallback.EMPTY;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    Executor mEncoderCallbackExecutor = CameraXExecutors.mainThreadExecutor();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    InternalState mState;

    final EncoderFinder mEncoderFinder = new EncoderFinder();
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

        mEncoderExecutor = CameraXExecutors.newSequentialExecutor(executor);

        if (encoderConfig instanceof AudioEncoderConfig) {
            ByteBufferInput byteBufferInput = new ByteBufferInput();
            // State change will run on mEncoderExecutor, use direct executor to avoid delay.
            mStateObservable.addObserver(CameraXExecutors.directExecutor(),
                    new Observable.Observer<InternalState>() {
                        @ExecutedBy("mEncoderExecutor")
                        @Override
                        public void onNewData(@Nullable InternalState value) {
                            byteBufferInput.setActive(value == STARTED);
                        }

                        @ExecutedBy("mEncoderExecutor")
                        @Override
                        public void onError(@NonNull Throwable t) {
                            // No defined. Ignore.
                        }
                    });
            mEncoderInput = byteBufferInput;
        } else if (encoderConfig instanceof VideoEncoderConfig) {
            mEncoderInput = new SurfaceInput();
        } else {
            throw new InvalidConfigException("Unknown encoder config type");
        }

        mMediaFormat = encoderConfig.toMediaFormat();
        mMediaCodec = selectMediaCodecEncoder(mMediaFormat);

        try {
            reset();
        } catch (MediaCodec.CodecException e) {
            throw new InvalidConfigException(e);
        }

        setState(CONFIGURED);
    }

    @ExecutedBy("mEncoderExecutor")
    private void reset() {
        mFreeInputBufferIndexQueue.clear();

        // Cancel incomplete acquisitions if exists.
        for (Completer<InputBuffer> completer : mAcquisitionQueue) {
            completer.setCancelled();
        }
        mAcquisitionQueue.clear();

        mMediaCodec.reset();
        mMediaCodec.setCallback(new MediaCodecCallback());
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        if (mEncoderInput instanceof SurfaceInput) {
            ((SurfaceInput) mEncoderInput).resetSurface();
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
        mEncoderExecutor.execute(() -> {
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
                case ERROR:
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
        });
    }

    /**
     * Stops the encoder.
     *
     * <p>It will trigger {@link EncoderCallback#onEncodeStop} after the last encoded data. It can
     * call {@link #start} to start again.
     */
    @Override
    public void stop() {
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                case STOPPING:
                case ERROR:
                    // Do nothing
                    break;
                case STARTED:
                case PAUSED:
                    setState(STOPPING);
                    if (mEncoderInput instanceof ByteBufferInput) {
                        // Wait for all issued input buffer done to avoid input loss.
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        for (InputBuffer inputBuffer : mInputBufferSet) {
                            futures.add(inputBuffer.getTerminationFuture());
                        }
                        Futures.successfulAsList(futures).addListener(this::signalEndOfInputStream,
                                mEncoderExecutor);
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
        });
    }

    /**
     * Pauses the encoder.
     *
     * <p>{@link #pause} only work between {@link #start} and {@link #stop}. Once the encoder is
     * paused, it will drop the input data until {@link #start} is invoked again.
     */
    @Override
    public void pause() {
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                case PAUSED:
                case ERROR:
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
        });
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
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                case STARTED:
                case PAUSED:
                case ERROR:
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
        });
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

    @ExecutedBy("mEncoderExecutor")
    private void setState(InternalState state) {
        if (mState == state) {
            return;
        }
        Logger.d(TAG, "Transitioning encoder internal state: " + mState + " --> " + state);
        mState = state;
        mStateObservable.notifyState();
    }

    @ExecutedBy("mEncoderExecutor")
    private void updatePauseToMediaCodec(boolean paused) {
        Bundle bundle = new Bundle();
        bundle.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, paused ? 1 : 0);
        mMediaCodec.setParameters(bundle);
    }

    @ExecutedBy("mEncoderExecutor")
    private void signalEndOfInputStream() {
        Futures.addCallback(acquireInputBuffer(),
                new FutureCallback<InputBuffer>() {
                    @Override
                    public void onSuccess(InputBuffer inputBuffer) {
                        inputBuffer.setPresentationTimeUs(generatePresentationTimeUs());
                        inputBuffer.setEndOfStream(true);
                        inputBuffer.submit();

                        Futures.addCallback(inputBuffer.getTerminationFuture(),
                                new FutureCallback<Void>() {
                                    @ExecutedBy("mEncoderExecutor")
                                    @Override
                                    public void onSuccess(@Nullable Void result) {
                                        // Do nothing.
                                    }

                                    @ExecutedBy("mEncoderExecutor")
                                    @Override
                                    public void onFailure(Throwable t) {
                                        if (t instanceof MediaCodec.CodecException) {
                                            handleEncodeError(
                                                    (MediaCodec.CodecException) t);
                                        } else {
                                            handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                                    t.getMessage(), t);
                                        }
                                    }
                                }, mEncoderExecutor);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                "Unable to acquire InputBuffer.", t);
                    }
                }, mEncoderExecutor);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mEncoderExecutor")
    void handleEncodeError(@NonNull MediaCodec.CodecException e) {
        handleEncodeError(EncodeException.ERROR_CODEC, e.getMessage(), e);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mEncoderExecutor")
    void handleEncodeError(@EncodeException.ErrorType int error, @Nullable String message,
            @Nullable Throwable throwable) {
        switch (mState) {
            case CONFIGURED:
                // Unable to start MediaCodec. This is a fatal error. Try to reset the encoder.
                notifyError(error, message, throwable);
                reset();
                break;
            case STARTED:
            case PAUSED:
            case STOPPING:
            case PENDING_START_PAUSED:
            case PENDING_START:
            case PENDING_RELEASE:
                setState(ERROR);
                stopMediaCodec(() -> notifyError(error, message, throwable));
                break;
            case ERROR:
                Logger.w(TAG, "Get more than one error: " + message + "(" + error + ")",
                        throwable);
                break;
            case RELEASED:
                // Do nothing
                break;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void notifyError(@EncodeException.ErrorType int error, @Nullable String message,
            @Nullable Throwable throwable) {
        EncoderCallback callback;
        Executor executor;
        synchronized (mLock) {
            callback = mEncoderCallback;
            executor = mEncoderCallbackExecutor;
        }
        try {
            executor.execute(
                    () -> callback.onEncodeError(new EncodeException(error, message, throwable)));
        } catch (RejectedExecutionException e) {
            Logger.e(TAG, "Unable to post to the supplied executor.", e);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mEncoderExecutor")
    void stopMediaCodec(@Nullable Runnable afterStop) {
        /*
         * MediaCodec#close will free all its input/output ByteBuffers. Therefore, before calling
         * MediaCodec#close, it must ensure all dispatched EncodedData(output ByteBuffers) and
         * InputBuffer(input ByteBuffers) are complete. Otherwise, the ByteBuffer receiver will
         * get buffer overflow when accessing the ByteBuffers.
         */
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (EncodedDataImpl dataToClose : mEncodedDataSet) {
            futures.add(dataToClose.getClosedFuture());
        }
        for (InputBuffer inputBuffer : mInputBufferSet) {
            futures.add(inputBuffer.getTerminationFuture());
        }
        Futures.successfulAsList(futures).addListener(() -> {
            mMediaCodec.stop();
            if (afterStop != null) {
                afterStop.run();
            }
            handleStopped();
        }, mEncoderExecutor);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mEncoderExecutor")
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

    @NonNull
    private MediaCodec selectMediaCodecEncoder(@NonNull MediaFormat mediaFormat)
            throws InvalidConfigException {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        String encoderName;

        encoderName = mEncoderFinder.findEncoderForFormat(mediaFormat, mediaCodecList);

        MediaCodec codec;

        try {
            codec = MediaCodec.createByCodecName(encoderName);
        } catch (IOException | NullPointerException | IllegalArgumentException e) {
            throw new InvalidConfigException("Encoder cannot created: " + encoderName, e);
        }
        Logger.i(TAG, "Selected encoder: " + codec.getName());

        return codec;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mEncoderExecutor")
    @NonNull
    ListenableFuture<InputBuffer> acquireInputBuffer() {
        switch (mState) {
            case CONFIGURED:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "Encoder is not started yet."));
            case STARTED:
            case PAUSED:
            case STOPPING:
            case PENDING_START:
            case PENDING_START_PAUSED:
            case PENDING_RELEASE:
                AtomicReference<Completer<InputBuffer>> ref = new AtomicReference<>();
                ListenableFuture<InputBuffer> future = CallbackToFutureAdapter.getFuture(
                        completer -> {
                            ref.set(completer);
                            return "acquireInputBuffer";
                        });
                Completer<InputBuffer> completer = Preconditions.checkNotNull(ref.get());
                mAcquisitionQueue.offer(completer);
                completer.addCancellationListener(() -> mAcquisitionQueue.remove(completer),
                        mEncoderExecutor);
                matchAcquisitionsAndFreeBufferIndexes();
                return future;
            case ERROR:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "Encoder is in error state."));
            case RELEASED:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "Encoder is released."));
            default:
                throw new IllegalStateException("Unknown state: " + mState);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mEncoderExecutor")
    void matchAcquisitionsAndFreeBufferIndexes() {
        while (!mAcquisitionQueue.isEmpty() && !mFreeInputBufferIndexQueue.isEmpty()) {
            Completer<InputBuffer> completer = mAcquisitionQueue.poll();
            int bufferIndex = mFreeInputBufferIndexQueue.poll();

            InputBufferImpl inputBuffer;
            try {
                inputBuffer = new InputBufferImpl(mMediaCodec, bufferIndex);
            } catch (MediaCodec.CodecException e) {
                handleEncodeError(e);
                return;
            }
            if (completer.set(inputBuffer)) {
                mInputBufferSet.add(inputBuffer);
                inputBuffer.getTerminationFuture().addListener(
                        () -> mInputBufferSet.remove(inputBuffer), mEncoderExecutor);
            } else {
                inputBuffer.cancel();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static long generatePresentationTimeUs() {
        return System.nanoTime() / 1000L;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class InternalStateObservable implements Observable<InternalState> {

        private final Map<Observer<InternalState>, Executor> mObservers = new LinkedHashMap<>();

        @ExecutedBy("mEncoderExecutor")
        void notifyState() {
            final InternalState state = mState;
            for (Map.Entry<Observer<InternalState>, Executor> entry : mObservers.entrySet()) {
                entry.getValue().execute(() -> entry.getKey().onNewData(state));
            }
        }

        @ExecutedBy("mEncoderExecutor")
        @NonNull
        @Override
        public ListenableFuture<InternalState> fetchData() {
            return Futures.immediateFuture(mState);
        }

        @ExecutedBy("mEncoderExecutor")
        @Override
        public void addObserver(@NonNull Executor executor,
                @NonNull Observer<InternalState> observer) {
            final InternalState state = mState;
            mObservers.put(observer, executor);
            executor.execute(() -> observer.onNewData(state));
        }

        @ExecutedBy("mEncoderExecutor")
        @Override
        public void removeObserver(@NonNull Observer<InternalState> observer) {
            mObservers.remove(observer);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class MediaCodecCallback extends MediaCodec.Callback {

        private boolean mHasFirstData = false;

        @Override
        public void onInputBufferAvailable(MediaCodec mediaCodec, int index) {
            mEncoderExecutor.execute(() -> {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        mFreeInputBufferIndexQueue.offer(index);
                        matchAcquisitionsAndFreeBufferIndexes();
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index,
                @NonNull MediaCodec.BufferInfo bufferInfo) {
            mEncoderExecutor.execute(() -> {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        final EncoderCallback encoderCallback;
                        final Executor executor;
                        synchronized (mLock) {
                            encoderCallback = mEncoderCallback;
                            executor = mEncoderCallbackExecutor;
                        }

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
                                            mEncodedDataSet.remove(encodedData);
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            mEncodedDataSet.remove(encodedData);
                                            if (t instanceof MediaCodec.CodecException) {
                                                handleEncodeError(
                                                        (MediaCodec.CodecException) t);
                                            } else {
                                                handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                                        t.getMessage(), t);
                                            }
                                        }
                                    }, mEncoderExecutor);
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
                            stopMediaCodec(() -> {
                                try {
                                    executor.execute(encoderCallback::onEncodeStop);
                                } catch (RejectedExecutionException e) {
                                    Logger.e(TAG, "Unable to post to the supplied executor.", e);
                                }
                            });
                        }
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            mEncoderExecutor.execute(() -> {
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
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
                @NonNull MediaFormat mediaFormat) {
            mEncoderExecutor.execute(() -> {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        EncoderCallback encoderCallback;
                        Executor executor;
                        synchronized (mLock) {
                            encoderCallback = mEncoderCallback;
                            executor = mEncoderCallbackExecutor;
                        }
                        try {
                            executor.execute(
                                    () -> encoderCallback.onOutputConfigUpdate(() -> mediaFormat));
                        } catch (RejectedExecutionException e) {
                            Logger.e(TAG, "Unable to post to the supplied executor.", e);
                        }
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class SurfaceInput implements Encoder.SurfaceInput {

        private final Object mLock = new Object();

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
            Surface surface;
            synchronized (mLock) {
                mSurfaceUpdateListener = Preconditions.checkNotNull(listener);
                mSurfaceUpdateExecutor = Preconditions.checkNotNull(executor);
                surface = mSurface;
            }
            if (surface != null) {
                notifySurfaceUpdate(executor, listener, surface);
            }
        }

        @SuppressLint("UnsafeNewApiCall")
        void resetSurface() {
            Surface surface;
            Executor executor;
            OnSurfaceUpdateListener listener;
            synchronized (mLock) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (mSurface == null) {
                        mSurface = MediaCodec.createPersistentInputSurface();
                        surface = mSurface;
                    } else {
                        surface = null;
                    }
                    mMediaCodec.setInputSurface(mSurface);
                } else {
                    mSurface = mMediaCodec.createInputSurface();
                    surface = mSurface;
                }
                listener = mSurfaceUpdateListener;
                executor = mSurfaceUpdateExecutor;
            }
            if (surface != null && listener != null && executor != null) {
                notifySurfaceUpdate(executor, listener, surface);
            }
        }

        private void notifySurfaceUpdate(@NonNull Executor executor,
                @NonNull OnSurfaceUpdateListener listener, @NonNull Surface surface) {
            try {
                executor.execute(() -> listener.onSurfaceUpdate(surface));
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "Unable to post to the supplied executor.", e);
                surface.release();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class ByteBufferInput implements Encoder.ByteBufferInput {

        private final Map<Observer<State>, Executor> mStateObservers = new LinkedHashMap<>();

        private State mBufferProviderState = State.INACTIVE;

        private final List<ListenableFuture<InputBuffer>> mAcquisitionList = new ArrayList<>();

        /** {@inheritDoc} */
        @NonNull
        @Override
        public ListenableFuture<State> fetchData() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                mEncoderExecutor.execute(() -> completer.set(mBufferProviderState));
                return "fetchData";
            });
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public ListenableFuture<InputBuffer> acquireBuffer() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                mEncoderExecutor.execute(() -> {
                    if (mBufferProviderState == State.ACTIVE) {
                        ListenableFuture<InputBuffer> future = acquireInputBuffer();
                        Futures.propagate(future, completer);
                        // Cancel by outer, also cancel internal future.
                        completer.addCancellationListener(() -> future.cancel(true),
                                CameraXExecutors.directExecutor());

                        // Keep tracking the acquisition by internal future. Once the provider state
                        // transition to inactive, cancel the internal future can also send signal
                        // to outer future since we propagate the internal result to the completer.
                        mAcquisitionList.add(future);
                        future.addListener(() -> mAcquisitionList.remove(future), mEncoderExecutor);
                    } else if (mBufferProviderState == State.INACTIVE) {
                        completer.setException(
                                new IllegalStateException("BufferProvider is not active."));
                    } else {
                        completer.setException(
                                new IllegalStateException(
                                        "Unknown state: " + mBufferProviderState));
                    }
                });
                return "acquireBuffer";
            });
        }

        /** {@inheritDoc} */
        @Override
        public void addObserver(@NonNull Executor executor, @NonNull Observer<State> observer) {
            mEncoderExecutor.execute(() -> {
                mStateObservers.put(Preconditions.checkNotNull(observer),
                        Preconditions.checkNotNull(executor));
                final State state = mBufferProviderState;
                executor.execute(() -> observer.onNewData(state));
            });
        }

        /** {@inheritDoc} */
        @Override
        public void removeObserver(@NonNull Observer<State> observer) {
            mEncoderExecutor.execute(
                    () -> mStateObservers.remove(Preconditions.checkNotNull(observer)));
        }

        @ExecutedBy("mEncoderExecutor")
        void setActive(boolean isActive) {
            final State newState = isActive ? State.ACTIVE : State.INACTIVE;
            if (mBufferProviderState == newState) {
                return;
            }
            mBufferProviderState = newState;

            if (newState == State.INACTIVE) {
                for (ListenableFuture<InputBuffer> future : mAcquisitionList) {
                    future.cancel(true);
                }
                mAcquisitionList.clear();
            }

            for (Map.Entry<Observer<State>, Executor> entry : mStateObservers.entrySet()) {
                try {
                    entry.getValue().execute(() -> entry.getKey().onNewData(newState));
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Unable to post to the supplied executor.", e);
                }
            }
        }
    }
}
