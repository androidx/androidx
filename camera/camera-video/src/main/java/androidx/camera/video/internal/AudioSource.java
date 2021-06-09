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

package androidx.camera.video.internal;

import static androidx.camera.video.internal.AudioSource.InternalState.CONFIGURED;
import static androidx.camera.video.internal.AudioSource.InternalState.RELEASED;
import static androidx.camera.video.internal.AudioSource.InternalState.STARTED;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.internal.encoder.InputBuffer;
import androidx.core.util.Preconditions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * AudioSource is used to obtain audio raw data and write to the buffer from {@link BufferProvider}.
 *
 * <p>The audio raw data could be one of sources from the device. The target source can be
 * specified with {@link Builder#setAudioSource(int)}.
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
public final class AudioSource {
    private static final String TAG = "AudioSource";
    // Common sample rate options to choose from in descending order.
    public static final List<Integer> COMMON_SAMPLE_RATES = Collections.unmodifiableList(
            Arrays.asList(48000, 44100, 22050, 11025, 8000, 4800));

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

    private final BufferProvider<InputBuffer> mBufferProvider;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AudioRecord mAudioRecord;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final int mBufferSize;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    InternalState mState = CONFIGURED;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    BufferProvider.State mBufferProviderState = BufferProvider.State.INACTIVE;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mIsSendingAudio;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    AudioSource(@NonNull Executor executor,
            @NonNull BufferProvider<InputBuffer> bufferProvider,
            int audioSource,
            int sampleRate,
            int channelConfig,
            int audioFormat,
            int defaultBufferSize)
            throws AudioSourceAccessException {
        mExecutor = CameraXExecutors.newSequentialExecutor(Preconditions.checkNotNull(executor));
        mBufferProvider = Preconditions.checkNotNull(bufferProvider);

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (bufferSize <= 0) {
            bufferSize = defaultBufferSize;
        }
        mBufferSize = bufferSize * 2;
        try {
            mAudioRecord = new AudioRecord(audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    mBufferSize);
        } catch (IllegalArgumentException e) {
            throw new AudioSourceAccessException("Unable to create AudioRecord", e);
        }

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            mAudioRecord.release();
            throw new AudioSourceAccessException("Unable to initialize AudioRecord");
        }

        mBufferProvider.addObserver(mExecutor, mStateObserver);
    }

    /**
     * Starts the AudioSource.
     *
     * <p>Audio data will start being sent to the {@link BufferProvider} when
     * {@link BufferProvider}'s state is {@link BufferProvider.State#ACTIVE}.
     *
     * @throws IllegalStateException if the AudioSource is released.
     */
    public void start() {
        mExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                    setState(STARTED);
                    updateSendingAudio();
                    break;
                case STARTED:
                    // Do nothing
                    break;
                case RELEASED:
                    throw new IllegalStateException("AudioRecorder is released");
            }
        });
    }

    /**
     * Stops the AudioSource.
     *
     * <p>Audio data will stop being sent to the {@link BufferProvider}.
     *
     * @throws IllegalStateException if it is released.
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
                    throw new IllegalStateException("AudioRecorder is released");
            }
        });
    }

    /**
     * Releases the AudioSource.
     *
     * <p>Once the AudioSource is released, it can not be used any more.
     */
    public void release() {
        mExecutor.execute(() -> {
            switch (mState) {
                case STARTED:
                case CONFIGURED:
                    mBufferProvider.removeObserver(mStateObserver);
                    mAudioRecord.release();
                    stopSendingAudio();
                    setState(RELEASED);
                    break;
                case RELEASED:
                    // Do nothing
                    break;
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void updateSendingAudio() {
        if (mState == STARTED && mBufferProviderState == BufferProvider.State.ACTIVE) {
            startSendingAudio();
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
            mAudioRecord.startRecording();
            if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                throw new IllegalStateException("Unable to start AudioRecord with state: "
                                + mAudioRecord.getRecordingState());
            }
        } catch (IllegalStateException e) {
            Logger.w(TAG, "Failed to start AudioRecord", e);
            return;
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
        try {
            Logger.d(TAG, "stopSendingAudio");
            mAudioRecord.stop();
            if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                throw new IllegalStateException("Unable to stop AudioRecord with state: "
                        + mAudioRecord.getRecordingState());
            }
        } catch (IllegalStateException e) {
            Logger.w(TAG, "Failed to stop AudioRecord", e);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void sendNextAudio() {
        Futures.addCallback(mBufferProvider.acquireBuffer(), mAcquireBufferCallback, mExecutor);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void setState(InternalState state) {
        Logger.d(TAG, "Transitioning internal state: " + mState + " --> " + state);
        mState = state;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long generatePresentationTimeUs() {
        long presentationTimeUs = -1;
        if (Build.VERSION.SDK_INT >= 24) {
            AudioTimestamp audioTimestamp = new AudioTimestamp();
            if (Api24Impl.getTimestamp(mAudioRecord, audioTimestamp,
                    AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS) {
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(audioTimestamp.nanoTime);
            } else {
                Logger.w(TAG, "Unable to get audio timestamp");
            }
        }
        if (presentationTimeUs == -1) {
            presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
        }
        return presentationTimeUs;
    }

    private final FutureCallback<InputBuffer> mAcquireBufferCallback =
            new FutureCallback<InputBuffer>() {
                @ExecutedBy("mExecutor")
                @Override
                public void onSuccess(InputBuffer inputBuffer) {
                    if (!mIsSendingAudio) {
                        inputBuffer.cancel();
                        return;
                    }
                    ByteBuffer byteBuffer = inputBuffer.getByteBuffer();

                    int length = mAudioRecord.read(byteBuffer, mBufferSize);
                    if (length > 0) {
                        byteBuffer.limit(length);
                        inputBuffer.setPresentationTimeUs(generatePresentationTimeUs());
                        inputBuffer.submit();
                    } else {
                        Logger.w(TAG, "Unable to read data from AudioRecord.");
                        inputBuffer.cancel();
                    }
                    sendNextAudio();
                }

                @ExecutedBy("mExecutor")
                @Override
                public void onFailure(Throwable throwable) {
                    Logger.d(TAG, "Unable to get input buffer, the BufferProvider "
                            + "could be transitioning to INACTIVE state.");
                }
            };

    private final Observable.Observer<BufferProvider.State> mStateObserver =
            new Observable.Observer<BufferProvider.State>() {
                @ExecutedBy("mExecutor")
                @Override
                public void onNewData(@Nullable BufferProvider.State state) {
                    Logger.d(TAG, "Receive BufferProvider state change: "
                            + mBufferProviderState + " to " + state);
                    mBufferProviderState = state;
                    updateSendingAudio();
                }

                @ExecutedBy("mExecutor")
                @Override
                public void onError(@NonNull Throwable t) {
                    // Not define, should not be possible.
                }
            };

    /**
     * The builder of the AudioSource.
     */
    public static class Builder {
        private Executor mExecutor;
        private int mAudioSource;
        private int mSampleRate;
        private int mChannelConfig;
        private int mAudioFormat;
        private int mDefaultBufferSize;
        private BufferProvider<InputBuffer> mBufferProvider;

        /** Sets the executor to run the background task. */
        @NonNull
        public Builder setExecutor(@NonNull Executor executor) {
            mExecutor = executor;
            return this;
        }

        /**
         * Sets the device audio source.
         *
         * @see android.media.MediaRecorder.AudioSource#MIC
         * @see android.media.MediaRecorder.AudioSource#CAMCORDER
         */
        @NonNull
        public Builder setAudioSource(int audioSource) {
            mAudioSource = audioSource;
            return this;
        }

        /** Sets the audio sample rate. */
        @NonNull
        public Builder setSampleRate(int sampleRate) {
            mSampleRate = sampleRate;
            return this;
        }

        /**
         * Sets the channel config.
         *
         * @see AudioFormat#CHANNEL_IN_MONO
         * @see AudioFormat#CHANNEL_IN_STEREO
         */
        @NonNull
        public Builder setChannelConfig(int channelConfig) {
            mChannelConfig = channelConfig;
            return this;
        }

        /**
         * Sets the audio format.
         *
         * @see AudioFormat#ENCODING_PCM_8BIT
         * @see AudioFormat#ENCODING_PCM_16BIT
         * @see AudioFormat#ENCODING_PCM_FLOAT
         */
        @NonNull
        public Builder setAudioFormat(int audioFormat) {
            mAudioFormat = audioFormat;
            return this;
        }

        /**
         * Sets the default buffer size.
         *
         * <p>AudioSource will try to generate a buffer size. But if it is unable to get one,
         * it will apply this default buffer size.
         */
        @NonNull
        public Builder setDefaultBufferSize(int bufferSize) {
            mDefaultBufferSize = bufferSize;
            return this;
        }

        /** Sets the {@link BufferProvider}. */
        @NonNull
        public Builder setBufferProvider(@NonNull BufferProvider<InputBuffer> bufferProvider) {
            mBufferProvider = bufferProvider;
            return this;
        }

        /** Build the AudioSource. */
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        @NonNull
        public AudioSource build() throws AudioSourceAccessException {
            return new AudioSource(mExecutor,
                    mBufferProvider,
                    mAudioSource,
                    mSampleRate,
                    mChannelConfig,
                    mAudioFormat,
                    mDefaultBufferSize
            );
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 7.0 (API 24).
     */
    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
        }

        @DoNotInline
        static int getTimestamp(@NonNull AudioRecord audioRecord,
                @NonNull AudioTimestamp audioTimestamp, int timeBase) {
            return audioRecord.getTimestamp(audioTimestamp, timeBase);
        }
    }
}
