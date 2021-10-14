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

package androidx.camera.core;

import android.app.NotificationManager;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@code CameraUnavailableException} is thrown when a camera device could not be queried or opened
 * or if the connection to an opened camera device is no longer valid.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraUnavailableException extends Exception {

    /**
     * Some other error occurred.
     */
    public static final int CAMERA_UNKNOWN_ERROR = 0;

    /**
     * The camera is disabled due to a device policy, and cannot be opened.
     */
    public static final int CAMERA_DISABLED = 1;

    /**
     * The camera device is removable and has been disconnected from the Android device, or the
     * camera service has shut down the connection due to a higher-priority access request for the
     * camera device.
     */
    public static final int CAMERA_DISCONNECTED = 2;

    /**
     * The camera device is currently in the error state.
     *
     * <p>The camera has failed to open or has failed at a later time as a result of some
     * non-user interaction.
     */
    public static final int CAMERA_ERROR = 3;


    /**
     * The camera device is in use already.
     */
    public static final int CAMERA_IN_USE = 4;

    /**
     * The system-wide limit for number of open cameras or camera resources has been reached, and
     * more camera devices cannot be opened.
     */
    public static final int CAMERA_MAX_IN_USE = 5;

    /**
     * The camera is unavailable due to {@link NotificationManager.Policy}. Some API 28 devices
     * cannot access the camera when the device is in "Do Not Disturb" mode. The camera will not
     * be accessible until "Do Not Disturb" mode is disabled.
     *
     * @see NotificationManager#getCurrentInterruptionFilter()
     * @see NotificationManager#ACTION_INTERRUPTION_FILTER_CHANGED
     */
    public static final int CAMERA_UNAVAILABLE_DO_NOT_DISTURB = 6;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            CAMERA_UNKNOWN_ERROR,
            CAMERA_DISABLED,
            CAMERA_DISCONNECTED,
            CAMERA_ERROR,
            CAMERA_IN_USE,
            CAMERA_MAX_IN_USE,
            CAMERA_UNAVAILABLE_DO_NOT_DISTURB
    })
    public @interface Reason {}

    private final int mReason;

    public CameraUnavailableException(@Reason int reason) {
        super();
        mReason = reason;
    }

    public CameraUnavailableException(@Reason int reason, @Nullable String message) {
        super(message);
        mReason = reason;
    }

    public CameraUnavailableException(@Reason int reason, @Nullable String message,
            @Nullable Throwable cause) {
        super(message, cause);
        mReason = reason;
    }

    public CameraUnavailableException(@Reason int reason, @Nullable Throwable cause) {
        super(cause);
        mReason = reason;
    }

    /** The reason the camera is unavailable. */
    @Reason
    public int getReason() {
        return mReason;
    }
}
