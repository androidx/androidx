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

package androidx.window;

import android.app.Activity;

import androidx.annotation.NonNull;

/**
 * Base interface for different extension versions that serves as an API compatibility wrapper.
 * @see ExtensionCompat
 * @see SidecarCompat
 */
interface ExtensionInterfaceCompat {

    /**
     * Verifies if the extension interface conforms to the declared version.
     */
    boolean validateExtensionInterface();

    /**
     * Sets the callback that is used by the extension to inform about hardware state changes.
     */
    void setExtensionCallback(@NonNull ExtensionCallbackInterface extensionCallback);

    /**
     * Notifies extension that a listener for display feature layout changes was registered for the
     * given activity context. Should notify the {@link ExtensionCallbackInterface} of
     * the initial {@link WindowLayoutInfo} when it is available.
     */
    void onWindowLayoutChangeListenerAdded(@NonNull Activity activity);

    /**
     * Notifies extension that a listener for display feature layout changes was removed for the
     * given activity context.
     */
    void onWindowLayoutChangeListenerRemoved(@NonNull Activity activity);

    /**
     * Notifies the extension that a device state change listener was updated. Should notify the
     * {@link ExtensionCallbackInterface} of the initial {@link DeviceState} when it is available.
     * @param isEmpty flag indicating if the list of device state change listeners is empty.
     */
    void onDeviceStateListenersChanged(boolean isEmpty);

    /**
     * Callback that will be registered with the WindowManager library, and that the extension
     * should use to report all state changes.
     */
    interface ExtensionCallbackInterface {
        /**
         * Called by extension when the device state changes.
         */
        void onDeviceStateChanged(@NonNull DeviceState newDeviceState);

        /**
         * Called by extension when the feature layout inside the window changes.
         */
        void onWindowLayoutChanged(@NonNull Activity activity,
                @NonNull WindowLayoutInfo newLayout);
    }
}
