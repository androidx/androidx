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
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The audio information about an {@link ActiveRecording} at a point in time.
 *
 * <p>The audio information will be contained in every {@link RecordingStats}.
 *
 * @hide
 */
@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
@AutoValue
public abstract class AudioStats {

    // Restrict to package-private.
    AudioStats() {

    }

    @NonNull
    static AudioStats of(@AudioState int state, @Nullable Throwable errorCause) {
        return new AutoValue_AudioStats(state, errorCause);
    }

    /**
     * The recording is being recorded with audio data.
     *
     * <p>When audio is active, the recorded video file will contain audio data.
     */
    public static final int AUDIO_STATE_ACTIVE = 0;
    /**
     * The recording is disabled.
     *
     * <p>This audio state results from a {@link PendingRecording} that was
     * {@linkplain PendingRecording#start() started} without calling
     * {@link PendingRecording#withAudioEnabled()}.
     */
    public static final int AUDIO_STATE_DISABLED = 1;
    /**
     * The recording is muted because the audio source is silenced by the system.
     *
     * <p>If the audio source is occupied by privilege application, depending on the system
     * version, the system may silence the application that are using the audio source. Use
     * {@link #getErrorCause()} to get the error cause.
     */
    public static final int AUDIO_STATE_SOURCE_SILENCED = 2;
    /**
     * The recording is muted because the audio encoder encountered errors.
     *
     * <p>Use {@link #getErrorCause()} to get the error cause.
     */
    public static final int AUDIO_STATE_ENCODER_ERROR = 3;

    /** @hide */
    @IntDef({AUDIO_STATE_ACTIVE, AUDIO_STATE_DISABLED, AUDIO_STATE_SOURCE_SILENCED,
            AUDIO_STATE_ENCODER_ERROR})
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
                    AUDIO_STATE_ENCODER_ERROR)));

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
     * Gets the error cause.
     *
     * <p>Returns {@code null} if {@link #hasError()} returns {@code false}.
     */
    @Nullable
    public abstract Throwable getErrorCause();
}
