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
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * A snapshot of statistics about an {@link ActiveRecording} at a point in time.
 *
 * <p>Recording stats provide information about a recording such as file size, duration and other
 * useful statistics which may be useful for tracking the state of a recording.
 *
 * <p>Recording stats are generated for every {@link VideoRecordEvent} and can be retrieved via
 * {@link VideoRecordEvent#getRecordingStats()}.
 * @see PendingRecording#withEventListener(Executor, Consumer)
 */
@AutoValue
public abstract class RecordingStats {

    /**
     * The recording is being recorded with audio data.
     *
     * <p>When audio is recording, the resulting video file will contain an audio track.
     */
    public static final int AUDIO_RECORDING = 0;
    /**
     * The recording is disabled.
     *
     * <p>This audio state results from a {@link PendingRecording} that was
     * {@linkplain PendingRecording#start() started} without calling
     * {@link PendingRecording#withAudioEnabled()}.
     */
    public static final int AUDIO_DISABLED = 1;
    /**
     * The recording is muted because the audio source is silenced by the system.
     *
     * <p>If the audio source is occupied by privilege application, depending on the system
     * version, the system may silence the application that are using the audio source.
     */
    public static final int AUDIO_SOURCE_SILENCED = 2;
    /**
     * The recording is muted because the audio encoder encountered errors.
     */
    public static final int AUDIO_ENCODER_ERROR = 3;

    /** @hide */
    @IntDef({AUDIO_RECORDING, AUDIO_DISABLED, AUDIO_SOURCE_SILENCED, AUDIO_ENCODER_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface AudioState {
    }

    // Restrict the constructor scope.
    RecordingStats() {
    }

    @NonNull
    static RecordingStats of(long duration, long bytes, @AudioState int audioState) {
        Preconditions.checkArgument(duration >= 0, "duration must be positive value.");
        Preconditions.checkArgument(bytes >= 0, "bytes must be positive value.");
        return new AutoValue_RecordingStats(duration, bytes, audioState);
    }

    /**
     * Returns current recorded duration in nanoseconds.
     *
     * <p>The duration represents the realtime number of nanoseconds that have transpired since
     * the recording started, excluding intervals where the recording was paused.
     * @return the duration, in nanoseconds, of the recording at the time of these recording stats
     * being generated.
     */
    public abstract long getRecordedDurationNanos();

    /**
     * Returns the number of bytes recorded.
     *
     * <p>The number of bytes recorded includes bytes stored for video and for audio, if applicable.
     * @return the total number of bytes stored for the recording at the time of these recording
     * stats being generated.
     */
    public abstract long getNumBytesRecorded();

    /**
     * Returns the state of audio in the recording.
     *
     * <p>The audio state describes whether audio is enabled for the recording and if audio is
     * currently recording or is silenced due to system priority or errors.
     *
     * @return The state of the audio at the time of these recording stats being generated. One of
     * {@link #AUDIO_RECORDING}, {@link #AUDIO_DISABLED}, {@link #AUDIO_SOURCE_SILENCED}, or
     * {@link #AUDIO_ENCODER_ERROR}.
     */
    @AudioState
    public abstract int getAudioState();
}
