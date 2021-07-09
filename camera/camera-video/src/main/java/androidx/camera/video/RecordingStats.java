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

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

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

    // Restrict the constructor scope.
    RecordingStats() {
    }

    @NonNull
    static RecordingStats of(long duration, long bytes, @NonNull AudioStats audioStats) {
        Preconditions.checkArgument(duration >= 0, "duration must be positive value.");
        Preconditions.checkArgument(bytes >= 0, "bytes must be positive value.");
        return new AutoValue_RecordingStats(duration, bytes, audioStats);
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
     * Returns the {@link AudioStats} that is associated with this recording stats.
     */
    @NonNull
    public abstract AudioStats getAudioStats();
}
