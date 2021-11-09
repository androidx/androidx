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
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link ViewConfiguration}.
 */
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
            return config.getScaledHorizontalScrollFactor();
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
            return config.getScaledVerticalScrollFactor();
        } else {
            return getLegacyScrollFactor(config, context);
        }
    }

    private static float getLegacyScrollFactor(ViewConfiguration config, Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 25 && sGetScaledScrollFactorMethod != null) {
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
    public static int getScaledHoverSlop(ViewConfiguration config) {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return config.getScaledHoverSlop();
        }
        return config.getScaledTouchSlop() / 2;
    }

    /**
     * Check if shortcuts should be displayed in menus.
     *
     * @return {@code True} if shortcuts should be displayed in menus.
     */
    public static boolean shouldShowMenuShortcutsWhenKeyboardPresent(ViewConfiguration config,
            @NonNull Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return config.shouldShowMenuShortcutsWhenKeyboardPresent();
        }
        final Resources res = context.getResources();
        final int platformResId = res.getIdentifier(
                "config_showMenuShortcutsWhenKeyboardPresent", "bool", "android");
        return platformResId != 0 && res.getBoolean(platformResId);
    }

    private ViewConfigurationCompat() {}
}
