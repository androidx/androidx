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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides controls for the currently active recording.
 */
public final class ActiveRecording extends Recording {

    private final AtomicBoolean mIsFinalized = new AtomicBoolean(false);

    ActiveRecording(@NonNull Recorder recorder, @NonNull OutputOptions options,
            @Nullable Executor callbackExecutor, @Nullable Consumer<VideoRecordEvent> listener) {
        super(recorder, options, callbackExecutor, listener);
    }

    /**
     * Creates an {@link ActiveRecording} from a {@link PendingRecording}.
     */
    @NonNull
    static ActiveRecording from(@NonNull PendingRecording pendingRecording) {
        Preconditions.checkNotNull(pendingRecording, "The given PendingRecording cannot be null.");
        return new ActiveRecording(pendingRecording.getRecorder(),
                pendingRecording.getOutputOptions(), pendingRecording.getCallbackExecutor(),
                pendingRecording.getEventListener());
    }

    /**
     * Pauses the current recording if active.
     *
     * <p>If the recording is already paused or has been finalized internally, this is a no-op.
     */
    public void pause() {
        if (mIsFinalized.get()) {
            return;
        }
        getRecorder().pause();
    }

    /**
     * Resumes the current recording if paused.
     *
     * <p>If the recording is running or has been finalized internally, this is a no-op.
     */
    public void resume() {
        if (mIsFinalized.get()) {
            return;
        }
        getRecorder().resume();
    }

    /**
     * Stops the recording.
     *
     * <p>Once stop, all methods of this ActiveRecording will be no-op.
     *
     * <p>Once an ActiveRecording has been stopped, the next recording can be started with
     * {@link PendingRecording#start()}.
     *
     * <p>If the recording is finalized internally, this is a no-op.
     */
    public void stop() {
        if (mIsFinalized.getAndSet(true)) {
            return;
        }
        getRecorder().stop();
    }

    @Override
    void updateVideoRecordEvent(@NonNull VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Finalize) {
            mIsFinalized.set(true);
        }
        super.updateVideoRecordEvent(event);
    }
}
