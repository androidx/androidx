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
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link ViewConfiguration}.
 */
@SuppressWarnings("JavaReflectionMemberAccess")
public final class ViewConfigurationCompat {
    private static final String TAG = "ViewConfigCompat";

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
     * @return the maximum velocity, in pixels/second, to trigger fling. If fling is not
     *         supported for the given arguments, returns {@link Integer#MAX_VALUE}.
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

        Resources res = context.getResources();
        int platformResId = getPreApi34MinimumFlingVelocityResId(res, source, axis);
        if (platformResId != 0) {
            int minFlingVelocity = res.getDimensionPixelSize(platformResId);
            return minFlingVelocity < 0 ? Integer.MAX_VALUE : minFlingVelocity;
        }

        return config.getScaledMinimumFlingVelocity();
    }

    /**
     * Maximum absolute value of velocity to initiate a fling for a motion generated by an
     * {@link InputDevice} with an id of {@code inputDeviceId}, from an input {@code source} and on
     * a given motion event {@code axis}.
     *
     * @return the maximum velocity, in pixels/second, to trigger fling. If fling is not
     *         supported for the given arguments, returns {@link Integer#MIN_VALUE}.
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

        Resources res = context.getResources();
        int platformResId = getMaximumFlingVelocityResId(res, source, axis);
        if (platformResId != 0) {
            int maxFlingVelocity = res.getDimensionPixelSize(platformResId);
            return maxFlingVelocity < 0 ? Integer.MIN_VALUE : maxFlingVelocity;
        }

        return config.getScaledMaximumFlingVelocity();
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

    private static int getMaximumFlingVelocityResId(Resources res, int source, int axis) {
        if (source == InputDeviceCompat.SOURCE_ROTARY_ENCODER && axis == MotionEvent.AXIS_SCROLL) {
            return getPlatformResId(res, "config_viewMaxRotaryEncoderFlingVelocity", "dimen");
        }
        return 0;
    }

    private static int getPreApi34MinimumFlingVelocityResId(Resources res, int source, int axis) {
        if (source == InputDeviceCompat.SOURCE_ROTARY_ENCODER && axis == MotionEvent.AXIS_SCROLL) {
            return getPlatformResId(res, "config_viewMinRotaryEncoderFlingVelocity", "dimen");
        }
        return 0;
    }

    private static int getPlatformResId(Resources res, String name, String defType) {
        return res.getIdentifier(name, defType, /* defPackage= */ "android");
    }
}
