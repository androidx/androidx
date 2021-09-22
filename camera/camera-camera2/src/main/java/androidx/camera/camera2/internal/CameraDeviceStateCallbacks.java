/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraDevice;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Different implementations of {@link CameraDevice.StateCallback}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraDeviceStateCallbacks {
    private CameraDeviceStateCallbacks() {
    }

    /**
     * Returns a device state callback which does nothing.
     */
    @NonNull
    public static CameraDevice.StateCallback createNoOpCallback() {
        return new NoOpDeviceStateCallback();
    }

    /**
     * Returns a device state callback which calls a list of other callbacks.
     */
    @NonNull
    public static CameraDevice.StateCallback createComboCallback(
            @NonNull List<CameraDevice.StateCallback> callbacks) {
        if (callbacks.isEmpty()) {
            return createNoOpCallback();
        } else if (callbacks.size() == 1) {
            return callbacks.get(0);
        }
        return new ComboDeviceStateCallback(callbacks);
    }

    /**
     * Returns a device state callback which calls a list of other callbacks.
     */
    @NonNull
    public static CameraDevice.StateCallback createComboCallback(
            @NonNull CameraDevice.StateCallback... callbacks) {
        return createComboCallback(Arrays.asList(callbacks));
    }

    static final class NoOpDeviceStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
        }
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    private static final class ComboDeviceStateCallback extends CameraDevice.StateCallback {
        private final List<CameraDevice.StateCallback> mCallbacks = new ArrayList<>();

        ComboDeviceStateCallback(@NonNull List<CameraDevice.StateCallback> callbacks) {
            for (CameraDevice.StateCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                if (!(callback instanceof NoOpDeviceStateCallback)) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            for (CameraDevice.StateCallback callback : mCallbacks) {
                callback.onOpened(cameraDevice);
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            for (CameraDevice.StateCallback callback : mCallbacks) {
                callback.onClosed(cameraDevice);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            for (CameraDevice.StateCallback callback : mCallbacks) {
                callback.onDisconnected(cameraDevice);
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            for (CameraDevice.StateCallback callback : mCallbacks) {
                callback.onError(cameraDevice, error);
            }
        }
    }
}
