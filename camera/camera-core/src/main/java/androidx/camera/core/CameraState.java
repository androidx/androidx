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

package androidx.camera.core;

import android.content.ComponentName;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the different states the camera can be in.
 *
 * <p>The following table displays the states the camera can be in, and the possible transitions
 * between them.
 *
 * <table>
 * <tr>
 *     <th>State</th>
 *     <th>Transition cause</th>
 *     <th>New State</th>
 * </tr>
 * <tr>
 *     <td rowspan="2">CLOSED</td>
 *     <td>Received signal to open camera, and camera unavailable</td>
 *     <td>PENDING_OPEN</td>
 * </tr>
 * <tr>
 *     <td>Received signal to open camera, and camera available</td>
 *     <td>OPENING</td>
 * </tr>
 * <tr>
 *     <td>PENDING_OPEN</td>
 *     <td>Received signal that camera is available</td>
 *     <td>OPENING</td>
 * </tr>
 * <tr>
 *     <td rowspan="5">OPENING</td>
 *     <td>Camera opened successfully</td>
 *     <td>OPEN</td>
 * </tr>
 * <tr>
 *     <td>Camera encountered recoverable error while opening</td>
 *     <td>OPENING(Error)</td>
 * </tr>
 * <tr>
 *     <td>Camera encountered critical error while opening</td>
 *     <td>CLOSING(Error)</td>
 * </tr>
 * <tr>
 *     <td>Camera opening failed prematurely</td>
 *     <td>CLOSED(Error)</td>
 * </tr>
 * <tr>
 *     <td>Reached max limit of camera (re)open attempts</td>
 *     <td>PENDING_OPEN</td>
 * </tr>
 * <tr>
 *     <td rowspan="3">OPEN</td>
 *     <td>Camera encountered recoverable error</td>
 *     <td>OPENING(Error)</td>
 * </tr>
 * <tr>
 *     <td>Camera encountered critical error</td>
 *     <td>CLOSING(Error)</td>
 * </tr>
 * <tr>
 *     <td>Received signal to close camera</td>
 *     <td>CLOSING</td>
 * </tr>
 * <tr>
 *     <td>CLOSING</td>
 *     <td>Camera closed</td>
 *     <td>CLOSED</td>
 * </tr>
 * </table>
 *
 * <p>Initially, a camera is in a {@link Type#CLOSED} state. When it receives a signal to open, for
 * example after one or multiple {@linkplain UseCase use cases} are attached to it, its state
 * moves to the {@link Type#OPENING} state. If it successfully opens the camera device, its state
 * moves to the {@link Type#OPEN} state, otherwise, it may move to a different state depending on
 * the error it encountered:
 *
 * <ul>
 * <li>If opening the camera device fails prematurely, for example, when "Do Not Disturb" mode is
 * enabled on a device that's affected by a bug in Android 9 (see
 * {@link #ERROR_DO_NOT_DISTURB_MODE_ENABLED}), the state moves to the {@link Type#CLOSED} state
 * .</li>
 * <li>If the error is recoverable, CameraX will attempt to reopen the camera device. If a recovery
 * attempt succeeds, the camera state moves to the {@link Type#OPEN} state, however, if all recovery
 * attempts are unsuccessful, the camera waits in a {@link Type#PENDING_OPEN} state to attempt
 * recovery again once the camera device's availability changes.</li>
 * <li>If the error is critical, and requires the intervention of the developer or user, the
 * camera's state moves to the {@link Type#CLOSING} state.</li>
 * </ul>
 *
 * <p>While in the {@link Type#PENDING_OPEN} state, the camera waits for a signal indicating the
 * camera device's availability. The signal can either be an external one from the camera service,
 * or an internal one from within CameraX. When received, the camera's state moves to the
 * {@link Type#OPENING} state, and an attempt to open the camera device is made.
 *
 * <p>While in the {@link Type#OPEN} state, the camera device may be disconnected due to an error.
 * In this case, depending on whether the error is critical or recoverable, CameraX may or may not
 * attempt to recover from it, thus the state will move to either a {@link Type#CLOSING} or
 * {@link Type#OPENING} state.
 *
 * <p>If the camera is in an {@link Type#OPEN} state and receives a signal to close the camera
 * device, for example when all its previously attached {@link UseCase use cases} are detached,
 * its state moves to the {@link Type#CLOSING} state. Once the camera device finishes closing,
 * the camera state moves to the {@link Type#CLOSED} state.
 *
 * <p>Whenever the camera encounters an error, it reports it through {@link #getError()}.
 */
@AutoValue
public abstract class CameraState {

    /**
     * An error indicating that the limit number of open cameras has been reached, and more
     * cameras cannot be opened until other instances are closed.
     */
    @SuppressWarnings("MinMaxConstant")
    public static final int ERROR_MAX_CAMERAS_IN_USE = 1;

    /**
     * An error indicating that the camera device is already in use.
     *
     * <p>This could be due to the camera device being used by a higher-priority camera client.
     */
    public static final int ERROR_CAMERA_IN_USE = 2;

    /**
     * An error indicating that the camera device has encountered a recoverable error.
     *
     * <p>CameraX will attempt to recover from the error, it if succeeds in doing so, the
     * camera will open, otherwise the camera will move to a {@link Type#PENDING_OPEN} state.
     *
     * When CameraX uses a {@link android.hardware.camera2} implementation, this error represents
     * a {@link android.hardware.camera2.CameraDevice.StateCallback#ERROR_CAMERA_DEVICE} error.
     */
    public static final int ERROR_OTHER_RECOVERABLE_ERROR = 3;

    /** An error indicating that configuring the camera has failed. */
    public static final int ERROR_STREAM_CONFIG = 4;

    /**
     * An error indicating that the camera device could not be opened due to a device policy.
     *
     * <p>The error may be encountered if a client from a background process attempts to open the
     * camera.
     *
     * @see android.app.admin.DevicePolicyManager#setCameraDisabled(ComponentName, boolean)
     */
    public static final int ERROR_CAMERA_DISABLED = 5;

    /**
     * An error indicating that the camera device was closed due to a fatal error.
     *
     * <p>The error may require the Android device to be shut down and restarted to restore camera
     * function. It may also indicate the existence of a persistent camera hardware problem.
     *
     * When CameraX uses a {@link android.hardware.camera2} implementation, this error represents
     * a {@link android.hardware.camera2.CameraDevice.StateCallback#ERROR_CAMERA_SERVICE} error.
     */
    public static final int ERROR_CAMERA_FATAL_ERROR = 6;

    /**
     * An error indicating that the camera could not be opened because "Do Not Disturb" mode is
     * enabled on devices affected by a bug in Android 9 (API level 28).
     *
     * <p>When "Do Not Disturb" mode is enabled, opening the camera device fails on certain
     * Android devices running on an early Android 9 release with a
     * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}
     * camera hardware level.
     *
     * <p>CameraX will not attempt to reopen the camera device, instead, disable the "Do Not
     * Disturb" mode, then explicitly open the camera again.
     */
    public static final int ERROR_DO_NOT_DISTURB_MODE_ENABLED = 7;

    /**
     * Create a new {@link CameraState} instance from a {@link Type} and a {@code null}
     * {@link StateError}.
     *
     * <p>A {@link CameraState} is not expected to be instantiated in normal operation.
     */
    @NonNull
    public static CameraState create(@NonNull Type type) {
        return create(type, null);
    }

    /**
     * Create a new {@link CameraState} instance from a {@link Type} and a potential
     * {@link StateError}.
     *
     * <p>A {@link CameraState} is not expected to be instantiated in normal operation.
     */
    @NonNull
    public static CameraState create(@NonNull Type type, @Nullable StateError error) {
        return new AutoValue_CameraState(type, error);
    }

    /**
     * Returns the camera's state.
     *
     * @return The camera's state
     */
    @NonNull
    public abstract Type getType();

    /**
     * Potentially returns an error the camera encountered.
     *
     * @return An error the camera encountered, or {@code null} otherwise.
     */
    @Nullable
    public abstract StateError getError();

    @IntDef(value = {
            ERROR_CAMERA_IN_USE,
            ERROR_MAX_CAMERAS_IN_USE,
            ERROR_OTHER_RECOVERABLE_ERROR,
            ERROR_STREAM_CONFIG,
            ERROR_CAMERA_DISABLED,
            ERROR_CAMERA_FATAL_ERROR,
            ERROR_DO_NOT_DISTURB_MODE_ENABLED})
    @Retention(RetentionPolicy.SOURCE)
    @interface ErrorCode {
    }

    /**
     * Types of errors the camera can encounter.
     *
     * <p>CameraX tries to recover from recoverable errors, which include
     * {@link #ERROR_CAMERA_IN_USE}, {@link #ERROR_MAX_CAMERAS_IN_USE} and
     * {@link #ERROR_OTHER_RECOVERABLE_ERROR}. The rest of the errors are critical, and require
     * the intervention of the developer or user to restore camera function. These errors include
     * {@link #ERROR_STREAM_CONFIG}, {@link #ERROR_CAMERA_DISABLED},
     * {@link #ERROR_CAMERA_FATAL_ERROR} and {@link #ERROR_DO_NOT_DISTURB_MODE_ENABLED}.
     */
    public enum ErrorType {
        /**
         * An error the camera encountered that CameraX will attempt to recover from.
         *
         * <p>Recoverable errors include {@link #ERROR_CAMERA_IN_USE},
         * {@link #ERROR_MAX_CAMERAS_IN_USE} and {@link #ERROR_OTHER_RECOVERABLE_ERROR}.
         */
        RECOVERABLE,

        /**
         * An error the camera encountered that CameraX will not attempt to recover from.
         *
         * <p>A critical error is one that requires the intervention of the developer or user to
         * restore camera function, and includes {@link #ERROR_STREAM_CONFIG},
         * {@link #ERROR_CAMERA_DISABLED}, {@link #ERROR_CAMERA_FATAL_ERROR} and
         * {@link #ERROR_DO_NOT_DISTURB_MODE_ENABLED}.
         */
        CRITICAL
    }

    /** States the camera can be in. */
    public enum Type {
        /**
         * Represents a state where the camera is waiting for a signal to attempt to open the camera
         * device.
         *
         * <p>The camera can move to this state from a {@link Type#CLOSED} or {@link Type#OPENING}
         * state:
         * <ul>
         * <li>It moves to this state from a {@link Type#CLOSED} state if it attempts to open an
         * unavailable camera device. A camera device is unavailable for opening if (a) it's
         * already in use by another camera client, for example one with higher priority or (b)
         * the maximum number of cameras allowed to be open at the same time in CameraX has been
         * reached, this limit is currently set to 1.</li>
         * <li>It moves to this state from an {@link Type#OPENING} state if it reaches the
         * maximum number of camera reopen attempts while trying to recover from a camera opening
         * error.</li>
         * </ul>
         *
         * <p>While in this state, the camera waits for an external signal from the camera
         * service or an internal one from CameraX to attempt to reopen the camera device.
         *
         * <p>Developers may rely on this state to close any other open cameras in the app, or
         * request their user close an open camera in another app.
         */
        PENDING_OPEN,

        /**
         * Represents a state where the camera device is currently opening.
         *
         * <p>The camera can move to this state from a {@link Type#PENDING_OPEN} or
         * {@link Type#CLOSED} state: It moves to this state from a {@link Type#PENDING_OPEN}
         * state after it receives a signal that the camera is available to open, and from a
         * {@link Type#CLOSED} state after a request to open the camera is made, and the camera
         * is available to open.
         *
         * <p>While in this state, the camera is actively attempting to open the camera device.
         * This takes several hundred milliseconds on most devices. If it succeeds, the state
         * moves to the {@link Type#OPEN} state. If it fails however, the camera may attempt to
         * reopen the camera device a certain number of times. While this is happening, the
         * camera state remains the same, i.e. in an opening state, and it exposes the error it
         * encountered through {@link #getError()}.
         *
         * <p>Developers can rely on this state to be aware of when the camera is actively
         * attempting to open the camera device, this allows them to communicate it to their
         * users through the UI.
         */
        OPENING,

        /**
         * Represents a state where the camera device is open.
         *
         * <p>The camera can only move to this state from an {@link Type#OPENING} state.
         *
         * <p>Once in this state, active {@linkplain UseCase use cases} attached to this camera
         * could expect to shortly start receiving camera frames.
         *
         * <p>Developers can rely on this state to be notified of when the camera device is actually
         * ready for use, and can then set up camera dependent resources, especially if they're
         * heavyweight.
         */
        OPEN,

        /**
         * Represents a state where the camera device is currently closing.
         *
         * <p>The camera can move to this state from an {@link Type#OPEN} or {@link Type#OPENING}
         * state: It moves to this state from an {@link Type#OPEN} state after it receives a
         * signal to close the camera device, this can be after all its attached
         * {@linkplain UseCase use cases} are detached, and from an {@link Type#OPENING} state if
         * the camera encounters a fatal error it cannot recover from.
         *
         * <p>Developers can rely on this state to be aware of when the camera device is actually
         * in the process of closing. this allows them to communicate it to their users through
         * the UI.
         */
        CLOSING,

        /**
         * Represents a state where the camera device is closed.
         *
         * <p>The camera is initially in this state, and can move back to it from a
         * {@link Type#CLOSING} or {@link Type#OPENING} state: It moves to this state from a
         * {@link Type#CLOSING} state after the camera device successfully closes, and from an
         * {@link Type#OPENING} state when opening a camera device that is unavailable due to
         * {@link android.app.NotificationManager.Policy}, as some API level 28 devices cannot
         * access the camera when the device is in "Do Not Disturb" mode.
         *
         * <p>Developers can rely on this state to be notified of when the camera device is actually
         * closed, and then use this signal to free up camera resources, or start the camera device
         * with another camera client.
         */
        CLOSED
    }

    /**
     * Error that the camera has encountered.
     *
     * <p>The camera may report an error when it's in one of the following states:
     * {@link Type#OPENING}, {@link Type#OPEN}, {@link Type#CLOSING} and {@link Type#CLOSED}.
     *
     * <p>CameraX attempts to recover from certain errors it encounters when opening the camera
     * device, in these instances, the error is {@linkplain ErrorType#RECOVERABLE recoverable},
     * otherwise, the error is {@linkplain ErrorType#CRITICAL critical}.
     *
     * <p>When CameraX encounters a critical error, the developer and/or user must intervene to
     * restore camera function. When the error is recoverable, the developer and/or user can
     * still aid in the recovery process, as shown in the following table.
     * <table>
     * <tr>
     *     <th>State</th>
     *     <th>Error Code</th>
     *     <th>Recoverable</th>
     *     <th>How to handle it</th>
     * </tr>
     * <tr>
     *     <td>{@link Type#OPEN}</td>
     *     <td>{@linkplain #ERROR_STREAM_CONFIG ERROR_STREAM_CONFIG}</td>
     *     <td>No</td>
     *     <td>Make sure you set up your {@linkplain UseCase use cases} correctly.</td>
     * </tr>
     * <tr>
     *     <td>{@link Type#OPENING}</td>
     *     <td>{@linkplain #ERROR_CAMERA_IN_USE ERROR_CAMERA_IN_USE}</td>
     *     <td>Yes</td>
     *     <td>Close the camera, or ask the user to close another camera app that is using the
     *     camera.</td>
     * </tr>
     * <tr>
     *     <td>{@link Type#OPENING}</td>
     *     <td>{@linkplain #ERROR_MAX_CAMERAS_IN_USE ERROR_MAX_CAMERAS_IN_USE}</td>
     *     <td>Yes</td>
     *     <td>Close another open camera in the app, or ask the user to close another camera
     *     app that's using the camera.</td>
     * </tr>
     * <tr>
     *     <td>{@link Type#OPENING}</td>
     *     <td>{@linkplain #ERROR_OTHER_RECOVERABLE_ERROR ERROR_OTHER_RECOVERABLE_ERROR}</td>
     *     <td>Yes</td>
     *     <td>N/A</td>
     * </tr>
     * <tr>
     *     <td>{@link Type#CLOSING}</td>
     *     <td>{@linkplain #ERROR_CAMERA_DISABLED ERROR_CAMERA_DISABLED}</td>
     *     <td>No</td>
     *     <td>Ask the user to enable the device's cameras.</td>
     * </tr>
     * <tr>
     *     <td>{@link Type#CLOSING}</td>
     *     <td>{@linkplain #ERROR_CAMERA_FATAL_ERROR ERROR_CAMERA_FATAL_ERROR}</td>
     *     <td>No</td>
     *     <td>Ask the user to reboot the device to restore camera function.</td>
     * </tr>
     * <tr>
     *     <td>{@link Type#CLOSED}</td>
     *     <td>{@linkplain #ERROR_DO_NOT_DISTURB_MODE_ENABLED
     *     ERROR_DO_NOT_DISTURB_MODE_ENABLED}</td>
     *     <td>No</td>
     *     <td>Ask the user to disable "Do Not Disturb" mode, then open the camera again.</td>
     * </tr>
     * </table>
     */
    @AutoValue
    public abstract static class StateError {

        /**
         * Creates a {@link StateError} with an error code.
         *
         * <p>A {@link StateError} is not expected to be instantiated in normal operation.
         */
        @NonNull
        public static StateError create(@ErrorCode int error) {
            return create(error, null);
        }

        /**
         * Creates a {@link StateError} with an error code and a {@linkplain Throwable cause}.
         *
         * <p>A {@link StateError} is not expected to be instantiated in normal operation.
         */
        @NonNull
        public static StateError create(@ErrorCode int error, @Nullable Throwable cause) {
            return new AutoValue_CameraState_StateError(error, cause);
        }

        /**
         * Returns the code of this error.
         *
         * <p>The error's code is one of the following: {@link #ERROR_CAMERA_IN_USE},
         * {@link #ERROR_MAX_CAMERAS_IN_USE}, {@link #ERROR_OTHER_RECOVERABLE_ERROR},
         * {@link #ERROR_STREAM_CONFIG}, {@link #ERROR_CAMERA_DISABLED},
         * {@link #ERROR_CAMERA_FATAL_ERROR} and {@link #ERROR_DO_NOT_DISTURB_MODE_ENABLED}.
         *
         * @return The code of this error.
         */
        @ErrorCode
        public abstract int getCode();

        /**
         * Returns a potential cause of this error.
         *
         * @return The cause of this error, or {@code null} if the cause was not supplied.
         */
        @Nullable
        public abstract Throwable getCause();

        /**
         * Returns the type of this error.
         *
         * <p>An error can either be {@linkplain ErrorType#RECOVERABLE recoverable} or
         * {@linkplain ErrorType#CRITICAL critical}.
         *
         * @return The type of this error
         */
        @NonNull
        public ErrorType getType() {
            int code = getCode();
            if (code == ERROR_CAMERA_IN_USE || code == ERROR_MAX_CAMERAS_IN_USE
                    || code == ERROR_OTHER_RECOVERABLE_ERROR) {
                return ErrorType.RECOVERABLE;
            }
            return ErrorType.CRITICAL;
        }
    }
}
