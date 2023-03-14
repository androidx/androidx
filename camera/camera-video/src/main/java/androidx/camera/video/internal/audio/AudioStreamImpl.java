/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.camera.video.internal.audio.AudioUtils.channelCountToChannelConfig;
import static androidx.camera.video.internal.audio.AudioUtils.channelCountToChannelMask;
import static androidx.camera.video.internal.audio.AudioUtils.frameCountToDurationNs;
import static androidx.camera.video.internal.audio.AudioUtils.sizeToFrameCount;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import android.Manifest;
import android.content.Context;
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
import androidx.camera.video.internal.compat.Api23Impl;
import androidx.camera.video.internal.compat.Api24Impl;
import androidx.camera.video.internal.compat.Api29Impl;
import androidx.camera.video.internal.compat.Api31Impl;
import androidx.camera.video.internal.compat.quirk.AudioTimestampFramePositionIncorrectQuirk;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.core.util.Preconditions;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An AudioStream implementation backed by {@link AudioRecord}.
 *
 * <p>This class is not thread safe, it should be used on the same thread.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioStreamImpl implements AudioStream {
    private static final String TAG = "AudioStreamImpl";

    final AudioRecord mAudioRecord;
    private final AudioSettings mSettings;
    private final AtomicBoolean mIsReleased = new AtomicBoolean(false);
    private final AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private final AtomicReference<Boolean> mNotifiedSilenceState = new AtomicReference<>(null);
    private final int mBufferSize;
    private final int mBytesPerFrame;
    @Nullable
    private AudioStreamCallback mAudioStreamCallback;
    @Nullable
    private Executor mCallbackExecutor;
    private long mTotalFramesRead;
    @Nullable
    private AudioManager.AudioRecordingCallback mAudioRecordingCallback;

    /**
     * Creates an AudioStreamImpl for the given settings.
     *
     * <p>It should be verified the combination of sample rate, channel count and audio format is
     * supported with {@link #isSettingsSupported(int, int, int)} before passing the settings to
     * this constructor, or an {@link UnsupportedOperationException} will be thrown.
     *
     * @param settings           The settings that will be used to configure the audio stream.
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
     * @throws AudioStreamException          if the audio device is not available or cannot be
     *                                       initialized with the given settings.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public AudioStreamImpl(@NonNull AudioSettings settings, @Nullable Context attributionContext)
            throws IllegalArgumentException, AudioStreamException {
        if (!isSettingsSupported(settings.getSampleRate(), settings.getChannelCount(),
                settings.getAudioFormat())) {
            throw new UnsupportedOperationException(String.format(
                    "The combination of sample rate %d, channel count %d and audio format"
                            + " %d is not supported.",
                    settings.getSampleRate(), settings.getChannelCount(),
                    settings.getAudioFormat()));
        }

        mSettings = settings;
        mBytesPerFrame = settings.getBytesPerFrame();

        int minBufferSize = getMinBufferSize(settings.getSampleRate(), settings.getChannelCount(),
                settings.getAudioFormat());
        // The minBufferSize should be a positive value since the settings had already been checked
        // by the isSettingsSupported().
        Preconditions.checkState(minBufferSize > 0);
        mBufferSize = minBufferSize * 2;

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

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            mAudioRecord.release();
            throw new AudioStreamException("Unable to initialize AudioRecord");
        }
    }

    @Override
    public void start() throws AudioStreamException {
        checkNotReleasedOrThrow();
        if (mIsStarted.getAndSet(true)) {
            return;
        }
        mAudioRecord.startRecording();
        if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            mIsStarted.set(false);
            throw new AudioStreamException("Unable to start AudioRecord with state: "
                    + mAudioRecord.getRecordingState());
        }
        mTotalFramesRead = 0;
        mNotifiedSilenceState.set(null);
        boolean isSilenced = false;
        if (Build.VERSION.SDK_INT >= 29) {
            AudioRecordingConfiguration config = Api29Impl.getActiveRecordingConfiguration(
                    mAudioRecord);
            isSilenced = config != null && Api29Impl.isClientSilenced(config);
        }
        notifySilenced(isSilenced);
    }

    @Override
    public void stop() {
        checkNotReleasedOrThrow();
        if (!mIsStarted.getAndSet(false)) {
            return;
        }
        mAudioRecord.stop();
        if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
            Logger.w(TAG, "Failed to stop AudioRecord with state: "
                    + mAudioRecord.getRecordingState());
        }
    }

    @Override
    public void release() {
        if (mIsReleased.getAndSet(true)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 29 && mAudioRecordingCallback != null) {
            Api29Impl.unregisterAudioRecordingCallback(mAudioRecord, mAudioRecordingCallback);
        }
        mAudioRecord.release();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads audio data from the audio hardware for recording into a direct buffer. If this
     * buffer is not a direct buffer, this method will always return a {@link PacketInfo} with zero
     * size.
     *
     * @param byteBuffer the buffer to which the audio data is written.
     * @return the retrieved information by this read operation.
     *
     * @throws IllegalStateException if the stream has not been started or has been released.
     */
    @NonNull
    @Override
    public PacketInfo read(@NonNull ByteBuffer byteBuffer) {
        checkNotReleasedOrThrow();
        checkStartedOrThrow();

        int sizeInBytes = mAudioRecord.read(byteBuffer, mBufferSize);
        long timestampNs = 0;
        if (sizeInBytes > 0) {
            byteBuffer.limit(sizeInBytes);
            timestampNs = generatePresentationTimeNs();
            mTotalFramesRead += sizeToFrameCount(sizeInBytes, mBytesPerFrame);
        }
        return PacketInfo.of(sizeInBytes, timestampNs);
    }

    @Override
    public void setCallback(@Nullable AudioStreamCallback callback, @Nullable Executor executor) {
        checkState(!mIsStarted.get(), "AudioStream can not be started when setCallback.");
        checkNotReleasedOrThrow();
        checkArgument(callback == null || executor != null,
                "executor can't be null with non-null callback.");
        mAudioStreamCallback = callback;
        mCallbackExecutor = executor;
        if (Build.VERSION.SDK_INT >= 29) {
            if (mAudioRecordingCallback != null) {
                Api29Impl.unregisterAudioRecordingCallback(mAudioRecord, mAudioRecordingCallback);
            }
            if (callback == null) {
                return;
            }
            if (mAudioRecordingCallback == null) {
                mAudioRecordingCallback = new AudioRecordingApi29Callback();
            }
            Api29Impl.registerAudioRecordingCallback(mAudioRecord, executor,
                    mAudioRecordingCallback);
        }
    }

    void notifySilenced(boolean isSilenced) {
        Executor executor = mCallbackExecutor;
        AudioStreamCallback callback = mAudioStreamCallback;
        if (executor != null && callback != null) {
            if (!Objects.equals(mNotifiedSilenceState.getAndSet(isSilenced), isSilenced)) {
                executor.execute(() -> callback.onSilenceStateChanged(isSilenced));
            }
        }
    }

    private long generatePresentationTimeNs() {
        long presentationTimeNs = -1;
        if (Build.VERSION.SDK_INT >= 24 && !hasAudioTimestampQuirk()) {
            AudioTimestamp audioTimestamp = new AudioTimestamp();
            if (Api24Impl.getTimestamp(mAudioRecord, audioTimestamp,
                    AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS) {
                presentationTimeNs = computeInterpolatedTimeNs(mSettings.getSampleRate(),
                        mTotalFramesRead, audioTimestamp);
            } else {
                Logger.w(TAG, "Unable to get audio timestamp");
            }
        }
        if (presentationTimeNs == -1) {
            presentationTimeNs = System.nanoTime();
        }
        return presentationTimeNs;
    }

    private void checkNotReleasedOrThrow() {
        checkState(!mIsReleased.get(), "AudioStream has been released.");
    }

    private void checkStartedOrThrow() {
        checkState(mIsStarted.get(), "AudioStream has not been started.");
    }

    /** Check if the combination of sample rate, channel count and audio format is supported. */
    public static boolean isSettingsSupported(int sampleRate, int channelCount, int audioFormat) {
        if (sampleRate <= 0 || channelCount <= 0) {
            return false;
        }
        return getMinBufferSize(sampleRate, channelCount, audioFormat) > 0;
    }

    private static boolean hasAudioTimestampQuirk() {
        return DeviceQuirks.get(AudioTimestampFramePositionIncorrectQuirk.class) != null;
    }

    private static long computeInterpolatedTimeNs(int sampleRate, long framePosition,
            @NonNull AudioTimestamp timestamp) {
        long frameDiff = framePosition - timestamp.framePosition;
        long compensateTimeInNanoSec = frameCountToDurationNs(frameDiff, sampleRate);
        long resultInNanoSec = timestamp.nanoTime + compensateTimeInNanoSec;
        return resultInNanoSec < 0 ? 0 : resultInNanoSec;
    }

    private static int getMinBufferSize(int sampleRate, int channelCount, int audioFormat) {
        return AudioRecord.getMinBufferSize(sampleRate, channelCountToChannelConfig(channelCount),
                audioFormat);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @RequiresApi(29)
    class AudioRecordingApi29Callback extends AudioManager.AudioRecordingCallback {
        @Override
        public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
            for (AudioRecordingConfiguration config : configs) {
                if (Api24Impl.getClientAudioSessionId(config) == mAudioRecord.getAudioSessionId()) {
                    boolean isSilenced = Api29Impl.isClientSilenced(config);
                    notifySilenced(isSilenced);
                    break;
                }
            }
        }
    }
}
