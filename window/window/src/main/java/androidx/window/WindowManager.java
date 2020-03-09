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
import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.window.extensions.ExtensionInterface;

import java.util.concurrent.Executor;

/**
 * Main interaction point with the WindowManager library. An instance of this class allows
 * polling the current state of the device and display, and registering callbacks for changes in
 * the corresponding states.
 */
public final class WindowManager {
    /**
     * Visual context that was registered with this instance of {@link WindowManager} at creation.
     * This is used to find the token identifier of the window when requesting layout information
     * from the {@link ExtensionInterface}.
     */
    private Context mContext;
    /**
     * The backend that supplies the information through this class.
     */
    private WindowBackend mWindowBackend;

    /**
     * Gets an instance of the class initialized with and connected to the provided {@link Context}.
     * All methods of this class will return information that is associated with this visual
     * context.
     * @param context A visual context, such as an {@link Activity} or a {@link ContextWrapper}
     *                around one, to use for initialization.
     * @param windowBackend Backing server class that will provide information for this instance.
     *                      Pass a custom {@link WindowBackend} implementation for testing,
     *                      or {@code null} to use the default implementation.
     */
    public WindowManager(@NonNull Context context, @Nullable WindowBackend windowBackend) {
        if (getActivityFromContext(context) == null) {
            throw new IllegalArgumentException("Used non-visual Context to obtain an instance of "
                    + "WindowManager. Please use an Activity or a ContextWrapper around one "
                    + "instead.");
        }
        mContext = context;
        mWindowBackend = windowBackend != null
                ? windowBackend : ExtensionWindowBackend.getInstance(context);
    }

    /**
     * Gets current window layout information for the associated {@link Context}. Must be called
     * only after the it is attached to the window and the layout pass has happened.
     * @see Activity#onAttachedToWindow()
     * @see WindowLayoutInfo
     */
    @NonNull
    public WindowLayoutInfo getWindowLayoutInfo() {
        return mWindowBackend.getWindowLayoutInfo(mContext);
    }

    /**
     * Gets the current device state.
     * @see DeviceState
     */
    @NonNull
    public DeviceState getDeviceState() {
        return mWindowBackend.getDeviceState();
    }

    /**
     * Registers a callback for layout changes of the window of the current visual {@link Context}.
     * Must be called only after the it is attached to the window.
     * @see Activity#onAttachedToWindow()
     */
    public void registerLayoutChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<WindowLayoutInfo> callback) {
        mWindowBackend.registerLayoutChangeCallback(mContext, executor, callback);
    }

    /**
     * Unregisters a callback for window layout changes of the window.
     */
    public void unregisterLayoutChangeCallback(@NonNull Consumer<WindowLayoutInfo> callback) {
        mWindowBackend.unregisterLayoutChangeCallback(callback);
    }

    /**
     * Registers a callback for device state changes.
     */
    public void registerDeviceStateChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<DeviceState> callback) {
        mWindowBackend.registerDeviceStateChangeCallback(executor, callback);
    }

    /**
     * Unregisters a callback for device state changes.
     */
    public void unregisterDeviceStateChangeCallback(@NonNull Consumer<DeviceState> callback) {
        mWindowBackend.unregisterDeviceStateChangeCallback(callback);
    }

    /**
     * Unwraps the hierarchy of {@link ContextWrapper}-s until {@link Activity} is reached.
     * @return Base {@link Activity} context or {@code null} if not available.
     */
    @Nullable
    static Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
