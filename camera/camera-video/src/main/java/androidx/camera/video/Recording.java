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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.utils.CloseGuardHelper;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides controls for the currently active recording.
 *
 * <p>An active recording is created by starting a pending recording with
 * {@link PendingRecording#start(Executor, Consumer)}. If there are no errors starting the
 * recording, upon creation, an active recording will provide controls to pause, resume or stop a
 * recording. If errors occur while starting the recording, the active recording will be
 * instantiated in a {@link VideoRecordEvent.Finalize finalized} state, and all controls will be
 * no-ops. The state of the recording can be observed by the video record event listener provided
 * to {@link PendingRecording#start(Executor, Consumer)} when starting the recording.
 *
 * <p>Either {@link #stop()} or {@link #close()} can be called when it is desired to
 * stop the recording. If {@link #stop()} or {@link #close()} are not called on this object
 * before it is no longer referenced, it will be automatically stopped at a future point in time
 * when the object is garbage collected, and no new recordings can be started from the same
 * {@link Recorder} that generated the object until that occurs.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Recording implements AutoCloseable {

    // Indicates the recording has been explicitly stopped by users.
    private final AtomicBoolean mIsClosed = new AtomicBoolean(false);
    private final Recorder mRecorder;
    private final long mRecordingId;
    private final OutputOptions mOutputOptions;
    private final CloseGuardHelper mCloseGuard = CloseGuardHelper.create();

    Recording(@NonNull Recorder recorder, long recordingId, @NonNull OutputOptions options,
            boolean finalizedOnCreation) {
        mRecorder = recorder;
        mRecordingId = recordingId;
        mOutputOptions = options;

        if (finalizedOnCreation) {
            mIsClosed.set(true);
        } else {
            mCloseGuard.open("stop");
        }
    }

    /**
     * Creates an {@link Recording} from a {@link PendingRecording} and recording ID.
     *
     * <p>The recording ID is expected to be unique to the recorder that generated the pending
     * recording.
     */
    @NonNull
    static Recording from(@NonNull PendingRecording pendingRecording, long recordingId) {
        Preconditions.checkNotNull(pendingRecording, "The given PendingRecording cannot be null.");
        return new Recording(pendingRecording.getRecorder(),
                recordingId,
                pendingRecording.getOutputOptions(),
                /*finalizedOnCreation=*/false);
    }

    /**
     * Creates an {@link Recording} from a {@link PendingRecording} and recording ID in a
     * finalized state.
     *
     * <p>This can be used if there was an error setting up the active recording and it would not
     * be able to be started.
     *
     * <p>The recording ID is expected to be unique to the recorder that generated the pending
     * recording.
     */
    @NonNull
    static Recording createFinalizedFrom(@NonNull PendingRecording pendingRecording,
            long recordingId) {
        Preconditions.checkNotNull(pendingRecording, "The given PendingRecording cannot be null.");
        return new Recording(pendingRecording.getRecorder(),
                recordingId,
                pendingRecording.getOutputOptions(),
                /*finalizedOnCreation=*/true);
    }

    @NonNull
    OutputOptions getOutputOptions() {
        return mOutputOptions;
    }

    /**
     * Pauses the current recording if active.
     *
     * <p>Successful pausing of a recording will generate a {@link VideoRecordEvent.Pause} event
     * which will be sent to the listener passed to
     * {@link PendingRecording#start(Executor, Consumer)}.
     *
     * <p>If the recording has already been paused or has been finalized internally, this is a
     * no-op.
     *
     * @throws IllegalStateException if the recording has been stopped with
     * {@link #close()} or {@link #stop()}.
     */
    public void pause() {
        if (mIsClosed.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        mRecorder.pause(this);
    }

    /**
     * Resumes the current recording if paused.
     *
     * <p>Successful resuming of a recording will generate a {@link VideoRecordEvent.Resume} event
     * which will be sent to the listener passed to
     * {@link PendingRecording#start(Executor, Consumer)}.
     *
     * <p>If the recording is active or has been finalized internally, this is a no-op.
     *
     * @throws IllegalStateException if the recording has been stopped with
     * {@link #close()} or {@link #stop()}.
     */
    public void resume() {
        if (mIsClosed.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        mRecorder.resume(this);
    }

    /**
     * Stops the recording, as if calling {@link #close()}.
     *
     * <p>This method is equivalent to calling {@link #close()}.
     */
    public void stop() {
        close();
    }

    /**
     * Mutes or un-mutes the current recording.
     *
     * <p>The output file will contain an audio track even the whole recording is muted. Create a
     * recording without calling {@link PendingRecording#withAudioEnabled()} to record a file
     * with no audio track.
     *
     * <p>Muting or unmuting a recording that isn't created
     * {@link PendingRecording#withAudioEnabled()} with audio enabled is no-op.
     *
     * @param muted mutes the recording if {@code true}, un-mutes otherwise.
     */
    public void mute(boolean muted) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("The recording has been stopped.");
        }
        mRecorder.mute(this, muted);
    }

    /**
     * Close this recording.
     *
     * <p>Once {@link #stop()} or {@code close()} called, all methods for controlling the state of
     * this recording besides {@link #stop()} or {@code close()} will throw an
     * {@link IllegalStateException}.
     *
     * <p>Once an active recording has been closed, the next recording can be started with
     * {@link PendingRecording#start(Executor, Consumer)}.
     *
     * <p>This method is idempotent; if the recording has already been closed or has been
     * finalized internally, calling {@link #stop()} or {@code close()} is a no-op.
     *
     * <p>This method is invoked automatically on active recording instances managed by the {@code
     * try-with-resources} statement.
     */
    @Override
    public void close() {
        stopWithError(VideoRecordEvent.Finalize.ERROR_NONE, /*errorCause=*/ null);
    }

    @Override
    @SuppressWarnings("GenericException") // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        try {
            mCloseGuard.warnIfOpen();
            stopWithError(VideoRecordEvent.Finalize.ERROR_RECORDING_GARBAGE_COLLECTED,
                    new RuntimeException("Recording stopped due to being garbage collected."));
        } finally {
            super.finalize();
        }
    }

    /** Returns the recording ID which is unique to the recorder that generated this recording. */
    long getRecordingId() {
        return mRecordingId;
    }

    /**
     * Returns whether the recording is closed.
     *
     * <p>The returned value does not reflect the state of the recording; it only reflects
     * whether {@link #stop()} or {@link #close()} was called on this object.
     *
     * <p>The state of the recording should be checked from the listener passed to
     * {@link PendingRecording#start(Executor, Consumer)}. Once the active recording is
     * stopped, a {@link VideoRecordEvent.Finalize} event will be sent to the listener.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isClosed() {
        return mIsClosed.get();
    }

    private void stopWithError(@VideoRecordEvent.Finalize.VideoRecordError int error,
            @Nullable Throwable errorCause) {
        mCloseGuard.close();
        if (mIsClosed.getAndSet(true)) {
            return;
        }
        mRecorder.stop(this, error, errorCause);
    }
}
