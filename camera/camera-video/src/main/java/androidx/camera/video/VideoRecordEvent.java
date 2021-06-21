/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * VideoRecordEvent is used to report the video recording events and status.
 *
 * <p>There are {@link Start}, {@link Finalize}, {@link Status}, {@link Pause} and {@link Resume}
 * events. The {@link #getEventType()} can be used to check what type of event is.
 *
 * Example: typical way to determine the event type and cast to the event class
 *
 * <pre>{@code
 *
 * VideoRecordEvent videoRecordEvent = obtainVideoRecordEvent();
 * switch (videoRecordEvent.getEventType()) {
 * case START:
 *     VideoRecordEvent.Start start = (VideoRecordEvent.Start) videoRecordEvent;
 *     break;
 * case FINALIZE:
 *     VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) videoRecordEvent;
 *     break;
 * case STATUS:
 *     VideoRecordEvent.Status status = (VideoRecordEvent.Status) videoRecordEvent;
 *     break;
 * case PAUSE:
 *     VideoRecordEvent.Pause pause = (VideoRecordEvent.Pause) videoRecordEvent;
 *     break;
 * case RESUME:
 *     VideoRecordEvent.Resume resume = (VideoRecordEvent.Resume) videoRecordEvent;
 *     break;
 * }
 *
 * }</pre>
 *
 * <p>When a video recording is requested, {@link Start} event will be reported at first and
 * {@link Finalize} event will be reported when the recording is finished. The stop reason can be
 * obtained via {@link Finalize#getError()}. {@link #ERROR_NONE} means that the video was recorded
 * successfully, and other error code indicate the recording is failed or stopped due to a certain
 * reason. Please note that a failed result does not mean that the video file has not been
 * generated. In some cases, the file can still be successfully generated. For example,
 * the result {@link #ERROR_INSUFFICIENT_DISK} will still have video file.
 *
 * <p>The {@link Status} event will be triggered continuously during the recording process,
 * {@link #getRecordingStats} can be used to get the recording state such as total recorded bytes
 * and total duration when the event is triggered.
 */
public abstract class VideoRecordEvent {

    /** The event types. */
    public enum EventType {
        /**
         * Indicates the start of recording.
         *
         * @see Start
         */
        START,

        /**
         * Indicates the finalization of recording.
         *
         * @see Finalize
         */
        FINALIZE,

        /**
         * The status report of the recording in progress.
         *
         * @see Status
         */
        STATUS,

        /**
         * Indicates the pause event of recording.
         *
         * @see Pause
         */
        PAUSE,

        /**
         * Indicates the resume event of recording.
         *
         * @see Resume
         */
        RESUME
    }

    /**
     * No error. The recording succeeds.
     */
    public static final int ERROR_NONE = 0;

    /**
     * Unknown error.
     */
    public static final int ERROR_UNKNOWN = 1;

    /**
     * The recording failed due to file size limitation.
     */
    // TODO(b/167481981): add more descriptions about the restrictions after getting into more
    //  details.
    public static final int ERROR_FILE_SIZE_LIMIT_REACHED = 2;

    /**
     * The recording failed due to insufficient disk space.
     */
    // TODO(b/167484136): add more descriptions about the restrictions after getting into more
    //  details.
    public static final int ERROR_INSUFFICIENT_DISK = 3;

    /**
     * The recording failed because the camera was closed.
     *
     * <p>One case is that camera has been closed due to lifecycle has stopped, so video
     * recording cannot be started.
     */
    public static final int ERROR_CAMERA_CLOSED = 4;

    /**
     * The recording failed due to the output options are invalid.
     */
    public static final int ERROR_INVALID_OUTPUT_OPTIONS = 5;

    /**
     * The recording failed while encoding.
     */
    public static final int ERROR_ENCODING_FAILED = 6;

    /**
     * The recording failed due to the recorder encountered errors.
     *
     * <p>Usually it can only be recovered by recreating a recorder and recordings with it.
     */
    public static final int ERROR_RECORDER_ERROR = 7;

    /**
     * The recording failed due to the recorder has not been initialized.
     */
    public static final int ERROR_RECORDER_UNINITIALIZED = 8;

    /**
     * Describes the error that occurred during a video recording.
     *
     * <p>This is the error code returning from {@link Finalize#getError()}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {ERROR_NONE, ERROR_UNKNOWN, ERROR_FILE_SIZE_LIMIT_REACHED,
            ERROR_INSUFFICIENT_DISK, ERROR_CAMERA_CLOSED, ERROR_INVALID_OUTPUT_OPTIONS,
            ERROR_ENCODING_FAILED, ERROR_RECORDER_ERROR, ERROR_RECORDER_UNINITIALIZED})
    public @interface VideoRecordError {
    }

    private final OutputOptions mOutputOptions;
    private final RecordingStats mRecordingStats;

    // Restrict access to emulate sealed class
    // Classes will be constructed with static factory methods
    VideoRecordEvent(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        mOutputOptions = Preconditions.checkNotNull(outputOptions);
        mRecordingStats = Preconditions.checkNotNull(recordingStats);
    }

    /**
     * Gets the event type.
     */
    @NonNull
    public abstract EventType getEventType();

    /**
     * Gets the recording status of current event.
     */
    @NonNull
    public RecordingStats getRecordingStats() {
        return mRecordingStats;
    }

    /**
     * Gets the {@link OutputOptions} associated with this event.
     */
    @NonNull
    public OutputOptions getOutputOptions() {
        return mOutputOptions;
    }

    @NonNull
    static Start start(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        return new Start(outputOptions, recordingStats);
    }

    /**
     * Indicates the start of recording.
     *
     * <p>When a video recording is requested, start event will be reported at first.
     */
    public static final class Start extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Start(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.START;
        }
    }

    @NonNull
    static Finalize finalize(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats,
            @NonNull OutputResults outputResults) {
        return new Finalize(outputOptions, recordingStats, outputResults, ERROR_NONE, null);
    }

    @NonNull
    static Finalize finalizeWithError(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats,
            @NonNull OutputResults outputResults,
            @VideoRecordError int error,
            @Nullable Throwable cause) {
        Preconditions.checkArgument(error != ERROR_NONE, "An error type is required.");
        return new Finalize(outputOptions, recordingStats, outputResults, error, cause);
    }

    /**
     * Indicates the stop of recording.
     *
     * <p>The stop event will be triggered regardless of whether the recording succeeds or
     * fails. Use {@link Finalize#getError()} to obtain the error type and
     * {@link Finalize#getCause()} to get the error cause. If there is no error,
     * {@link #ERROR_NONE} will be returned. Other error code indicate the recording is failed or
     * stopped due to a certain reason. Please note that a failed result does not mean that the
     * video file has not been generated. In some cases, the file can still be successfully
     * generated. For example, the result {@link #ERROR_INSUFFICIENT_DISK} will still have video
     * file.
     */
    public static final class Finalize extends VideoRecordEvent {
        private final OutputResults mOutputResults;
        @VideoRecordError
        private final int mError;
        private final Throwable mCause;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Finalize(@NonNull OutputOptions outputOptions,
                @NonNull RecordingStats recordingStats,
                @NonNull OutputResults outputResults,
                @VideoRecordError int error,
                @Nullable Throwable cause) {
            super(outputOptions, recordingStats);
            mOutputResults = outputResults;
            mError = error;
            mCause = cause;
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.FINALIZE;
        }

        /**
         * Gets the {@link OutputResults}.
         */
        @NonNull
        public OutputResults getOutputResults() {
            return mOutputResults;
        }

        /**
         * Indicates whether an error occurred.
         *
         * <p>Returns {@code true} if {@link #getError()} returns {@link #ERROR_NONE}, otherwise
         * {@code false}.
         */
        public boolean hasError() {
            return mError != ERROR_NONE;
        }

        /**
         * Gets the error type for a video recording.
         *
         * <p>Returns {@link #ERROR_NONE} if the recording did not stop due to an error.
         */
        @VideoRecordError
        public int getError() {
            return mError;
        }

        /**
         * Gets the error cause.
         *
         * <p>Returns {@code null} if {@link #hasError()} returns {@code false}.
         */
        @Nullable
        public Throwable getCause() {
            return mCause;
        }
    }

    @NonNull
    static Status status(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        return new Status(outputOptions, recordingStats);
    }

    /**
     * The status report of the recording in progress.
     */
    public static final class Status extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Status(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.STATUS;
        }
    }

    @NonNull
    static Pause pause(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        return new Pause(outputOptions, recordingStats);
    }

    /**
     * Indicates the pause event of recording.
     */
    public static final class Pause extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Pause(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.PAUSE;
        }
    }

    @NonNull
    static Resume resume(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        return new Resume(outputOptions, recordingStats);
    }

    /**
     * Indicates the resume event of recording.
     */
    public static final class Resume extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Resume(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.RESUME;
        }
    }
}
