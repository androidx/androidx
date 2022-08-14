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

package androidx.window.extensions.area;

import android.app.Activity;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.window.extensions.WindowExtensions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

/**
 * The interface definition that will be used by the WindowManager library to get custom
 * OEM-provided behavior around moving windows between displays or display areas on a device.
 *
 * Currently the only behavior supported is RearDisplay Mode, where the window
 * is moved to the display that faces the same direction as the rear camera.
 *
 * <p>This interface should be implemented by OEM and deployed to the target devices.
 * @see WindowExtensions#getWindowLayoutComponent()
 */
public interface WindowAreaComponent {

    /**
     * WindowArea status constant to signify that the feature is
     * unsupported on this device. Could be due to the device not supporting that
     * specific feature.
     */
    int STATUS_UNSUPPORTED = 0;

    /**
     * WindowArea status constant to signify that the feature is
     * currently unavailable but is supported on this device. This value could signify
     * that the current device state does not support the specific feature or another
     * process is currently enabled in that feature.
     */
    int STATUS_UNAVAILABLE = 1;

    /**
     * WindowArea status constant to signify that the feature is
     * available to be entered or enabled.
     */
    int STATUS_AVAILABLE = 2;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            STATUS_UNSUPPORTED,
            STATUS_UNAVAILABLE,
            STATUS_AVAILABLE
    })
    @interface WindowAreaStatus {}

    /**
     * Session state constant to represent there being no active session
     * currently in progress. Used by the library to call the correct callbacks if
     * a session is ended.
     */
    int SESSION_STATE_INACTIVE = 0;

    /**
     * Session state constant to represent that there is an
     * active session currently in progress. Used by the library to
     * know when to return the session object to the developer when the
     * session is created and active.
     */
    int SESSION_STATE_ACTIVE = 1;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SESSION_STATE_ACTIVE,
            SESSION_STATE_INACTIVE
    })
    @interface WindowAreaSessionState {}

    /**
     * Adds a listener interested in receiving updates on the RearDisplayStatus
     * of the device. Because this is being called from the OEM provided
     * extensions, the library will post the result of the listener on the executor
     * provided by the developer.
     *
     * The listener provided will receive values that
     * correspond to the [WindowAreaStatus] value that aligns with the current status
     * of the rear display.
     * @param consumer interested in receiving updates to WindowAreaStatus.
     */
    void addRearDisplayStatusListener(@NonNull Consumer<Integer> consumer);

    /**
     * Removes a listener no longer interested in receiving updates.
     * @param consumer no longer interested in receiving updates to WindowAreaStatus
     */
    void removeRearDisplayStatusListener(@NonNull Consumer<Integer> consumer);


    /**
     * Creates and starts a rear display session and sends state updates to the
     * consumer provided. This consumer will receive a constant represented by
     * [WindowAreaSessionState] to represent the state of the current rear display
     * session. We will translate to a more friendly interface in the library.
     *
     * Because this is being called from the OEM provided extensions, the library
     * will post the result of the listener on the executor provided by the developer.
     *
     * @param activity to allow that the OEM implementation will use as a base
     * context and to identify the source display area of the request.
     * The reference to the activity instance must not be stored in the OEM
     * implementation to prevent memory leaks.
     * @param consumer to provide updates to the client on the status of the session
     * @throws UnsupportedOperationException if this method is called when RearDisplay
     * mode is not available. This could be to an incompatible device state or when
     * another process is currently in this mode.
     */
    @SuppressWarnings("ExecutorRegistration") // Jetpack will post it on the app-provided executor.
    void startRearDisplaySession(@NonNull Activity activity,
            @NonNull Consumer<Integer> consumer);

    /**
     * Ends a RearDisplaySession and sends [STATE_INACTIVE] to the consumer
     * provided in the {@code startRearDisplaySession} method. This method is only
     * called through the {@code RearDisplaySession} provided to the developer.
     */
    void endRearDisplaySession();
}
