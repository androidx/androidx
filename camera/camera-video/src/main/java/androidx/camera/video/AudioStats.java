/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The audio information about an {@link Recording} at a point in time.
 *
 * <p>The audio information will be contained in every {@link RecordingStats}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class AudioStats {

    // Restrict to package-private.
    AudioStats() {

    }

    @NonNull
    static AudioStats of(@AudioState int state, @Nullable Throwable errorCause,
            double audioAmplitude) {
        return new AutoValue_AudioStats(state, audioAmplitude, errorCause);
    }

    /**
     * The recording is being recorded with audio data.
     *
     * <p>When audio is active, the recorded video file will contain audio data.
     */
    public static final int AUDIO_STATE_ACTIVE = 0;
    /**
     * The recording of audio is disabled.
     *
     * <p>This audio state results from a {@link PendingRecording} that was
     * {@linkplain
     * PendingRecording#start(java.util.concurrent.Executor, androidx.core.util.Consumer) started}
     * without calling {@link PendingRecording#withAudioEnabled()}.
     */
    public static final int AUDIO_STATE_DISABLED = 1;
    /**
     * The recording is muted because the audio source is silenced by the system.
     *
     * <p>If the audio source is occupied by a privilege application, such as the dialer,
     * depending on the system version, the system may silence the application that are using the
     * audio source. Use {@link #getErrorCause()} to get the error cause.
     */
    public static final int AUDIO_STATE_SOURCE_SILENCED = 2;
    /**
     * The recording is muted because the audio encoder encountered errors.
     *
     * <p>If the audio source encounters errors during recording, audio stats generated after the
     * error will contain this audio state, and the recording will proceed without audio.
     *
     * <p>Use {@link #getErrorCause()} to get the error cause.
     */
    public static final int AUDIO_STATE_ENCODER_ERROR = 3;

    /**
     *  The recording is muted because the audio source encountered errors.
     *
     * <p>If the audio source encounters errors during recording, audio stats generated after the
     * error will contain this audio state, and the recording will proceed without audio.
     *
     * <p>Use {@link #getErrorCause()} to get the error cause.
     */
    public static final int AUDIO_STATE_SOURCE_ERROR = 4;

    /**
     * The recording is muted by {@link Recording#mute(boolean)}.
     */
    public static final int AUDIO_STATE_MUTED = 5;

    /**
     * Should audio recording be disabled, any attempts to retrieve the amplitude will
     * return this value.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final double AUDIO_AMPLITUDE_NONE = 0;

    @IntDef({AUDIO_STATE_ACTIVE, AUDIO_STATE_DISABLED, AUDIO_STATE_SOURCE_SILENCED,
            AUDIO_STATE_ENCODER_ERROR, AUDIO_STATE_SOURCE_ERROR, AUDIO_STATE_MUTED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface AudioState {
    }

    /**
     * The subset of states considered error states.
     *
     * <p>{@link #hasError()} returns {@code true} if the current state is one of the error states.
     */
    private static final Set<Integer> ERROR_STATES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(AUDIO_STATE_SOURCE_SILENCED,
                    AUDIO_STATE_ENCODER_ERROR, AUDIO_STATE_SOURCE_ERROR)));

    /**
     * Indicates whether the recording is being recorded with audio.
     */
    public boolean hasAudio() {
        return getAudioState() == AUDIO_STATE_ACTIVE;
    }

    /**
     * Indicates whether an error occurred.
     *
     * <p>Returns {@code true} if the audio is muted due to unexpected error like audio source is
     * occupied or audio encoder encountered errors, otherwise {@code false}.
     */
    public boolean hasError() {
        return ERROR_STATES.contains(getAudioState());
    }

    /**
     * Returns the state of audio in the recording.
     *
     * <p>The audio state describes whether audio is enabled for the recording and if audio is
     * currently recording or is silenced due to system priority or errors.
     *
     * @return The state of the audio at the time of these audio stats being generated. One of
     * {@link #AUDIO_STATE_ACTIVE}, {@link #AUDIO_STATE_DISABLED},
     * {@link #AUDIO_STATE_SOURCE_SILENCED}, or {@link #AUDIO_STATE_ENCODER_ERROR}.
     */
    @AudioState
    public abstract int getAudioState();

    /**
     * Returns the average amplitude of the most recent audio samples.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    abstract double getAudioAmplitudeInternal();

    /**
     * Gets the error cause.
     *
     * <p>Returns {@code null} if {@link #hasError()} returns {@code false}.
     */
    @Nullable
    public abstract Throwable getErrorCause();

    /**
     * Returns the maximum absolute amplitude of the audio most recently sampled. Returns
     * {@link #AUDIO_AMPLITUDE_NONE} if audio is disabled.
     *
     * <p>The amplitude is the maximum absolute value over all channels which the audio was
     * most recently sampled from.
     *
     * <p>Amplitude is a relative measure of the maximum sound pressure/voltage range of the device
     * microphone.
     *
     * <p>The amplitude value returned will be a double between {@code 0} and {@code 1}.
     */
    @OptIn(markerClass = ExperimentalAudioApi.class)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public double getAudioAmplitude() {
        if (getAudioState() == AUDIO_STATE_DISABLED) {
            return AUDIO_AMPLITUDE_NONE;
        } else {
            return getAudioAmplitudeInternal();
        }
    }
}
