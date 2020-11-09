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
import android.graphics.Rect;

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
     * Activity that was registered with this instance of {@link WindowManager} at creation.
     * This is used to find the token identifier of the window when requesting layout information
     * from the {@link androidx.window.sidecar.SidecarInterface} or is passed directly to the
     * {@link ExtensionInterface}.
     */
    private Activity mActivity;
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
     */
    public WindowManager(@NonNull Context context) {
        this(context, ExtensionWindowBackend.getInstance(context));
    }

    /**
     * Gets an instance of the class initialized with and connected to the provided {@link Context}.
     * All methods of this class will return information that is associated with this visual
     * context.
     * @param context A visual context, such as an {@link Activity} or a {@link ContextWrapper}
     *                around one, to use for initialization.
     * @param windowBackend Backing server class that will provide information for this instance.
     *                      Pass a custom {@link WindowBackend} implementation for testing.
     */
    public WindowManager(@NonNull Context context, @NonNull WindowBackend windowBackend) {
        Activity activity = getActivityFromContext(context);
        if (activity == null) {
            throw new IllegalArgumentException("Used non-visual Context to obtain an instance of "
                    + "WindowManager. Please use an Activity or a ContextWrapper around one "
                    + "instead.");
        }
        mActivity = activity;
        mWindowBackend = windowBackend;
    }

    /**
     * Registers a callback for layout changes of the window of the current visual {@link Context}.
     * Must be called only after the it is attached to the window.
     * @see Activity#onAttachedToWindow()
     */
    public void registerLayoutChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<WindowLayoutInfo> callback) {
        mWindowBackend.registerLayoutChangeCallback(mActivity, executor, callback);
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
     * Returns the {@link WindowMetrics} according to the current system state.
     * <p>
     * The metrics describe the size of the area the window would occupy with
     * {@link android.view.WindowManager.LayoutParams#MATCH_PARENT MATCH_PARENT} width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     * <p>
     * The value of this is based on the <b>current</b> windowing state of the system. For
     * example, for activities in multi-window mode, the metrics returned are based on the
     * current bounds that the user has selected for the {@link android.app.Activity Activity}'s
     * window.
     *
     * @see #getMaximumWindowMetrics()
     * @see android.view.WindowManager#getCurrentWindowMetrics()
     */
    @NonNull
    public WindowMetrics getCurrentWindowMetrics() {
        Rect currentBounds = WindowBoundsHelper.getInstance().computeCurrentWindowBounds(mActivity);
        return new WindowMetrics(currentBounds);
    }

    /**
     * Returns the largest {@link WindowMetrics} an app may expect in the current system state.
     * <p>
     * The metrics describe the size of the largest potential area the window might occupy with
     * {@link android.view.WindowManager.LayoutParams#MATCH_PARENT MATCH_PARENT} width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     * <p>
     * The value of this is based on the largest <b>potential</b> windowing state of the system.
     * For example, for activities in multi-window mode the metrics returned are based on what the
     * bounds would be if the user expanded the window to cover the entire screen.
     * <p>
     * Note that this might still be smaller than the size of the physical display if certain
     * areas of the display are not available to windows created for the associated {@link Context}.
     * For example, devices with foldable displays that wrap around the enclosure may split the
     * physical display into different regions, one for the front and one for the back, each acting
     * as different logical displays. In this case {@link #getMaximumWindowMetrics()} would return
     * the region describing the side of the device the associated {@link Context context's}
     * window is placed.
     *
     * @see #getCurrentWindowMetrics()
     * @see android.view.WindowManager#getMaximumWindowMetrics()
     */
    @NonNull
    public WindowMetrics getMaximumWindowMetrics() {
        Rect maxBounds = WindowBoundsHelper.getInstance().computeMaximumWindowBounds(mActivity);
        return new WindowMetrics(maxBounds);
    }

    /**
     * Unwraps the hierarchy of {@link ContextWrapper}-s until {@link Activity} is reached.
     * @return Base {@link Activity} context or {@code null} if not available.
     */
    @Nullable
    private static Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
