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

import java.util.concurrent.Executor;

/**
 * A recording that can be started at a future time.
 */
public final class PendingRecording extends Recording {

    PendingRecording(@NonNull Recorder recorder, @NonNull OutputOptions options) {
        super(recorder, options, null, null);
    }

    /**
     * Sets the event listener that will receive {@link VideoRecordEvent} for this recording.
     *
     * @param callbackExecutor the executor that the event listener will be run on.
     * @param listener the event listener to handle the video record event.
     * @return this pending recording
     */
    @NonNull
    public PendingRecording withEventListener(@NonNull Executor callbackExecutor,
            @NonNull Consumer<VideoRecordEvent> listener) {
        Preconditions.checkNotNull(callbackExecutor, "CallbackExecutor can't be null.");
        Preconditions.checkNotNull(listener, "Event listener can't be null");
        setCallbackExecutor(callbackExecutor);
        setEventListener(listener);
        return this;
    }

    /**
     * Starts the recording, making it an active recording.
     *
     * <p>Only a single recording can be active at a time, so if another recording is active,
     * this will throw an {@link IllegalStateException}.
     *
     * @throws IllegalStateException if the associated Recorder currently has an unfinished
     * active recording.
     */
    @NonNull
    public ActiveRecording start() {
        return getRecorder().start(this);
    }
}
