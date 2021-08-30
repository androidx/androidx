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
import androidx.camera.core.impl.utils.CloseGuardHelper;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides controls for the currently active recording.
 *
 * <p>An active recording is created by starting a pending recording with
 * {@link PendingRecording#start()}. If there are no errors starting the recording, upon
 * creation, an active recording will provide controls to pause, resume or stop a recording. If
 * errors occur while starting the recording, the active recording will be instantiated in a
 * {@link VideoRecordEvent.Finalize finalized} state, and all controls will be no-ops. The state
 * of the recording can be observed by
 * {@link PendingRecording#withEventListener(Executor, Consumer) adding a video record event
 * listener} to the pending recording before starting.
 *
 * <p>Either {@link #stop()} or {@link #close()} can be called when it is desired to
 * stop the recording, and must be called before this object and the
 * {@link Recorder} from which this object was created will no longer be referenced.
 */
public final class ActiveRecording implements AutoCloseable {

    private static final String TAG = "ActiveRecording";

    // Indicates the recording has been explicitly stopped by users.
    private final AtomicBoolean mIsStopped = new AtomicBoolean(false);
    // Indicates the recording has been finalized. Not to be confused with the object being
    // finalized via its finalizer.
    private final AtomicBoolean mIsRecordingFinalized = new AtomicBoolean(false);
    private final Recorder mRecorder;
    private final OutputOptions mOutputOptions;
    private final Consumer<VideoRecordEvent> mEventListener;
    private final Executor mCallbackExecutor;
    private final CloseGuardHelper mCloseGuard = CloseGuardHelper.create();
    private final boolean mAudioEnabled;

    ActiveRecording(@NonNull Recorder recorder, @NonNull OutputOptions options,
            @Nullable Executor callbackExecutor, @Nullable Consumer<VideoRecordEvent> listener,
            boolean audioEnabled) {
        mRecorder = recorder;
        mOutputOptions = options;
        mCallbackExecutor = callbackExecutor;
        mEventListener = listener;

        mCloseGuard.open("stop");
        mAudioEnabled = audioEnabled;
    }

    /**
     * Creates an {@link ActiveRecording} from a {@link PendingRecording}.
     */
    @NonNull
    static ActiveRecording from(@NonNull PendingRecording pendingRecording) {
        Preconditions.checkNotNull(pendingRecording, "The given PendingRecording cannot be null.");
        return new ActiveRecording(pendingRecording.getRecorder(),
                pendingRecording.getOutputOptions(), pendingRecording.getCallbackExecutor(),
                pendingRecording.getEventListener(), pendingRecording.isAudioEnabled());
    }

    @NonNull
    OutputOptions getOutputOptions() {
        return mOutputOptions;
    }

    boolean isAudioEnabled() {
        return mAudioEnabled;
    }

    /**
     * Pauses the current recording if active.
     *
     * <p>Successful pausing of a recording will generate a {@link VideoRecordEvent.Pause} event
     * which will be sent to the listener set on
     * {@link PendingRecording#withEventListener(Executor, Consumer)}.
     *
     * <p>If the recording has already been paused or has been finalized internally, this is a
     * no-op.
     *
     * @throws IllegalStateException if the recording has been stopped with
     * {@link #close()} or {@link #stop()}.
     */
    public void pause() {
        if (mIsStopped.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        if (mIsRecordingFinalized.get()) {
            return;
        }
        mRecorder.pause();
    }

    /**
     * Resumes the current recording if paused.
     *
     * <p>Successful resuming of a recording will generate a {@link VideoRecordEvent.Resume} event
     * which will be sent to the listener set on
     * {@link PendingRecording#withEventListener(Executor, Consumer)}.
     *
     * <p>If the recording is active or has been finalized internally, this is a no-op.
     *
     * @throws IllegalStateException if the recording has been stopped with
     * {@link #close()} or {@link #stop()}.
     */
    public void resume() {
        if (mIsStopped.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        if (mIsRecordingFinalized.get()) {
            return;
        }
        mRecorder.resume();
    }

    /**
     * Stops the recording.
     *
     * <p>Once stopped, all methods for controlling the state of this recording besides
     * {@code stop()} or {@link #close()} will throw an {@link IllegalStateException}.
     *
     * <p>Once an active recording has been stopped, the next recording can be started with
     * {@link PendingRecording#start()}.
     *
     * <p>This method is idempotent; if the recording has already been stopped or has been
     * finalized internally, calling {@code stop()} is a no-op.
     */
    public void stop() {
        mCloseGuard.close();
        if (mIsStopped.getAndSet(true) || mIsRecordingFinalized.get()) {
            return;
        }
        mRecorder.stop();
    }

    /**
     * Updates the recording status and callback to users.
     */
    void updateVideoRecordEvent(@NonNull VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Finalize) {
            mIsRecordingFinalized.set(true);
        }
        if (mCallbackExecutor != null && mEventListener != null) {
            try {
                mCallbackExecutor.execute(() -> mEventListener.accept(event));
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "The callback executor is invalid.", e);
            }
        }
    }

    /**
     * Close this recording, as if calling {@link #stop()}.
     *
     * <p>This method is invoked automatically on active recording instances managed by the {@code
     * try-with-resources} statement.
     *
     * <p>This method is equivalent to calling {@link #stop()}.
     */
    @Override
    public void close() {
        stop();
    }

    @Override
    @SuppressWarnings("GenericException") // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        try {
            mCloseGuard.warnIfOpen();
            stop();
        } finally {
            super.finalize();
        }
    }
}
