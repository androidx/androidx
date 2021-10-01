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

package androidx.camera.view.video;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.VideoCapture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Listener containing callbacks for video file I/O events. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@ExperimentalVideo
public interface OnVideoSavedCallback {
    /**
     * An unknown error occurred.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    int ERROR_UNKNOWN = VideoCapture.ERROR_UNKNOWN;
    /**
     * An error occurred with encoder state, either when trying to change state or when an
     * unexpected state change occurred.
     */
    int ERROR_ENCODER = VideoCapture.ERROR_ENCODER;
    /** An error with muxer state such as during creation or when stopping. */
    int ERROR_MUXER = VideoCapture.ERROR_MUXER;
    /**
     * An error indicating start recording was called when video recording is still in progress.
     */
    int ERROR_RECORDING_IN_PROGRESS = VideoCapture.ERROR_RECORDING_IN_PROGRESS;
    /**
     * An error indicating the file saving operations.
     */
    int ERROR_FILE_IO = VideoCapture.ERROR_FILE_IO;
    /**
     * An error indicating this VideoCapture is not bound to a camera.
     */
    int ERROR_INVALID_CAMERA = VideoCapture.ERROR_INVALID_CAMERA;

    /**
     * Describes the error that occurred during video capture operations.
     *
     * <p>This is a parameter sent to the error callback functions set in listeners such as {@link
     * OnVideoSavedCallback#onError(int, String, Throwable)}.
     *
     * <p>See message parameter in onError callback or log for more details.
     *
     * @hide
     */
    @IntDef({ERROR_UNKNOWN, ERROR_ENCODER, ERROR_MUXER, ERROR_RECORDING_IN_PROGRESS,
            ERROR_FILE_IO, ERROR_INVALID_CAMERA})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @interface VideoCaptureError {
    }

    /** Called when the video has been successfully saved. */
    void onVideoSaved(@NonNull OutputFileResults outputFileResults);

    /** Called when an error occurs while attempting to save the video. */
    void onError(@VideoCaptureError int videoCaptureError, @NonNull String message,
            @Nullable Throwable cause);
}
