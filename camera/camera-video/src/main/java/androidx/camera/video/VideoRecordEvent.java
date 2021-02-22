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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * VideoRecordEvent is used to report the video recording events and status.
 *
 * <p>There are {@link Start}, {@link Stop} and {@link Status} events. The {@link #getEventType()}
 * can be used to check what type of event is.
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
 * case STOP:
 *     VideoRecordEvent.Stop stop = (VideoRecordEvent.Stop) videoRecordEvent;
 *     break;
 * case STATUS:
 *     VideoRecordEvent.Status status = (VideoRecordEvent.Status) videoRecordEvent;
 *     break;
 * }
 *
 * }</pre>
 *
 * <p>When a video recording is requested, {@link Start} event will be reported at first and
 * {@link Stop} event will be reported when the recording is finished. The stop reason can be
 * obtained via {@link Stop#getError()}. {@link #ERROR_NONE} means that the video was recorded
 * successfully, and other error code indicate the recording is failed or stopped due to a certain
 * reason. Please note that a failed result does not mean that the video file has not been
 * generated. In some cases, the file can still be successfully generated. For example,
 * the result {@link #ERROR_INSUFFICIENT_DISK} will still have video file.
 *
 * <p>The {@link Status} event will be triggered continuously during the recording process,
 * {@link Status#getNumBytesRecorded()} can be used to get the total record size when reporting
 * status. And {@link Status#getRecordedDurationNs()} can be used to get the total duration.
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
         * Indicates the stop of recording.
         *
         * @see Stop
         */
        STOP,

        /**
         * The status report of the recording in progress.
         *
         * @see Status
         */
        STATUS
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
     * Describes the error that occurred during a video recording.
     *
     * <p>This is the error code returning from {@link Stop#getError()}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {ERROR_NONE, ERROR_UNKNOWN, ERROR_FILE_SIZE_LIMIT_REACHED,
            ERROR_INSUFFICIENT_DISK, ERROR_CAMERA_CLOSED})
    public @interface VideoRecordError {
    }

    /**
     * Gets the event type.
     */
    @NonNull
    public abstract EventType getEventType();

    /**
     * Indicates the start of recording.
     *
     * <p>When a video recording is requested, start event will be reported at first.
     */
    public static final class Start extends VideoRecordEvent {
        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.START;
        }
    }

    /**
     * Indicates the stop of recording.
     *
     * <p>The stop event will be triggered regardless of whether the recording succeeds or
     * fails. Use {@link Stop#getError()} to obtain the error type and {@link Stop#getCause()} to
     * get the error cause. If there is no error, {@link #ERROR_NONE} will be returned. Other
     * error code indicate the recording is failed or stopped due to a certain reason. Please
     * note that a failed result does not mean that the video file has not been generated. In
     * some cases, the file can still be successfully generated. For example, the result
     * {@link #ERROR_INSUFFICIENT_DISK} will still have video file.
     */
    public static final class Stop extends VideoRecordEvent {
        @VideoRecordError
        private final int mError;
        private final Throwable mCause;

        Stop(@VideoRecordError int error, @Nullable Throwable cause) {
            mError = error;
            mCause = cause;
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.STOP;
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
         * Gets the error cause. Returns {@code null} if {@link #getError()} returns
         * {@link #ERROR_NONE}.
         */
        @Nullable
        public Throwable getCause() {
            return mCause;
        }
    }

    /**
     * The status report of the recording in progress.
     */
    public static final class Status extends VideoRecordEvent {
        private final long mDurationNs;
        private final long mBytes;

        Status(long durationNs, long bytes) {
            mDurationNs = durationNs;
            mBytes = bytes;
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public EventType getEventType() {
            return EventType.STATUS;
        }

        /**
         * Gets the total recording duration in nanoseconds.
         *
         * <p>The duration will not include the duration of pause.
         */
        public long getRecordedDurationNs() {
            return mDurationNs;
        }

        /**
         * Gets the total recorded byte count.
         */
        public long getNumBytesRecorded() {
            return mBytes;
        }
    }
}
