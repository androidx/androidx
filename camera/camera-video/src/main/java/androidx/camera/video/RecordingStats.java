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
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

/**
 * RecordingStats keeps track of the current recording’s statistics. It is a snapshot of things
 * like recorded duration and recorded file size.
 */
@AutoValue
public abstract class RecordingStats {

    public static final RecordingStats EMPTY_STATS = of(0, 0);

    @NonNull
    static RecordingStats of(long duration, long bytes) {
        Preconditions.checkArgument(duration >= 0, "duration must be positive value.");
        Preconditions.checkArgument(bytes >= 0, "bytes must be positive value.");
        return new AutoValue_RecordingStats(duration, bytes);
    }

    /** Returns current recorded duration in nano seconds. */
    public abstract long getRecordedDurationNs();

    /** Returns current recorded bytes. */
    public abstract long getNumBytesRecorded();
}
