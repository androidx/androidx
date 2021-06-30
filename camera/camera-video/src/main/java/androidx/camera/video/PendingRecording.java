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
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;

/**
 * A recording that can be started at a future time.
 *
 * <p>A pending recording allows for configuration of a recording before it is started. Once a
 * pending recording is started with {@link #start()}, any changes to the pending recording will
 * not affect the actual recording; any modifications to the recording will need to occur through
 * the controls of the {@link ActiveRecording} class returned by {@link #start()}.
 *
 * <p>A pending recording can be created using one of the {@link Recorder} methods for starting a
 * recording such as {@link Recorder#prepareRecording(MediaStoreOutputOptions)}.
 */
public final class PendingRecording {

    private final Recorder mRecorder;
    private final OutputOptions mOutputOptions;
    private Consumer<VideoRecordEvent> mEventListener;
    private Executor mCallbackExecutor;

    PendingRecording(@NonNull Recorder recorder, @NonNull OutputOptions options) {
        mRecorder = recorder;
        mOutputOptions = options;
    }

    @NonNull
    Recorder getRecorder() {
        return mRecorder;
    }

    @NonNull
    OutputOptions getOutputOptions() {
        return mOutputOptions;
    }

    @Nullable
    Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Nullable
    Consumer<VideoRecordEvent> getEventListener() {
        return mEventListener;
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
        mCallbackExecutor = callbackExecutor;
        mEventListener = listener;
        return this;
    }

    /**
     * Starts the recording, making it an active recording.
     *
     * <p>Only a single recording can be active at a time, so if another recording is active,
     * this will throw an {@link IllegalStateException}.
     *
     * <p>If there are no errors starting the recording, the returned {@link ActiveRecording}
     * can be used to {@link ActiveRecording#pause() pause}, {@link ActiveRecording#resume() resume
     * }, or {@link ActiveRecording#stop() stop} the recording.
     *
     * <p>Upon successfully starting the recording, a {@link VideoRecordEvent.Start} event will
     * be the first event sent to the listener set in
     * {@link #withEventListener(Executor, Consumer)}.
     *
     * <p>If errors occur while starting the recording, a {@link VideoRecordEvent.Finalize} event
     * will be the first event sent to the listener set in
     * {@link #withEventListener(Executor, Consumer)}, and information about the error can be
     * found in that event's {@link VideoRecordEvent.Finalize#getError()} method. The returned
     * {@link ActiveRecording} will be in a finalized state, and all controls will be no-ops.
     *
     * @throws IllegalStateException if the associated Recorder currently has an unfinished
     * active recording.
     */
    @NonNull
    public ActiveRecording start() {
        return mRecorder.start(this);
    }
}
