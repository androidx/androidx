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

import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.VideoRecordError;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * VideoRecordEvent is used to report video recording events and status.
 *
 * <p>Upon starting a recording by {@link PendingRecording#start(Executor, Consumer)}, recording
 * events will start to be sent to the listener passed to
 * {@link PendingRecording#start(Executor, Consumer)}.
 *
 * <p>There are {@link Start}, {@link Finalize}, {@link Status}, {@link Pause} and {@link Resume}
 * events.
 *
 * <p>Example: Below is the typical way to determine the event type and cast to the event class, if
 * needed.
 *
 * <pre>{@code
 *
 * Recording recording = recorder.prepareRecording(context, outputOptions)
 *     .start(ContextCompat.getMainExecutor(context), videoRecordEvent -> {
 *         if (videoRecordEvent instanceof VideoRecordEvent.Start) {
 *             // Handle the start of a new active recording
 *             ...
 *         } else if (videoRecordEvent instanceof VideoRecordEvent.Pause) {
 *             // Handle the case where the active recording is paused
 *             ...
 *         } else if (videoRecordEvent instanceof VideoRecordEvent.Resume) {
 *             // Handles the case where the active recording is resumed
 *             ...
 *         } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
 *             VideoRecordEvent.Finalize finalizeEvent =
 *                 (VideoRecordEvent.Finalize) videoRecordEvent;
 *             // Handles a finalize event for the active recording, checking Finalize.getError()
 *             int error = finalizeEvent.getError();
 *             if (error != Finalize.ERROR_NONE) {
 *                 ...
 *             }
 *         }
 *
 *         // All events, including VideoRecordEvent.Status, contain RecordingStats.
 *         // This can be used to update the UI or track the recording duration.
 *         RecordingStats recordingStats = videoRecordEvent.getRecordingStats();
 *         ...
 *     });
 *
 * }</pre>
 *
 * <p>If using Kotlin, the VideoRecordEvent class can be treated similar to a {@code sealed
 * class}. In Kotlin, it is recommended to use a {@code when} expression rather than an {@code
 * if}-{@code else if} chain as in the above example.
 *
 * <p>When a video recording is requested, {@link Start} event will be reported at first and
 * {@link Finalize} event will be reported when the recording is finished. The stop reason can be
 * obtained via {@link Finalize#getError()}. {@link Finalize#ERROR_NONE} means that the video was
 * recorded successfully, and other error code indicate the recording is failed or stopped due to
 * a certain reason. Please note that a failed result does not mean that the video file has not been
 * generated. In some cases, the file can still be successfully generated. For example, the
 * result {@link Finalize#ERROR_FILE_SIZE_LIMIT_REACHED} will still have video file.
 *
 * <p>The {@link Status} event will be triggered continuously during the recording process,
 * {@link #getRecordingStats} can be used to get the recording state such as total recorded bytes
 * and total duration when the event is triggered.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class VideoRecordEvent {

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
     * Gets the recording statistics of current event.
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
     * <p>When a video recording is successfully requested by
     * {@link PendingRecording#start(Executor, Consumer)}, a {@code Start} event will be the
     * first event.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Start extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Start(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
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
     * Indicates the finalization of recording.
     *
     * <p>The finalize event will be triggered regardless of whether the recording succeeds or
     * fails. Use {@link Finalize#getError()} to obtain the error type and
     * {@link Finalize#getCause()} to get the error cause. If there is no error,
     * {@link #ERROR_NONE} will be returned. Other error types indicate the recording is failed or
     * stopped due to a certain reason. Please note that receiving a finalize event with error
     * does not necessarily mean that the video file has not been generated. In some cases, the
     * file can still be successfully generated depending on the error type. For example, a file
     * will still be generated when the recording is finalized with
     * {@link #ERROR_FILE_SIZE_LIMIT_REACHED}. A file may or may not be generated when the
     * recording is finalized with {@link #ERROR_INSUFFICIENT_STORAGE}. Example to detect if an
     * output file is generated:
     * <pre>{@code
     * if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
     *     VideoRecordEvent.Finalize finalizeEvent =
     *             (VideoRecordEvent.Finalize) videoRecordEvent;
     *     OutputOptions options = finalizeEvent.getOutputOptions();
     *     switch (finalizeEvent.getError()) {
     *     case ERROR_INSUFFICIENT_STORAGE:
     *         if (options instanceof FileOutputOptions) {
     *             if (((FileOutputOptions) options).getFile().exists()) {
     *                 // file exists
     *             }
     *         } else if (options instanceof MediaStoreOutputOptions) {
     *             Uri uri = finalizeEvent.getOutputResults().getOutputUri();
     *             if (uri != Uri.EMPTY) {
     *                 // file exists
     *             }
     *         } else if (options instanceof FileDescriptorOutputOptions) {
     *             // User has to check the referenced target of the file descriptor.
     *         }
     *         break;
     *     }
     * }
     * }</pre>
     *
     * <p>For certain types of errors, the output file will not be constructed correctly, and it
     * will be the user's responsibility to deal with the incomplete file, such as deleting it.
     * Example to delete the file:
     *
     * <pre>{@code
     * if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
     *     VideoRecordEvent.Finalize finalizeEvent =
     *             (VideoRecordEvent.Finalize) videoRecordEvent;
     *     OutputOptions options = finalizeEvent.getOutputOptions();
     *     switch (finalizeEvent.getError()) {
     *     case ERROR_UNKNOWN:
     *     case ERROR_RECORDER_ERROR:
     *     case ERROR_ENCODING_FAILED:
     *     case ERROR_NO_VALID_DATA:
     *         if (options instanceof FileOutputOptions) {
     *             ((FileOutputOptions) options).getFile().delete();
     *         } else if (options instanceof MediaStoreOutputOptions) {
     *             Uri uri = finalizeEvent.getOutputResults().getOutputUri();
     *             if (uri != Uri.EMPTY) {
     *                 context.getContentResolver().delete(uri, null, null);
     *             }
     *         } else if (options instanceof FileDescriptorOutputOptions) {
     *             // User has to clean up the referenced target of the file descriptor.
     *         }
     *         break;
     *     }
     * }
     * }</pre>
     *
     * <p>If there's no error that prevents the file to be generated, the file can be accessed
     * safely after receiving the finalize event.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Finalize extends VideoRecordEvent {
        /**
         * The recording succeeded with no error.
         */
        public static final int ERROR_NONE = 0;

        /**
         * An unknown error occurred.
         *
         * <p>The output file may or moy not be generated. Since the error is not determined,
         * application should clean up the output file, such as deleting it.
         */
        public static final int ERROR_UNKNOWN = 1;

        /**
         * The recording failed due to file size limitation.
         *
         * <p>The file size limit refers to {@link OutputOptions#getFileSizeLimit()}. The
         * recording will be finalized automatically with this error when the limit is reached and
         * the data produced before the limit is reached will be saved to the output file.
         */
        // TODO(b/167481981): add more descriptions about the restrictions after getting into more
        //  details.
        public static final int ERROR_FILE_SIZE_LIMIT_REACHED = 2;

        /**
         * The recording failed due to insufficient storage space.
         *
         * <p>There are two possible cases that will cause this error.
         * <ul>
         *     <li>The storage is already full before the recording starts, so no output file
         *     will be generated.</li>
         *     <li>The storage becomes full during recording, so the output file will be
         *     generated. </li>
         * </ul>
         */
        // TODO(b/167484136): add more descriptions about the restrictions after getting into more
        //  details.
        public static final int ERROR_INSUFFICIENT_STORAGE = 3;

        /**
         * The recording failed because the source becomes inactive and stops sending frames.
         *
         * <p>One case is that if camera is closed due to lifecycle stopped, the active recording
         * will be finalized with this error, and the output will be generated, containing the
         * frames produced before camera closing. Attempting to start a new recording will be
         * finalized immediately if the source remains inactive and no output will be generated.
         */
        public static final int ERROR_SOURCE_INACTIVE = 4;

        /**
         * The recording failed due to invalid output options.
         *
         * <p>This error is generated when invalid output options have been used while preparing a
         * recording, such as with the
         * {@link Recorder#prepareRecording(android.content.Context, MediaStoreOutputOptions)}
         * method. The error will depend on the subclass of {@link OutputOptions} used.
         *
         * <p>No output file will be generated with this error.
         */
        public static final int ERROR_INVALID_OUTPUT_OPTIONS = 5;

        /**
         * The recording failed while encoding.
         *
         * <p>This error may be generated when the video or audio codec encounters an error during
         * encoding. When this happens and the output file is generated, the output file is not
         * properly constructed. The application will need to clean up the output file, such as
         * deleting the file.
         */
        public static final int ERROR_ENCODING_FAILED = 6;

        /**
         * The recording failed because the {@link Recorder} is in an unrecoverable error state.
         *
         * <p>When this happens and the output file is generated, the output file is not properly
         * constructed. The application will need to clean up the output file, such as deleting
         * the file. Such an error will usually require creating a new {@link Recorder} object to
         * start a new recording.
         */
        public static final int ERROR_RECORDER_ERROR = 7;

        /**
         * The recording failed because no valid data was produced to be recorded.
         *
         * <p>This error is generated when the essential data for a recording to be played correctly
         * is missing, for example, a recording must contain at least one key frame. The
         * application will need to clean up the output file, such as deleting the file.
         */
        public static final int ERROR_NO_VALID_DATA = 8;

        /**
         * The recording failed due to duration limitation.
         *
         * <p>The duration limit refers to {@link OutputOptions#getDurationLimitMillis()}. The
         * recording will be finalized automatically with this error when the limit is reached and
         * the data produced before the limit is reached will be saved to the output file.
         */
        public static final int ERROR_DURATION_LIMIT_REACHED = 9;

        /**
         * The recording was stopped because the {@link Recording} object was garbage collected.
         *
         * <p>The {@link Recording} object returned by
         * {@link PendingRecording#start(Executor, Consumer)} must be referenced until the
         * recording is no longer needed. If it is not, the active recording will be stopped and
         * this error will be produced. Once {@link Recording#stop()} or
         * {@link Recording#close()} has been invoked, the recording object no longer needs to be
         * referenced.
         */
        public static final int ERROR_RECORDING_GARBAGE_COLLECTED = 10;

        /**
         * Describes the error that occurred during a video recording.
         *
         * <p>This is the error code returning from {@link Finalize#getError()}.
         *
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {ERROR_NONE, ERROR_UNKNOWN, ERROR_FILE_SIZE_LIMIT_REACHED,
                ERROR_INSUFFICIENT_STORAGE, ERROR_INVALID_OUTPUT_OPTIONS, ERROR_ENCODING_FAILED,
                ERROR_RECORDER_ERROR, ERROR_NO_VALID_DATA, ERROR_SOURCE_INACTIVE,
                ERROR_DURATION_LIMIT_REACHED, ERROR_RECORDING_GARBAGE_COLLECTED})
        public @interface VideoRecordError {
        }

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
         * <p>Possible values are {@link #ERROR_NONE}, {@link #ERROR_UNKNOWN},
         * {@link #ERROR_FILE_SIZE_LIMIT_REACHED}, {@link #ERROR_INSUFFICIENT_STORAGE},
         * {@link #ERROR_INVALID_OUTPUT_OPTIONS}, {@link #ERROR_ENCODING_FAILED},
         * {@link #ERROR_RECORDER_ERROR}, {@link #ERROR_NO_VALID_DATA} and
         * {@link #ERROR_SOURCE_INACTIVE}.
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

        @NonNull
        static String errorToString(@VideoRecordError int error) {
            switch (error) {
                case ERROR_NONE: return "ERROR_NONE";
                case ERROR_UNKNOWN: return "ERROR_UNKNOWN";
                case ERROR_FILE_SIZE_LIMIT_REACHED: return "ERROR_FILE_SIZE_LIMIT_REACHED";
                case ERROR_INSUFFICIENT_STORAGE: return "ERROR_INSUFFICIENT_STORAGE";
                case ERROR_INVALID_OUTPUT_OPTIONS: return "ERROR_INVALID_OUTPUT_OPTIONS";
                case ERROR_ENCODING_FAILED: return "ERROR_ENCODING_FAILED";
                case ERROR_RECORDER_ERROR: return "ERROR_RECORDER_ERROR";
                case ERROR_NO_VALID_DATA: return "ERROR_NO_VALID_DATA";
                case ERROR_SOURCE_INACTIVE: return "ERROR_SOURCE_INACTIVE";
                case ERROR_DURATION_LIMIT_REACHED: return "ERROR_DURATION_LIMIT_REACHED";
                case ERROR_RECORDING_GARBAGE_COLLECTED: return "ERROR_RECORDING_GARBAGE_COLLECTED";
            }

            // Should never reach here, but just in case...
            return "Unknown(" + error + ")";
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
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Status extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Status(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }
    }

    @NonNull
    static Pause pause(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        return new Pause(outputOptions, recordingStats);
    }

    /**
     * Indicates the pause event of recording.
     *
     * <p>A {@code Pause} event will be triggered after calling {@link Recording#pause()}.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Pause extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Pause(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }
    }

    @NonNull
    static Resume resume(@NonNull OutputOptions outputOptions,
            @NonNull RecordingStats recordingStats) {
        return new Resume(outputOptions, recordingStats);
    }

    /**
     * Indicates the resume event of recording.
     *
     * <p>A {@code Resume} event will be triggered after calling {@link Recording#resume()}.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Resume extends VideoRecordEvent {

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        Resume(@NonNull OutputOptions outputOptions, @NonNull RecordingStats recordingStats) {
            super(outputOptions, recordingStats);
        }
    }
}
