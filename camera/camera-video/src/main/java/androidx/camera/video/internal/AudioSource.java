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
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.AudioTimestamp;
import android.os.Build;

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
import androidx.camera.video.internal.compat.Api24Impl;
import androidx.camera.video.internal.compat.Api29Impl;
import androidx.camera.video.internal.encoder.InputBuffer;
import androidx.core.util.Preconditions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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

    private AudioManager.AudioRecordingCallback mAudioRecordingCallback;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AtomicBoolean mSourceSilence = new AtomicBoolean(false);

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
    Executor mCallbackExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AudioSourceCallback mAudioSourceCallback;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    AudioSource(@NonNull Executor executor,
            @NonNull BufferProvider<InputBuffer> bufferProvider,
            int audioSource,
            int sampleRate,
            int channelCount,
            int audioFormat)
            throws AudioSourceAccessException {
        int minBufferSize = getMinBufferSize(sampleRate, channelCount, audioFormat);
        // The minBufferSize should be a positive value since the settings had already been checked
        // by the isSettingsSupported().
        Preconditions.checkState(minBufferSize > 0);

        mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        mBufferProvider = bufferProvider;
        mBufferSize = minBufferSize * 2;
        try {
            mAudioRecord = new AudioRecord(audioSource,
                    sampleRate,
                    channelCountToChannelConfig(channelCount),
                    audioFormat,
                    mBufferSize);
        } catch (IllegalArgumentException e) {
            throw new AudioSourceAccessException("Unable to create AudioRecord", e);
        }

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            mAudioRecord.release();
            throw new AudioSourceAccessException("Unable to initialize AudioRecord");
        }

        if (Build.VERSION.SDK_INT >= 29) {
            mAudioRecordingCallback = new AudioRecordingApi29Callback();
            Api29Impl.registerAudioRecordingCallback(mAudioRecord, mExecutor,
                    mAudioRecordingCallback);
        }

        mBufferProvider.addObserver(mExecutor, mStateObserver);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @RequiresApi(29)
    class AudioRecordingApi29Callback extends AudioManager.AudioRecordingCallback {
        @Override
        public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
            super.onRecordingConfigChanged(configs);
            if (mCallbackExecutor != null && mAudioSourceCallback != null) {
                for (AudioRecordingConfiguration config : configs) {
                    if (Api24Impl.getClientAudioSessionId(config)
                            == mAudioRecord.getAudioSessionId()) {
                        boolean isSilenced = Api29Impl.isClientSilenced(config);
                        if (mSourceSilence.getAndSet(isSilenced) != isSilenced) {
                            mCallbackExecutor.execute(
                                    () -> mAudioSourceCallback.onSilenced(isSilenced));
                        }
                        break;
                    }
                }
            }
        }
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
                    // Fall-through
                case CONFIGURED:
                    mBufferProvider.removeObserver(mStateObserver);
                    if (Build.VERSION.SDK_INT >= 29) {
                        Api29Impl.unregisterAudioRecordingCallback(mAudioRecord,
                                mAudioRecordingCallback);
                    }
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
                    throw new IllegalStateException("The audio recording callback must be "
                            + "registered before the audio source is started.");
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void notifyError(Throwable throwable) {
        if (mCallbackExecutor != null && mAudioSourceCallback != null) {
            mCallbackExecutor.execute(() -> mAudioSourceCallback.onError(throwable));
        }
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
            setState(CONFIGURED);
            notifyError(new AudioSourceAccessException("Unable to start the audio record.", e));
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
            notifyError(e);
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
                    notifyError(throwable);
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
                public void onError(@NonNull Throwable throwable) {
                    notifyError(throwable);
                }
            };

    /** Check if the combination of sample rate, channel count and audio format is supported. */
    public static boolean isSettingsSupported(int sampleRate, int channelCount, int audioFormat) {
        if (sampleRate <= 0 || channelCount <= 0) {
            return false;
        }
        return getMinBufferSize(sampleRate, channelCount, audioFormat) > 0;
    }

    private static int channelCountToChannelConfig(int channelCount) {
        return channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
    }

    private static int getMinBufferSize(int sampleRate, int channelCount, int audioFormat) {
        return AudioRecord.getMinBufferSize(sampleRate, channelCountToChannelConfig(channelCount),
                audioFormat);
    }

    /**
     * The builder of the AudioSource.
     */
    public static class Builder {
        private Executor mExecutor;
        private int mAudioSource = -1;
        private int mSampleRate = -1;
        private int mChannelCount = -1;
        private int mAudioFormat = -1;
        private BufferProvider<InputBuffer> mBufferProvider;

        /** Sets the executor to run the background task. */
        @NonNull
        public Builder setExecutor(@NonNull Executor executor) {
            mExecutor = Preconditions.checkNotNull(executor);
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

        /**
         * Sets the audio sample rate.
         *
         * <p>It has to ensure the combination of sample rate, channel count and audio format is
         * supported by {@link AudioSource#isSettingsSupported(int, int, int)}.
         *
         * @throws IllegalArgumentException if the sample rate is not positive.
         */
        @NonNull
        public Builder setSampleRate(int sampleRate) {
            Preconditions.checkArgument(sampleRate > 0);
            mSampleRate = sampleRate;
            return this;
        }

        /**
         * Sets the channel count.
         *
         * <p>It has to ensure the combination of sample rate, channel count and audio format is
         * supported by {@link AudioSource#isSettingsSupported(int, int, int)}.
         *
         * @throws IllegalArgumentException if the channel count is not positive.
         */
        @NonNull
        public Builder setChannelCount(int channelCount) {
            Preconditions.checkArgument(channelCount > 0);
            mChannelCount = channelCount;
            return this;
        }

        /**
         * Sets the audio format.
         *
         * <p>It has to ensure the combination of sample rate, channel count and audio format is
         * supported by {@link AudioSource#isSettingsSupported(int, int, int)}.
         *
         * @see AudioFormat#ENCODING_PCM_16BIT
         */
        @NonNull
        public Builder setAudioFormat(int audioFormat) {
            mAudioFormat = audioFormat;
            return this;
        }

        /** Sets the {@link BufferProvider}. */
        @NonNull
        public Builder setBufferProvider(@NonNull BufferProvider<InputBuffer> bufferProvider) {
            mBufferProvider = Preconditions.checkNotNull(bufferProvider);
            return this;
        }

        /** Build the AudioSource. */
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        @NonNull
        public AudioSource build() throws AudioSourceAccessException {
            String missing = "";
            if (mExecutor == null) {
                missing += " executor";
            }
            if (mBufferProvider == null) {
                missing += " bufferProvider";
            }
            if (mAudioSource == -1) {
                missing += " audioSource";
            }
            if (mSampleRate == -1) {
                missing += " sampleRate";
            }
            if (mChannelCount == -1) {
                missing += " channelCount";
            }
            if (mAudioFormat == -1) {
                missing += " audioFormat";
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing required properties:" + missing);
            }
            if (!isSettingsSupported(mSampleRate, mChannelCount, mAudioFormat)) {
                throw new IllegalStateException(String.format("The combination of sample rate %d "
                                + ", channel count %d and audio format %d is not supported.",
                        mSampleRate, mChannelCount, mAudioFormat));
            }
            return new AudioSource(mExecutor,
                    mBufferProvider,
                    mAudioSource,
                    mSampleRate,
                    mChannelCount,
                    mAudioFormat
            );
        }
    }

    /**
     * The callback for receiving the audio source status.
     */
    public interface AudioSourceCallback {
        /**
         * The method called when the audio source is silenced.
         *
         * <p>The audio source is silenced when the audio record is occupied by privilege
         * application. When it happens, the audio source will keep providing audio data with
         * silence sample.
         */
        void onSilenced(boolean silenced);

        /**
         * The method called when the audio source encountered errors.
         */
        void onError(@NonNull Throwable t);
    }
}
