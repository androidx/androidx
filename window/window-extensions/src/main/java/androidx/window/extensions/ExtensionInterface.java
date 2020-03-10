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

package androidx.window.extensions;

import android.os.IBinder;

import androidx.annotation.NonNull;

/**
 * Main Extension interface definition that will be used by the WindowManager library to get custom
 * OEM-provided information that isn't covered by platform APIs.
 *
 * <p>This interface should be implemented by OEM and deployed to the target devices.
 *
 * @see ExtensionProvider
 */
public interface ExtensionInterface {

    /**
     * Registers the support library as the callback for the extension. This interface will be used
     * to report all extension changes to the support library.
     */
    void setExtensionCallback(@NonNull ExtensionCallback callback);

    /**
     * Gets current information about the display features present within the application window.
     */
    @NonNull
    ExtensionWindowLayoutInfo getWindowLayoutInfo(@NonNull IBinder windowToken);

    /**
     * Notifies extension that a listener for display feature layout changes was registered for the
     * given window token.
     */
    void onWindowLayoutChangeListenerAdded(@NonNull IBinder windowToken);

    /**
     * Notifies extension that a listener for display feature layout changes was removed for the
     * given window token.
     */
    void onWindowLayoutChangeListenerRemoved(@NonNull IBinder windowToken);

    /**
     * Gets current device state.
     * @see #onDeviceStateListenersChanged(boolean)
     */
    @NonNull
    ExtensionDeviceState getDeviceState();

    /**
     * Notifies the extension that a device state change listener was updated.
     * @param isEmpty flag indicating if the list of device state change listeners is empty.
     */
    void onDeviceStateListenersChanged(boolean isEmpty);

    /**
     * Callback that will be registered with the WindowManager library, and that the extension
     * should use to report all state changes.
     */
    interface ExtensionCallback {
        /**
         * Called by extension when the device state changes.
         */
        void onDeviceStateChanged(@NonNull ExtensionDeviceState newDeviceState);

        /**
         * Called by extension when the feature layout inside the window changes.
         */
        void onWindowLayoutChanged(@NonNull IBinder windowToken,
                @NonNull ExtensionWindowLayoutInfo newLayout);
    }
}
