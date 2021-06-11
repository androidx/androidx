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
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * RecordingStats keeps track of the current recording’s statistics. It is a snapshot of things
 * like recorded duration and recorded file size.
 */
@AutoValue
public abstract class RecordingStats {

    /**
     * The recording is being recorded with audio data.
     */
    public static final int AUDIO_RECORDING = 0;
    /**
     * The recording is disabled.
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

    /** Returns current recorded duration in nano seconds. */
    public abstract long getRecordedDurationNanos();

    /** Returns current recorded bytes. */
    public abstract long getNumBytesRecorded();

    /** Returns current audio state. */
    @AudioState
    public abstract int getAudioState();
}
