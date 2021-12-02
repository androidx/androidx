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

package androidx.camera.camera2.internal.compat;

import android.app.NotificationManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper for accessing features in {@link CameraAccessException} in a backwards compatible fashion.
 */
@RequiresApi(21)
public class CameraAccessExceptionCompat extends Exception {

    // Start of the CameraAccessException error
    // *********************************************************************************************
    /**
     * The camera device is in use already.
     */
    public static final int CAMERA_IN_USE = 4; // Equal to CameraAccessException.CAMERA_IN_USE.

    /**
     * The system-wide limit for number of open cameras or camera resources has been reached, and
     * more camera devices cannot be opened or torch mode cannot be turned on until previous
     * instances are closed.
     */
    public static final int MAX_CAMERAS_IN_USE = 5;
            // Equal to CameraAccessException.MAX_CAMERAS_IN_USE.

    /**
     * The camera is disabled due to a device policy, and cannot be opened.
     *
     * @see android.app.admin.DevicePolicyManager#setCameraDisabled(android.content.ComponentName,
     * boolean)
     */
    public static final int CAMERA_DISABLED = 1; // Equal to CameraAccessException.CAMERA_DISABLED.

    /**
     * The camera device is removable and has been disconnected from the Android device, or the
     * camera id used with {@link android.hardware.camera2.CameraManager#openCamera} is no longer
     * valid, or the camera service has shut down the connection due to a higher-priority access
     * request for the camera device.
     */
    public static final int CAMERA_DISCONNECTED = 2;
            // Equal to CameraAccessException.CAMERA_DISCONNECTED.

    /**
     * The camera device is currently in the error state.
     *
     * <p>The camera has failed to open or has failed at a later time as a result of some
     * non-user interaction. Refer to {@link CameraDevice.StateCallback#onError} for the exact
     * nature of the error.</p>
     *
     * <p>No further calls to the camera will succeed. Clean up the camera with
     * {@link CameraDevice#close} and try handling the error in order to successfully re-open the
     * camera.</p>
     */
    public static final int CAMERA_ERROR = 3; // Equal to CameraAccessException.CAMERA_ERROR.

    /**
     * A deprecated HAL version is in use.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int CAMERA_DEPRECATED_HAL = 1000;
            // Equal to CameraAccessException.CAMERA_DEPRECATED_HAL.

    @VisibleForTesting
    static final Set<Integer> PLATFORM_ERRORS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CAMERA_IN_USE,
                    MAX_CAMERAS_IN_USE, CAMERA_DISABLED, CAMERA_DISCONNECTED, CAMERA_ERROR)));

    // End of the CameraAccessException error
    // *********************************************************************************************

    // Start of the CameraAccessExceptionCompat error
    // *********************************************************************************************

    // Compat specific errors should start at 10001

    /**
     * The camera is unavailable due to {@link NotificationManager.Policy}. Some API 28 devices
     * cannot access the camera when the device is in "Do Not Disturb" mode. The camera will not
     * be accessible until "Do Not Disturb" mode is disabled.
     *
     * @see NotificationManager#getCurrentInterruptionFilter()
     * @see NotificationManager#ACTION_INTERRUPTION_FILTER_CHANGED
     */
    public static final int CAMERA_UNAVAILABLE_DO_NOT_DISTURB = 10001;

    /**
     * Error occurs when creating {@link CameraCharacteristics}. Some devices may throw
     * {@link AssertionError} when creating CameraCharacteristics and FPS ranges are null.
     */
    public static final int CAMERA_CHARACTERISTICS_CREATION_ERROR = 10002;

    @VisibleForTesting
    static final Set<Integer> COMPAT_ERRORS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(CAMERA_UNAVAILABLE_DO_NOT_DISTURB,
                    CAMERA_CHARACTERISTICS_CREATION_ERROR)));

    // End of the CameraAccessExceptionCompat error
    // *********************************************************************************************

    private final int mReason;

    private final CameraAccessException mCameraAccessException;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            CAMERA_IN_USE,
            MAX_CAMERAS_IN_USE,
            CAMERA_DISABLED,
            CAMERA_DISCONNECTED,
            CAMERA_ERROR,

            // Start of the compat error
            CAMERA_UNAVAILABLE_DO_NOT_DISTURB,
            CAMERA_CHARACTERISTICS_CREATION_ERROR
    })
    public @interface AccessError {
    }

    public CameraAccessExceptionCompat(@AccessError int reason) {
        super(getDefaultMessage(reason));
        mReason = reason;
        mCameraAccessException = PLATFORM_ERRORS.contains(reason)
                ? new CameraAccessException(reason) : null;
    }

    public CameraAccessExceptionCompat(@AccessError int reason, @Nullable String message) {
        super(getCombinedMessage(reason, message));
        mReason = reason;
        mCameraAccessException = PLATFORM_ERRORS.contains(reason)
                ? new CameraAccessException(reason, message) : null;
    }

    public CameraAccessExceptionCompat(@AccessError int reason, @Nullable String message,
            @Nullable Throwable cause) {
        super(getCombinedMessage(reason, message), cause);
        mReason = reason;
        mCameraAccessException = PLATFORM_ERRORS.contains(reason)
                ? new CameraAccessException(reason, message, cause) : null;
    }

    public CameraAccessExceptionCompat(@AccessError int reason, @Nullable Throwable cause) {
        super(getDefaultMessage(reason), cause);
        mReason = reason;
        mCameraAccessException = PLATFORM_ERRORS.contains(reason)
                ? new CameraAccessException(reason, null, cause) : null;
    }

    private CameraAccessExceptionCompat(@NonNull CameraAccessException e) {
        super(e.getMessage(), e.getCause());
        mReason = e.getReason();
        mCameraAccessException = e;
    }

    /**
     * The reason for the failure to access the camera.
     */
    @AccessError
    public final int getReason() {
        return mReason;
    }

    /**
     * Provides the platform class object represented by this object.
     *
     * @return platform class object, null if it is a compat specific error.
     */
    @Nullable
    public CameraAccessException toCameraAccessException() {
        return mCameraAccessException;
    }

    /**
     * Provides a backward-compatible wrapper for {@link CameraAccessException}.
     *
     * @param cameraAccessException {@link CameraAccessException} class to wrap
     * @return wrapped class
     */
    @NonNull
    public static CameraAccessExceptionCompat toCameraAccessExceptionCompat(
            @NonNull CameraAccessException cameraAccessException) {
        if (cameraAccessException == null) {
            throw new NullPointerException("cameraAccessException should not be null");
        }
        return new CameraAccessExceptionCompat(cameraAccessException);
    }

    @Nullable
    private static String getDefaultMessage(@AccessError int problem) {
        switch (problem) {
            case CAMERA_IN_USE:
                return "The camera device is in use already";
            case MAX_CAMERAS_IN_USE:
                return "The system-wide limit for number of open cameras has been reached, "
                        + "and more camera devices cannot be opened until previous instances "
                        + "are closed.";
            case CAMERA_DISCONNECTED:
                return "The camera device is removable and has been disconnected from the "
                        + "Android device, or the camera service has shut down the connection due "
                        + "to a higher-priority access request for the camera device.";
            case CAMERA_DISABLED:
                return "The camera is disabled due to a device policy, and cannot be opened.";
            case CAMERA_ERROR:
                return "The camera device is currently in the error state; "
                        + "no further calls to it will succeed.";

            // Start of the compat errors
            case CAMERA_UNAVAILABLE_DO_NOT_DISTURB:
                return "Some API 28 devices cannot access the camera when the device is in \"Do "
                        + "Not Disturb\" mode. The camera will not be accessible until \"Do Not "
                        + "Disturb\" mode is disabled.";

            case CAMERA_CHARACTERISTICS_CREATION_ERROR:
                return "Failed to create CameraCharacteristics.";
        }
        return null;
    }

    private static String getCombinedMessage(@AccessError int problem, String message) {
        String problemString = getProblemString(problem);
        return String.format("%s (%d): %s", problemString, problem, message);
    }

    @NonNull
    private static String getProblemString(int problem) {
        String problemString;
        switch (problem) {
            case CAMERA_IN_USE:
                problemString = "CAMERA_IN_USE";
                break;
            case MAX_CAMERAS_IN_USE:
                problemString = "MAX_CAMERAS_IN_USE";
                break;
            case CAMERA_DISCONNECTED:
                problemString = "CAMERA_DISCONNECTED";
                break;
            case CAMERA_DISABLED:
                problemString = "CAMERA_DISABLED";
                break;
            case CAMERA_ERROR:
                problemString = "CAMERA_ERROR";
                break;
            case CAMERA_DEPRECATED_HAL:
                problemString = "CAMERA_DEPRECATED_HAL";
                break;

            // Start of the compat errors
            case CAMERA_UNAVAILABLE_DO_NOT_DISTURB:
                problemString = "CAMERA_UNAVAILABLE_DO_NOT_DISTURB";
                break;
            case CAMERA_CHARACTERISTICS_CREATION_ERROR:
                problemString = "CAMERA_CHARACTERISTICS_CREATION_ERROR";
                break;
            default:
                problemString = "<UNKNOWN ERROR>";
        }
        return problemString;
    }
}
