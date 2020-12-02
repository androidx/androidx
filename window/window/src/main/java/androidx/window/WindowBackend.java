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
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
 * Backing interface for {@link WindowManager} instances that server as the default information
 * supplier.
 */
public interface WindowBackend {

    /**
     * Registers a callback for layout changes of the window for the supplied {@link Activity}.
     * Must be called only after the it is attached to the window.
     */
    void registerLayoutChangeCallback(@NonNull Activity activity, @NonNull Executor executor,
            @NonNull Consumer<WindowLayoutInfo> callback);

    /**
     * Unregisters a callback for window layout changes of the {@link Activity} window.
     */
    void unregisterLayoutChangeCallback(@NonNull Consumer<WindowLayoutInfo> callback);

    /**
     * Registers a callback for device state changes.
     */
    void registerDeviceStateChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<DeviceState> callback);

    /**
     * Unregisters a callback for device state changes.
     */
    void unregisterDeviceStateChangeCallback(@NonNull Consumer<DeviceState> callback);
}
