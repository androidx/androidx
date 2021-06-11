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
import androidx.camera.core.Logger;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides controls for the currently active recording.
 */
public final class ActiveRecording {

    private static final String TAG = "ActiveRecording";

    // Indicates the recording has been explicitly stopped by users.
    private final AtomicBoolean mIsStopped = new AtomicBoolean(false);
    // Indicates the recording has been finalized.
    private final AtomicBoolean mIsFinalized = new AtomicBoolean(false);
    private final Recorder mRecorder;
    private final OutputOptions mOutputOptions;
    private Consumer<VideoRecordEvent> mEventListener;
    private Executor mCallbackExecutor;

    ActiveRecording(@NonNull Recorder recorder, @NonNull OutputOptions options,
            @Nullable Executor callbackExecutor, @Nullable Consumer<VideoRecordEvent> listener) {
        mRecorder = recorder;
        mOutputOptions = options;
        mCallbackExecutor = callbackExecutor;
        mEventListener = listener;
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

    @NonNull
    OutputOptions getOutputOptions() {
        return mOutputOptions;
    }

    /**
     * Pauses the current recording if active.
     *
     * <p>If the recording has already been paused or has been finalized internally, this is a
     * no-op.
     *
     * @throws IllegalStateException if the recording has been stopped.
     */
    public void pause() {
        if (mIsStopped.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        if (mIsFinalized.get()) {
            return;
        }
        mRecorder.pause();
    }

    /**
     * Resumes the current recording if paused.
     *
     * <p>If the recording is active or has been finalized internally, this is a no-op.
     *
     * @throws IllegalStateException if the recording has been stopped.
     */
    public void resume() {
        if (mIsStopped.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        if (mIsFinalized.get()) {
            return;
        }
        mRecorder.resume();
    }

    /**
     * Stops the recording.
     *
     * <p>Once stop, all other methods of this ActiveRecording will throw an
     * {@link IllegalStateException}.
     *
     * <p>Once an ActiveRecording has been stopped, the next recording can be started with
     * {@link PendingRecording#start()}.
     *
     * <p>If the recording has already been stopped or has been finalized internally, this is a
     * no-op.
     */
    public void stop() {
        if (mIsStopped.getAndSet(true) || mIsFinalized.get()) {
            return;
        }
        mRecorder.stop();
    }

    /**
     * Updates the recording status and callback to users.
     */
    void updateVideoRecordEvent(@NonNull VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Finalize) {
            mIsFinalized.set(true);
        }
        if (mCallbackExecutor != null && mEventListener != null) {
            try {
                mCallbackExecutor.execute(() -> mEventListener.accept(event));
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "The callback executor is invalid.", e);
            }
        }
    }
}
