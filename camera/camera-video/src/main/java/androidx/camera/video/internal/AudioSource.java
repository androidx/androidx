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
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.AudioTimestamp;
import android.os.Build;

import androidx.annotation.IntRange;
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
import androidx.camera.video.internal.compat.Api23Impl;
import androidx.camera.video.internal.compat.Api24Impl;
import androidx.camera.video.internal.compat.Api29Impl;
import androidx.camera.video.internal.compat.Api31Impl;
import androidx.camera.video.internal.encoder.InputBuffer;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

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
 * specified with {@link Settings.Builder#setAudioSource(int)}.
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

    // The following should only be accessed by mExecutor
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    BufferProvider<InputBuffer> mBufferProvider;
    private FutureCallback<InputBuffer> mAcquireBufferCallback;
    private Observable.Observer<BufferProvider.State> mStateObserver;


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
    public AudioSource(@NonNull Settings settings, @NonNull Executor executor,
            @Nullable Context attributionContext)
            throws AudioSourceAccessException {
        if (!isSettingsSupported(settings.getSampleRate(), settings.getChannelCount(),
                settings.getAudioFormat())) {
            throw new UnsupportedOperationException(String.format(
                    "The combination of sample rate %d, channel count %d and audio format"
                            + " %d is not supported.",
                    settings.getSampleRate(), settings.getChannelCount(),
                    settings.getAudioFormat()));
        }

        int minBufferSize = getMinBufferSize(settings.getSampleRate(), settings.getChannelCount(),
                settings.getAudioFormat());
        // The minBufferSize should be a positive value since the settings had already been checked
        // by the isSettingsSupported().
        Preconditions.checkState(minBufferSize > 0);

        mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        mBufferSize = minBufferSize * 2;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                AudioFormat audioFormatObj = new AudioFormat.Builder()
                        .setSampleRate(settings.getSampleRate())
                        .setChannelMask(channelCountToChannelMask(settings.getChannelCount()))
                        .setEncoding(settings.getAudioFormat())
                        .build();
                AudioRecord.Builder audioRecordBuilder = Api23Impl.createAudioRecordBuilder();
                if (Build.VERSION.SDK_INT >= 31 && attributionContext != null) {
                    Api31Impl.setContext(audioRecordBuilder, attributionContext);
                }
                Api23Impl.setAudioSource(audioRecordBuilder, settings.getAudioSource());
                Api23Impl.setAudioFormat(audioRecordBuilder, audioFormatObj);
                Api23Impl.setBufferSizeInBytes(audioRecordBuilder, mBufferSize);
                mAudioRecord = Api23Impl.build(audioRecordBuilder);
            } else {
                mAudioRecord = new AudioRecord(settings.getAudioSource(),
                        settings.getSampleRate(),
                        channelCountToChannelConfig(settings.getChannelCount()),
                        settings.getAudioFormat(),
                        mBufferSize);
            }
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
     * Sets the {@link BufferProvider}.
     *
     * <p>A buffer provider is required to stream audio. If no buffer provider is provided, then
     * audio will be dropped until one is provided and active.
     *
     * @param bufferProvider The new buffer provider to use.
     */
    public void setBufferProvider(@NonNull BufferProvider<InputBuffer> bufferProvider) {
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
                    throw new IllegalStateException("AudioRecorder is released");
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
                    resetBufferProvider(null);
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

    @ExecutedBy("mExecutor")
    private void resetBufferProvider(@Nullable BufferProvider<InputBuffer> bufferProvider) {
        if (mBufferProvider != null) {
            mBufferProvider.removeObserver(mStateObserver);
            mBufferProvider = null;
            mStateObserver = null;
            mAcquireBufferCallback = null;
        }
        mBufferProviderState = BufferProvider.State.INACTIVE;
        updateSendingAudio();
        if (bufferProvider != null) {
            mBufferProvider = bufferProvider;
            mStateObserver = new Observable.Observer<BufferProvider.State>() {
                @ExecutedBy("mExecutor")
                @Override
                public void onNewData(@Nullable BufferProvider.State state) {
                    if (mBufferProvider == bufferProvider) {
                        Logger.d(TAG, "Receive BufferProvider state change: "
                                + mBufferProviderState + " to " + state);
                        mBufferProviderState = state;
                        updateSendingAudio();
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
                    if (mBufferProvider != bufferProvider) {
                        Logger.d(TAG, "Unable to get input buffer, the BufferProvider "
                                + "could be transitioning to INACTIVE state.");
                        notifyError(throwable);
                    }
                }
            };
            mBufferProvider.addObserver(mExecutor, mStateObserver);
        }
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

    private static int channelCountToChannelMask(int channelCount) {
        // Currently equivalent to channelCountToChannelConfig, but keep this logic separate
        // since technically channel masks are different from the legacy channel config and we don't
        // want any future updates to break things.
        return channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
    }

    private static int getMinBufferSize(int sampleRate, int channelCount, int audioFormat) {
        return AudioRecord.getMinBufferSize(sampleRate, channelCountToChannelConfig(channelCount),
                audioFormat);
    }

    /**
     * Settings required to configure the audio source.
     */
    @AutoValue
    public abstract static class Settings {

        /** Creates a builder for these settings. */
        @SuppressLint("Range") // Need to initialize as invalid values
        @NonNull
        public static Settings.Builder builder() {
            return new AutoValue_AudioSource_Settings.Builder()
                    .setAudioSource(-1)
                    .setSampleRate(-1)
                    .setChannelCount(-1)
                    .setAudioFormat(-1);
        }

        /** Creates a {@link Builder} initialized with the same settings as this instance. */
        @NonNull
        public abstract Builder toBuilder();

        /**
         * Gets the device audio source.
         *
         * @see android.media.MediaRecorder.AudioSource#MIC
         * @see android.media.MediaRecorder.AudioSource#CAMCORDER
         */
        public abstract int getAudioSource();

        /**
         * Gets the audio sample rate.
         */
        @IntRange(from = 1)
        public abstract int getSampleRate();

        /**
         * Gets the channel count.
         */
        @IntRange(from = 1)
        public abstract int getChannelCount();

        /**
         * Sets the audio format.
         *
         * @see AudioFormat#ENCODING_PCM_16BIT
         */
        public abstract int getAudioFormat();

        // Should not be instantiated directly
        Settings() {
        }

        /**
         * A Builder for {@link AudioSource.Settings}
         */
        @AutoValue.Builder
        public abstract static class Builder {
            /**
             * Sets the device audio source.
             *
             * @see android.media.MediaRecorder.AudioSource#MIC
             * @see android.media.MediaRecorder.AudioSource#CAMCORDER
             */
            @NonNull
            public abstract Builder setAudioSource(int audioSource);

            /**
             * Sets the audio sample rate in Hertz.
             */
            @NonNull
            public abstract Builder setSampleRate(@IntRange(from = 1) int sampleRate);

            /**
             * Sets the channel count.
             */
            @NonNull
            public abstract Builder setChannelCount(@IntRange(from = 1) int channelCount);

            /**
             * Sets the audio format.
             *
             * @see AudioFormat#ENCODING_PCM_16BIT
             */
            @NonNull
            public abstract Builder setAudioFormat(int audioFormat);

            abstract Settings autoBuild(); // Actual build method. Not public.

            /**
             * Returns the built config after performing settings validation.
             *
             * <p>It should be verified that combination of sample rate, channel count and audio
             * format is supported by {@link AudioSource#isSettingsSupported(int, int, int)} or
             * an {@link UnsupportedOperationException} will be thrown when passing the settings
             * to the
             * {@linkplain AudioSource#AudioSource(Settings, Executor, Context) AudioSource
             * constructor}.
             *
             * @throws IllegalArgumentException if a setting is missing or invalid.
             */
            @NonNull
            public final Settings build() {
                Settings settings = autoBuild();
                String missingOrInvalid = "";
                if (settings.getAudioSource() == -1) {
                    missingOrInvalid += " audioSource";
                }
                if (settings.getSampleRate() <= 0) {
                    missingOrInvalid += " sampleRate";
                }
                if (settings.getChannelCount() <= 0) {
                    missingOrInvalid += " channelCount";
                }
                if (settings.getAudioFormat() == -1) {
                    missingOrInvalid += " audioFormat";
                }

                if (!missingOrInvalid.isEmpty()) {
                    throw new IllegalArgumentException("Required settings missing or "
                            + "non-positive:" + missingOrInvalid);
                }

                return settings;
            }

            // Should not be instantiated directly
            Builder() {
            }
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
