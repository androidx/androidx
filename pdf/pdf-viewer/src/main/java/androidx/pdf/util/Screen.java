/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

/**
 * Wrapper around {@link Context} to give shortcut access to commonly used values.
 * TODO: Rename this class 'Res'.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class Screen {

    private static final String TAG = Screen.class.getSimpleName();

    private final Context mCtx;
    private final Resources mRes;

    private boolean mHasSoftBottomNavInsets;

    public Screen(@NonNull Context ctx) {
        this.mCtx = ctx.getApplicationContext();
        mRes = this.mCtx.getResources();
    }

    /** Converts a value given in dp to pixels, based on the screen density. */
    public int pxFromDp(int dp) {
        float density = mRes.getDisplayMetrics().density;
        return (int) (dp * density);
    }

    /** Converts a value given in pixels to density independent pixels. */
    public int dpFromPx(int px) {
        float density = mRes.getDisplayMetrics().density;
        return (int) (px / density);
    }

    public int getDensityDpi() {
        return mRes.getDisplayMetrics().densityDpi;
    }

    public float getDensity() {
        return mRes.getDisplayMetrics().density;
    }

    public int getWidthPx() {
        return mRes.getDisplayMetrics().widthPixels;
    }

    public int getHeightPx() {
        return mRes.getDisplayMetrics().heightPixels;
    }

    /** Shortcut method for getting a dimension in pixels from its resource id. */
    public int getDimensionPx(int id) {
        return mRes.getDimensionPixelOffset(id);
    }

    /** Shortcut method for getting a color int from its resource id. */
    public int getColor(int id) {
        return mRes.getColor(id);
    }

    /** Shortcut method for getting a string from its resource id. */
    @NonNull
    public String getString(int id, @NonNull Object... formatArgs) {
        return mRes.getString(id, formatArgs);
    }

    /**
     * Detects whether this device has a soft nav bar. That is, some phones have physical buttons
     * for
     * the navigation bar.
     */
    private boolean hasSoftNavBar() {
        // This is not 100% reliable, as the OneTouch phone reports true.
        return getAndroidBoolean("config_showNavigationBar");
    }

    /** Detects if there is a soft nav bar currently at the bottom of the screen. */
    public boolean hasSoftNavBarAtBottom() {
        // Just because this boolean is false, doesn't mean that there isn't a soft bottom navbar.
        if (mHasSoftBottomNavInsets) {
            return true;
        }

        if (!hasSoftNavBar()) {
            // Hardware nav bar. This is not 100% reliable, as the OneTouch phone reports false
            // above.
            return false;
        }

        if (mRes.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            // In portrait (or square), non-gesture nav, nav bar is at bottom
            return true;
        }

        // Note: this comes from PhoneWindowManager#setInitialDisplaySize.
        boolean navBarCanMove = mRes.getConfiguration().smallestScreenWidthDp < 600;
        return !navBarCanMove;
    }

    /** Notifies this class of new WindowInsets which can be used to detect navbar. */
    public void reportWindowInsets(@NonNull WindowInsetsCompat windowInsets) {
        Insets gestureInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
        Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
        mHasSoftBottomNavInsets = gestureInsets.bottom > 0 || navInsets.bottom > 0;
    }

    private boolean getAndroidBoolean(String name) {
        int resId = mRes.getIdentifier(name, "bool", "android");
        return resId > 0 && mRes.getBoolean(resId);
    }
}
