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

    ActiveRecording(@NonNull Recorder recorder, @NonNull OutputOptions options,
            @Nullable Executor callbackExecutor, @Nullable Consumer<VideoRecordEvent> listener) {
        mRecorder = recorder;
        mOutputOptions = options;
        mCallbackExecutor = callbackExecutor;
        mEventListener = listener;

        mCloseGuard.open("stop");
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
        if (mIsRecordingFinalized.get()) {
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
        if (mIsRecordingFinalized.get()) {
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
     * {@inheritDoc}
     *
     * This method is equivalent to calling {@link #stop()}
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
