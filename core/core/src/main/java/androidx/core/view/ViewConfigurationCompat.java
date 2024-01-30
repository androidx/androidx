/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.view;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Supplier;

import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link ViewConfiguration}.
 */
@SuppressWarnings("JavaReflectionMemberAccess")
public final class ViewConfigurationCompat {
    private static final String TAG = "ViewConfigCompat";

    /** Value used as a minimum fling velocity, when fling is not supported. */
    private static final int NO_FLING_MIN_VELOCITY = Integer.MAX_VALUE;

    /** Value used as a maximum fling velocity, when fling is not supported. */
    private static final int NO_FLING_MAX_VELOCITY = Integer.MIN_VALUE;

    private static final int RESOURCE_ID_SUPPORTED_BUT_NOT_FOUND = 0;
    private static final int RESOURCE_ID_NOT_SUPPORTED = -1;

    private static Method sGetScaledScrollFactorMethod;

    static {
        if (Build.VERSION.SDK_INT == 25) {
            try {
                sGetScaledScrollFactorMethod =
                        ViewConfiguration.class.getDeclaredMethod("getScaledScrollFactor");
            } catch (Exception e) {
                Log.i(TAG, "Could not find method getScaledScrollFactor() on ViewConfiguration");
            }
        }
    }

    /**
     * Call {@link ViewConfiguration#getScaledPagingTouchSlop()}.
     *
     * @deprecated Call {@link ViewConfiguration#getScaledPagingTouchSlop()} directly.
     * This method will be removed in a future release.
     */
    @Deprecated
    public static int getScaledPagingTouchSlop(ViewConfiguration config) {
        return config.getScaledPagingTouchSlop();
    }

    /**
     * Report if the device has a permanent menu key available to the user, in a backwards
     * compatible way.
     *
     * @deprecated Use {@link ViewConfiguration#hasPermanentMenuKey()} directly.
     */
    @Deprecated
    public static boolean hasPermanentMenuKey(ViewConfiguration config) {
        return config.hasPermanentMenuKey();
    }

    /**
     * @param config Used to get the scaling factor directly from the {@link ViewConfiguration}.
     * @param context Used to locate a resource value.
     *
     * @return Amount to scroll in response to a horizontal {@link MotionEventCompat#ACTION_SCROLL}
     *         event. Multiply this by the event's axis value to obtain the number of pixels to be
     *         scrolled.
     */
    public static float getScaledHorizontalScrollFactor(@NonNull ViewConfiguration config,
            @NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getScaledHorizontalScrollFactor(config);
        } else {
            return getLegacyScrollFactor(config, context);
        }
    }

    /**
     * @param config Used to get the scaling factor directly from the {@link ViewConfiguration}.
     * @param context Used to locate a resource value.
     *
     * @return Amount to scroll in response to a vertical {@link MotionEventCompat#ACTION_SCROLL}
     *         event. Multiply this by the event's axis value to obtain the number of pixels to be
     *         scrolled.
     */
    public static float getScaledVerticalScrollFactor(@NonNull ViewConfiguration config,
            @NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getScaledVerticalScrollFactor(config);
        } else {
            return getLegacyScrollFactor(config, context);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static float getLegacyScrollFactor(ViewConfiguration config, Context context) {
        if (Build.VERSION.SDK_INT >= 25 && sGetScaledScrollFactorMethod != null) {
            try {
                return (int) sGetScaledScrollFactorMethod.invoke(config);
            } catch (Exception e) {
                Log.i(TAG, "Could not find method getScaledScrollFactor() on ViewConfiguration");
            }
        }
        // Fall back to pre-API-25 behavior.
        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, outValue, true)) {
            return outValue.getDimension(context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    /**
     * @param config Used to get the hover slop directly from the {@link ViewConfiguration}.
     *
     * @return The hover slop value.
     */
    public static int getScaledHoverSlop(@NonNull ViewConfiguration config) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getScaledHoverSlop(config);
        }
        return config.getScaledTouchSlop() / 2;
    }

    /**
     * Check if shortcuts should be displayed in menus.
     *
     * @return {@code True} if shortcuts should be displayed in menus.
     */
    public static boolean shouldShowMenuShortcutsWhenKeyboardPresent(
            @NonNull ViewConfiguration config,
            @NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.shouldShowMenuShortcutsWhenKeyboardPresent(config);
        }
        final Resources res = context.getResources();
        final int platformResId =
                getPlatformResId(res, "config_showMenuShortcutsWhenKeyboardPresent", "bool");
        return platformResId != 0 && res.getBoolean(platformResId);
    }

    /**
     * Minimum absolute value of velocity to initiate a fling for a motion generated by an
     * {@link InputDevice} with an id of {@code inputDeviceId}, from an input {@code source} and on
     * a given motion event {@code axis}.
     *
     * <p>Before utilizing this method to get a minimum fling velocity for a motion generated by the
     * input device, scale the velocity of the motion events generated by the input device to pixels
     * per second.
     *
     * <p>For instance, if you tracked {@link MotionEvent#AXIS_SCROLL} vertical velocities generated
     * from a {@link InputDevice#SOURCE_ROTARY_ENCODER}, the velocity returned from
     * {@link VelocityTracker} will be in the units with which the axis values were reported in the
     * motion event. Before comparing that velocity against the minimum fling velocity specified
     * here, make sure that the {@link MotionEvent#AXIS_SCROLL} velocity from the tracker is
     * calculated in "units per second" (see {@link VelocityTracker#computeCurrentVelocity(int)},
     * {@link VelocityTracker#computeCurrentVelocity(int, float)} to adjust your velocity
     * computations to "per second"), and use {@link #getScaledVerticalScrollFactor} to change this
     * velocity value to "pixels/second".
     *
     * <p>If the provided {@code inputDeviceId} is not valid, or if the input device whose ID is
     * provided does not support the given motion event source and/or axis, this method will return
     * {@code Integer.MAX_VALUE}.
     *
     * <h3>Obtaining the correct arguments for this method call</h3>
     * <p><b>inputDeviceId</b>: if calling this method in response to a {@link MotionEvent}, use
     * the device ID that is reported by the event, which can be obtained using
     * {@link MotionEvent#getDeviceId()}. Otherwise, use a valid ID that is obtained from
     * {@link InputDevice#getId()}, or from an {@link InputManager} instance
     * ({@link InputManager#getInputDeviceIds()} gives all the valid input device IDs).
     *
     * <p><b>axis</b>: a {@link MotionEvent} may report data for multiple axes, and each axis may
     * have multiple data points for different pointers. Use the axis for which you obtained the
     * velocity for ({@link VelocityTracker} lets you calculate velocities for a specific axis. Use
     * the axis for which you calculated velocity). You can use
     * {@link InputDevice#getMotionRanges()} to get all the {@link InputDevice.MotionRange}s for the
     * {@link InputDevice}, from which you can derive all the valid axes for the device.
     *
     * <p><b>source</b>: use {@link MotionEvent#getSource()} if calling this method in response to a
     * {@link MotionEvent}. Otherwise, use a valid source for the {@link InputDevice}. You can use
     * {@link InputDevice#getMotionRanges()} to get all the {@link InputDevice.MotionRange}s for the
     * {@link InputDevice}, from which you can derive all the valid sources for the device.
     *
     *
     * <p>This method optimizes calls over multiple input device IDs, so caching the return value of
     * the method is not necessary if you are handling multiple input devices.
     *
     * @param context the {@link Context} associated with the view.
     * @param config the {@link ViewConfiguration} to derive the minimum fling velocity from.
     * @param inputDeviceId the ID of the {@link InputDevice} that generated the motion triggering
     *          fling.
     * @param axis the axis on which the motion triggering the fling happened. This axis should be
     *          a valid axis that can be reported by the provided input device from the provided
     *          input device source.
     * @param source the input source of the motion causing fling. This source should be a valid
     *          source for the {@link InputDevice} whose ID is {@code inputDeviceId}.
     *
     * @return the minimum velocity, in pixels/second, to trigger fling.
     *
     * @see InputDevice#getMotionRange(int, int)
     * @see InputDevice#getMotionRanges()
     * @see VelocityTracker#getAxisVelocity(int, int)
     * @see VelocityTracker#getAxisVelocity(int)
     */
    public static int getScaledMinimumFlingVelocity(
            @NonNull Context context,
            @NonNull ViewConfiguration config,
            int inputDeviceId,
            int axis,
            int source) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.getScaledMinimumFlingVelocity(config, inputDeviceId, axis, source);
        }

        if (!isInputDeviceInfoValid(inputDeviceId, axis, source)) {
            return NO_FLING_MIN_VELOCITY;
        }

        Resources res = context.getResources();
        return getCompatFlingVelocityThreshold(
                res,
                getPreApi34MinimumFlingVelocityResId(res, source, axis),
                config::getScaledMinimumFlingVelocity,
                NO_FLING_MIN_VELOCITY);
    }

    /**
     * Maximum absolute value of velocity to initiate a fling for a motion generated by an
     * {@link InputDevice} with an id of {@code inputDeviceId}, from an input {@code source} and on
     * a given motion event {@code axis}.
     *
     * <p>Similar to
     * {@link #getScaledMinimumFlingVelocity(Context, ViewConfiguration, int, int, int)}, but for
     * maximum fling velocity, instead of minimum. Also, unlike that method which returns
     * {@code Integer.MAX_VALUE} for bad input device ID, source and/or motion event axis inputs,
     * this method returns {@code Integer.MIN_VALUE} for such bad inputs.
     */
    public static int getScaledMaximumFlingVelocity(
            @NonNull Context context,
            @NonNull ViewConfiguration config,
            int inputDeviceId,
            int axis,
            int source) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.getScaledMaximumFlingVelocity(config, inputDeviceId, axis, source);
        }

        if (!isInputDeviceInfoValid(inputDeviceId, axis, source)) {
            return NO_FLING_MAX_VELOCITY;
        }

        Resources res = context.getResources();
        return getCompatFlingVelocityThreshold(
                res,
                getPreApi34MaximumFlingVelocityResId(res, source, axis),
                config::getScaledMaximumFlingVelocity,
                NO_FLING_MAX_VELOCITY);
    }

    private ViewConfigurationCompat() {
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static float getScaledHorizontalScrollFactor(ViewConfiguration viewConfiguration) {
            return viewConfiguration.getScaledHorizontalScrollFactor();
        }

        @DoNotInline
        static float getScaledVerticalScrollFactor(ViewConfiguration viewConfiguration) {
            return viewConfiguration.getScaledVerticalScrollFactor();
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getScaledHoverSlop(ViewConfiguration viewConfiguration) {
            return viewConfiguration.getScaledHoverSlop();
        }

        @DoNotInline
        static boolean shouldShowMenuShortcutsWhenKeyboardPresent(
                ViewConfiguration viewConfiguration) {
            return viewConfiguration.shouldShowMenuShortcutsWhenKeyboardPresent();
        }
    }

    @RequiresApi(34)
    static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getScaledMaximumFlingVelocity(
                @NonNull ViewConfiguration viewConfiguration,
                int inputDeviceId,
                int axis,
                int source) {
            return viewConfiguration.getScaledMaximumFlingVelocity(inputDeviceId, axis, source);
        }

        @DoNotInline
        static int getScaledMinimumFlingVelocity(
                @NonNull ViewConfiguration viewConfiguration,
                int inputDeviceId,
                int axis,
                int source) {
            return viewConfiguration.getScaledMinimumFlingVelocity(inputDeviceId, axis, source);
        }
    }

    private static int getPreApi34MaximumFlingVelocityResId(Resources res, int source, int axis) {
        if (source == InputDeviceCompat.SOURCE_ROTARY_ENCODER && axis == MotionEvent.AXIS_SCROLL) {
            return getPlatformResId(res, "config_viewMaxRotaryEncoderFlingVelocity", "dimen");
        }
        // A compat fling velocity threshold is not supported.
        return RESOURCE_ID_NOT_SUPPORTED;
    }

    private static int getPreApi34MinimumFlingVelocityResId(Resources res, int source, int axis) {
        if (source == InputDeviceCompat.SOURCE_ROTARY_ENCODER && axis == MotionEvent.AXIS_SCROLL) {
            return getPlatformResId(res, "config_viewMinRotaryEncoderFlingVelocity", "dimen");
        }
        // A compat fling velocity threshold is not supported.
        return RESOURCE_ID_NOT_SUPPORTED;
    }

    private static int getPlatformResId(Resources res, String name, String defType) {
        return res.getIdentifier(name, defType, /* defPackage= */ "android");
    }

    private static boolean isInputDeviceInfoValid(int id, int axis, int source) {
        InputDevice device = InputDevice.getDevice(id);
        return device != null && device.getMotionRange(axis, source) != null;
    }

    /**
     * Provides a compat fling velocity threshold by looking up the value of the compat
     * threshold specifier platform resource (provided via {@code platformResId}).
     *
     * <p>If the resource ID is not found, it returns the {@code noFlingThreshold}, to avoid
     * forcing fling to older Android versions that did not intentionally add these compat
     * resources. Exception is in the case of {@link #RESOURCE_ID_NOT_SUPPORTED}, where
     * specifying a compat resource ID is not supported by the compat class, in which case we
     * return the default fling velocity via the {@code defaultThresholdSupplier}.
     */
    private static int getCompatFlingVelocityThreshold(
            Resources res,
            int platformResId,
            Supplier<Integer> defaultThresholdSupplier,
            int noFlingThreshold) {
        switch (platformResId) {
            case RESOURCE_ID_NOT_SUPPORTED:
                // Because compat fling velocity threshold is not supported, use the default fling
                // velocity threshold.
                return defaultThresholdSupplier.get();
            case RESOURCE_ID_SUPPORTED_BUT_NOT_FOUND:
                // A compat fling velocity threshold is supported, but the device has not added the
                // compat resources. To not "force" fling on this device, block fling.
                return noFlingThreshold;
            default:
                int threshold = res.getDimensionPixelSize(platformResId);
                // Thresholds should be non-negative. A negative threshold is considered as
                // "no-fling".
                return threshold < 0 ? noFlingThreshold : threshold;
        }
    }
}
