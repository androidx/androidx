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

package androidx.camera.video.internal.audio;

import static androidx.camera.video.internal.audio.AudioSource.InternalState.CONFIGURED;
import static androidx.camera.video.internal.audio.AudioSource.InternalState.RELEASED;
import static androidx.camera.video.internal.audio.AudioSource.InternalState.STARTED;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.internal.BufferProvider;
import androidx.camera.video.internal.encoder.InputBuffer;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AudioSource is used to obtain audio raw data and write to the buffer from {@link BufferProvider}.
 *
 * <p>The audio raw data could be one of sources from the device. The target source can be
 * specified with {@link AudioSettings.Builder#setAudioSource(int)}.
 *
 * <p>Calling {@link #start} will start reading audio data from the target source and then write
 * the data into the buffer from {@link BufferProvider}. Calling {@link #stop} will stop sending
 * audio data. However, to really read/write data to buffer, the {@link BufferProvider}'s state
 * must be {@link BufferProvider.State#ACTIVE}. So recording may temporarily pause when the
 * {@link BufferProvider}'s state is {@link BufferProvider.State#INACTIVE}.
 *
 * @see BufferProvider
 * @see AudioRecord
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioSource {
    private static final String TAG = "AudioSource";

    /**
     * The default start retry interval in milliseconds.
     *
     * @see #start()
     */
    @VisibleForTesting
    static final long DEFAULT_START_RETRY_INTERVAL_MS = 3000L;

    enum InternalState {
        /** The initial state or when {@link #stop} is called after started. */
        CONFIGURED,

        /** The state is when it is in {@link #CONFIGURED} state and {@link #start} is called. */
        STARTED,

        /** The state is when {@link #release} is called. */
        RELEASED,
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AtomicReference<Boolean> mNotifiedSilenceState = new AtomicReference<>(null);

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AtomicBoolean mNotifiedSuspendState = new AtomicBoolean(false);

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AudioStream mAudioStream;

    final SilentAudioStream mSilentAudioStream;

    private final long mStartRetryIntervalNs;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    InternalState mState = CONFIGURED;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    BufferProvider.State mBufferProviderState = BufferProvider.State.INACTIVE;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mIsSendingAudio;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    Executor mCallbackExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    AudioSourceCallback mAudioSourceCallback;

    // The following should only be accessed by mExecutor
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    BufferProvider<? extends InputBuffer> mBufferProvider;
    @Nullable
    private FutureCallback<InputBuffer> mAcquireBufferCallback;
    @Nullable
    private Observable.Observer<BufferProvider.State> mStateObserver;
    boolean mInSilentStartState;
    private long mLatestFailedStartTimeNs;
    boolean mAudioStreamSilenced;
    boolean mMuted;
    @Nullable
    private byte[] mZeroBytes;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            double mAudioAmplitude;
    long mAmplitudeTimestamp = 0;
    private final int mAudioFormat;

    /**
     * Creates an AudioSource for the given settings.
     *
     * <p>It should be verified the combination of sample rate, channel count and audio format is
     * supported with {@link #isSettingsSupported(int, int, int)} before passing the settings to
     * this constructor, or an {@link UnsupportedOperationException} will be thrown.
     *
     * @param settings           The settings that will be used to configure the audio source.
     * @param executor           An executor that will be used to read audio samples in the
     *                           background. The
     *                           threads of this executor may be blocked while waiting for samples.
     * @param attributionContext A {@link Context} object that will be used to attribute the
     *                           audio to the contained {@link android.content.AttributionSource}.
     *                           Audio attribution is only available on API 31+. Setting this on
     *                           lower API levels or if the context does not contain an
     *                           attribution source, setting this context will have no effect.
     *                           This context will not be retained beyond the scope of the
     *                           constructor.
     * @throws UnsupportedOperationException if the combination of sample rate, channel count,
     *                                       and audio format in the provided settings is
     *                                       unsupported.
     * @throws AudioSourceAccessException    if the audio device is not available or cannot be
     *                                       initialized with the given settings.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public AudioSource(@NonNull AudioSettings settings, @NonNull Executor executor,
            @Nullable Context attributionContext) throws AudioSourceAccessException {
        this(settings, executor, attributionContext, AudioStreamImpl::new,
                DEFAULT_START_RETRY_INTERVAL_MS);
    }

    @VisibleForTesting
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    AudioSource(@NonNull AudioSettings settings, @NonNull Executor executor,
            @Nullable Context attributionContext, @NonNull AudioStreamFactory audioStreamFactory,
            long startRetryIntervalMs) throws AudioSourceAccessException {
        mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        mStartRetryIntervalNs = MILLISECONDS.toNanos(startRetryIntervalMs);
        try {
            mAudioStream = audioStreamFactory.create(settings, attributionContext);
        } catch (IllegalArgumentException | AudioStream.AudioStreamException e) {
            throw new AudioSourceAccessException("Unable to create AudioStream", e);
        }
        mAudioStream.setCallback(new AudioStreamCallback(), mExecutor);
        mSilentAudioStream = new SilentAudioStream(settings);
        mAudioFormat = settings.getAudioFormat();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    class AudioStreamCallback implements AudioStream.AudioStreamCallback {
        @ExecutedBy("mExecutor")
        @Override
        public void onSilenceStateChanged(boolean isSilenced) {
            mAudioStreamSilenced = isSilenced;
            if (mState == STARTED) {
                notifySilenced();
            }
        }
    }

    /**
     * Sets the {@link BufferProvider}.
     *
     * <p>A buffer provider is required to stream audio. If no buffer provider is provided, then
     * audio will be dropped until one is provided and active.
     *
     * @param bufferProvider The new buffer provider to use.
     */
    public void setBufferProvider(@NonNull BufferProvider<? extends InputBuffer> bufferProvider) {
        mExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                    // Fall-through
                case STARTED:
                    if (mBufferProvider != bufferProvider) {
                        resetBufferProvider(bufferProvider);
                    }
                    break;
                case RELEASED:
                    throw new AssertionError("AudioSource is released");
            }
        });
    }

    /**
     * Starts the AudioSource.
     *
     * <p>Before starting, a {@link BufferProvider} should be set with
     * {@link #setBufferProvider(BufferProvider)}. If a buffer provider is not set, audio data
     * will be dropped.
     *
     * <p>Audio data will start being sent to the {@link BufferProvider} when
     * {@link BufferProvider}'s state is {@link BufferProvider.State#ACTIVE}.
     *
     * <p>If the AudioSource fails to start, instead of firing
     * {@link AudioSourceCallback#onError(Throwable)}, it will
     * <li>Retry internally with a fixed interval.</li>
     * <li>Write silent audio to the BufferProvider until a successful retry or {@link #stop()}
     * is called.
     * <li>Trigger {@link AudioSourceCallback#onSilenceStateChanged(boolean)} with {@code true}
     * on the first failure and {@code false} on the successful retry.</li>
     *
     * <p>Use {@link #mute(boolean)} to mute the audio source before starting it. If not call,
     * the audio source will be started unmuted by default.
     */
    public void start() {
        mExecutor.execute(() -> start(mMuted));
    }

    /**
     * Starts the AudioSource.
     *
     * <p>Before starting, a {@link BufferProvider} should be set with
     * {@link #setBufferProvider(BufferProvider)}. If a buffer provider is not set, audio data
     * will be dropped.
     *
     * <p>Audio data will start being sent to the {@link BufferProvider} when
     * {@link BufferProvider}'s state is {@link BufferProvider.State#ACTIVE}.
     *
     * <p>If the AudioSource fails to start, instead of firing
     * {@link AudioSourceCallback#onError(Throwable)}, it will
     * <li>Retry internally with a fixed interval.</li>
     * <li>Write silent audio to the BufferProvider until a successful retry or {@link #stop()}
     * is called.
     * <li>Trigger {@link AudioSourceCallback#onSilenceStateChanged(boolean, int)} with {@code true}
     * on the first failure and {@code false} on the successful retry.</li>
     *
     * @param muted {@code true} to start the audio source muted, otherwise {@code false}.
     */
    public void start(boolean muted) {
        mExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                    mNotifiedSilenceState.set(null);
                    mNotifiedSuspendState.set(false);
                    setState(STARTED);
                    mute(muted);
                    updateSendingAudio();
                    break;
                case STARTED:
                    // Do nothing
                    break;
                case RELEASED:
                    throw new AssertionError("AudioSource is released");
            }
        });
    }

    /**
     * Stops the AudioSource.
     *
     * <p>Audio data will stop being sent to the {@link BufferProvider}.
     */
    public void stop() {
        mExecutor.execute(() -> {
            switch (mState) {
                case STARTED:
                    setState(CONFIGURED);
                    updateSendingAudio();
                    break;
                case CONFIGURED:
                    // Do nothing
                    break;
                case RELEASED:
                    Logger.w(TAG, "AudioSource is released. "
                            + "Calling stop() is a no-op.");
            }
        });
    }

    /**
     * Releases the AudioSource.
     *
     * <p>Once the AudioSource is released, it can not be used any more.
     */
    @NonNull
    public ListenableFuture<Void> release() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> {
                try {
                    switch (mState) {
                        case STARTED:
                            // Fall-through
                        case CONFIGURED:
                            resetBufferProvider(null);
                            mSilentAudioStream.release();
                            mAudioStream.release();
                            stopSendingAudio();
                            setState(RELEASED);
                            break;
                        case RELEASED:
                            // Do nothing
                            break;
                    }
                    completer.set(null);
                } catch (Throwable t) {
                    completer.setException(t);
                }
            });

            return "AudioSource-release";
        });
    }

    /**
     * Sets callback to receive configuration status.
     *
     * <p>The callback must be set before the audio source is started.
     *
     * @param executor the callback executor
     * @param callback the configuration callback
     */
    public void setAudioSourceCallback(@NonNull Executor executor,
            @NonNull AudioSourceCallback callback) {
        mExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                    mCallbackExecutor = executor;
                    mAudioSourceCallback = callback;
                    break;
                case STARTED:
                    // Fall-through
                case RELEASED:
                    throw new AssertionError("The audio recording callback must be "
                            + "registered before the audio source is started.");
            }
        });
    }

    /** Mutes or un-mutes the audio source. */
    public void mute(boolean muted) {
        mExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                    // Fall-through
                case STARTED:
                    if (mMuted == muted) {
                        return;
                    }
                    mMuted = muted;
                    if (mState == STARTED) {
                        notifySilenced();
                    }
                    break;
                case RELEASED:
                    throw new AssertionError("AudioSource is released");
            }
        });
    }

    @ExecutedBy("mExecutor")
    private void resetBufferProvider(
            @Nullable BufferProvider<? extends InputBuffer> bufferProvider) {
        if (mBufferProvider != null) {
            mBufferProvider.removeObserver(requireNonNull(mStateObserver));
            mBufferProvider = null;
            mStateObserver = null;
            mAcquireBufferCallback = null;
            mBufferProviderState = BufferProvider.State.INACTIVE;
            updateSendingAudio();
        }
        if (bufferProvider != null) {
            mBufferProvider = bufferProvider;
            mStateObserver = new Observable.Observer<BufferProvider.State>() {
                @ExecutedBy("mExecutor")
                @Override
                public void onNewData(@Nullable BufferProvider.State state) {
                    requireNonNull(state);
                    if (mBufferProvider == bufferProvider) {
                        Logger.d(TAG, "Receive BufferProvider state change: "
                                + mBufferProviderState + " to " + state);
                        if (mBufferProviderState != state) {
                            mBufferProviderState = state;
                            updateSendingAudio();
                        }
                    }
                }

                @ExecutedBy("mExecutor")
                @Override
                public void onError(@NonNull Throwable throwable) {
                    if (mBufferProvider == bufferProvider) {
                        notifyError(throwable);
                    }
                }
            };

            mAcquireBufferCallback = new FutureCallback<InputBuffer>() {
                @ExecutedBy("mExecutor")
                @Override
                public void onSuccess(InputBuffer inputBuffer) {
                    if (!mIsSendingAudio || mBufferProvider != bufferProvider) {
                        inputBuffer.cancel();
                        return;
                    }
                    if (mInSilentStartState && isStartRetryIntervalReached()) {
                        retryStartAudioStream();
                        // TODO(b/269393269): when a retry succeed, there will be a small time gap
                        //  between silence and real audio. The gap should be filled with
                        //  silence audio.
                    }
                    // If the audio stream fails to start, SilentAudioStream will be used.
                    AudioStream audioStream = getCurrentAudioStream();
                    ByteBuffer byteBuffer = inputBuffer.getByteBuffer();
                    AudioStream.PacketInfo packetInfo = audioStream.read(byteBuffer);
                    if (packetInfo.getSizeInBytes() > 0) {
                        if (mMuted) {
                            overrideBySilence(byteBuffer, packetInfo.getSizeInBytes());
                        }
                        // should only be ENCODING_PCM_16BIT for now at least
                        // reads incoming bytebuffer for amplitude value every .2 seconds
                        if (mCallbackExecutor != null
                                && (packetInfo.getTimestampNs() - mAmplitudeTimestamp) >= 200) {
                            mAmplitudeTimestamp = packetInfo.getTimestampNs();
                            postMaxAmplitude(byteBuffer);
                        }
                        byteBuffer.limit(byteBuffer.position() + packetInfo.getSizeInBytes());
                        inputBuffer.setPresentationTimeUs(
                                NANOSECONDS.toMicros(packetInfo.getTimestampNs()));
                        inputBuffer.submit();
                    } else {
                        Logger.w(TAG, "Unable to read data from AudioRecord.");
                        inputBuffer.cancel();
                    }
                    sendNextAudio();
                }

                @ExecutedBy("mExecutor")
                @Override
                public void onFailure(@NonNull Throwable throwable) {
                    if (mBufferProvider != bufferProvider) {
                        return;
                    }
                    Logger.d(TAG, "Unable to get input buffer, the BufferProvider "
                            + "could be transitioning to INACTIVE state.");
                    // IllegalStateException and CancellationException (extends
                    // IllegalStateException) indicate BufferProvider is transitioning to
                    // INACTIVE state, which is normal case and should not notify error.
                    if (!(throwable instanceof IllegalStateException)) {
                        notifyError(throwable);
                    }
                }
            };
            // Update BufferProvider state as possible.
            BufferProvider.State state = fetchBufferProviderState(bufferProvider);
            if (state != null) {
                mBufferProviderState = state;
                updateSendingAudio();
            }
            mBufferProvider.addObserver(mExecutor, mStateObserver);
        }
    }

    @ExecutedBy("mExecutor")
    @NonNull
    AudioStream getCurrentAudioStream() {
        return mInSilentStartState ? mSilentAudioStream : mAudioStream;
    }

    @ExecutedBy("mExecutor")
    void retryStartAudioStream() {
        checkState(mInSilentStartState);
        try {
            mAudioStream.start();
            Logger.d(TAG, "Retry start AudioStream succeed");
            mSilentAudioStream.stop();
            mInSilentStartState = false;
        } catch (AudioStream.AudioStreamException e) {
            Logger.w(TAG, "Retry start AudioStream failed", e);
            mLatestFailedStartTimeNs = getCurrentSystemTimeNs();
        }
    }

    @ExecutedBy("mExecutor")
    boolean isStartRetryIntervalReached() {
        checkState(mLatestFailedStartTimeNs > 0);
        return getCurrentSystemTimeNs() - mLatestFailedStartTimeNs >= mStartRetryIntervalNs;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void notifyError(@NonNull Throwable throwable) {
        Executor executor = mCallbackExecutor;
        AudioSourceCallback callback = mAudioSourceCallback;
        if (executor != null && callback != null) {
            executor.execute(() -> callback.onError(throwable));
        }
    }

    @ExecutedBy("mExecutor")
    void notifySilenced() {
        Executor executor = mCallbackExecutor;
        AudioSourceCallback callback = mAudioSourceCallback;
        if (executor != null && callback != null) {
            boolean isSilenced = mMuted || mInSilentStartState || mAudioStreamSilenced;
            if (!Objects.equals(mNotifiedSilenceState.getAndSet(isSilenced), isSilenced)) {
                executor.execute(() -> callback.onSilenceStateChanged(isSilenced));
            }
        }
    }

    @ExecutedBy("mExecutor")
    void notifySuspended(boolean isSuspended) {
        Executor executor = mCallbackExecutor;
        AudioSourceCallback callback = mAudioSourceCallback;
        if (executor != null && callback != null) {
            if (mNotifiedSuspendState.getAndSet(isSuspended) != isSuspended) {
                executor.execute(() -> callback.onSuspendStateChanged(isSuspended));
            }
        }
    }

    @ExecutedBy("mExecutor")
    void overrideBySilence(@NonNull ByteBuffer byteBuffer, int sizeInBytes) {
        if (mZeroBytes == null || mZeroBytes.length < sizeInBytes) {
            mZeroBytes = new byte[sizeInBytes];
        }
        int positionBeforePut = byteBuffer.position();
        byteBuffer.put(mZeroBytes, 0, sizeInBytes);
        byteBuffer.limit(byteBuffer.position()).position(positionBeforePut);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void updateSendingAudio() {
        if (mState == STARTED) {
            boolean isBufferProviderActive = mBufferProviderState == BufferProvider.State.ACTIVE;
            notifySuspended(!isBufferProviderActive);
            if (isBufferProviderActive) {
                startSendingAudio();
            } else {
                stopSendingAudio();
            }
        } else {
            stopSendingAudio();
        }
    }

    @ExecutedBy("mExecutor")
    private void startSendingAudio() {
        if (mIsSendingAudio) {
            // Already started, ignore
            return;
        }
        try {
            Logger.d(TAG, "startSendingAudio");
            mAudioStream.start();
            mInSilentStartState = false;
        } catch (AudioStream.AudioStreamException e) {
            Logger.w(TAG, "Failed to start AudioStream", e);
            mInSilentStartState = true;
            mSilentAudioStream.start();
            mLatestFailedStartTimeNs = getCurrentSystemTimeNs();
            notifySilenced();
        }
        mIsSendingAudio = true;
        sendNextAudio();
    }

    @ExecutedBy("mExecutor")
    private void stopSendingAudio() {
        if (!mIsSendingAudio) {
            // Already stopped, ignore.
            return;
        }
        mIsSendingAudio = false;
        Logger.d(TAG, "stopSendingAudio");
        mAudioStream.stop();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void sendNextAudio() {
        Futures.addCallback(requireNonNull(mBufferProvider).acquireBuffer(),
                requireNonNull(mAcquireBufferCallback),
                mExecutor);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void setState(InternalState state) {
        Logger.d(TAG, "Transitioning internal state: " + mState + " --> " + state);
        mState = state;
    }

    void postMaxAmplitude(ByteBuffer byteBuffer) {
        Executor executor = mCallbackExecutor;
        AudioSourceCallback callback = mAudioSourceCallback;
        double maxAmplitude = 0;

        if (mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
            //TODO
            // may want to add calculation for different audio formats
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

            while (shortBuffer.hasRemaining()) {
                maxAmplitude = Math.max(maxAmplitude, Math.abs(shortBuffer.get()));
            }

            maxAmplitude = maxAmplitude / Short.MAX_VALUE;

            mAudioAmplitude = maxAmplitude;

            if (executor != null && callback != null) {
                executor.execute(() -> callback.onAmplitudeValue(mAudioAmplitude));
            }
        }
    }


    @Nullable
    private static BufferProvider.State fetchBufferProviderState(
            @NonNull BufferProvider<? extends InputBuffer> bufferProvider) {
        try {
            ListenableFuture<BufferProvider.State> state = bufferProvider.fetchData();
            return state.isDone() ? state.get() : null;
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    /** Check if the combination of sample rate, channel count and audio format is supported. */
    public static boolean isSettingsSupported(int sampleRate, int channelCount, int audioFormat) {
        return AudioStreamImpl.isSettingsSupported(sampleRate, channelCount, audioFormat);
    }

    private static long getCurrentSystemTimeNs() {
        return System.nanoTime();
    }

    /**
     * The callback for receiving the audio source status.
     */
    public interface AudioSourceCallback {

        /**
         * The method called when the audio source suspend state changed.
         *
         * <p>One case where the audio source goes into suspend state is when it is started but the
         * {@link BufferProvider} is in {@link BufferProvider.State#INACTIVE} state.
         */
        @VisibleForTesting
        default void onSuspendStateChanged(boolean suspended) {
        }

        /**
         * The method called when the audio source silence state changed.
         *
         * <p>The audio source is silenced when the audio record is occupied by privilege
         * application. When it happens, the audio source will keep providing audio data with
         * silence sample.
         */
        void onSilenceStateChanged(boolean silenced);

        /**
         * The method called when the audio source encountered errors.
         */
        void onError(@NonNull Throwable t);

        /**
         * The method called to retrieve audio amplitude values.
         */
        void onAmplitudeValue(double maxAmplitude);
    }
}
