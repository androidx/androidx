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

package androidx.browser.auth;

import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_NAVIGATION_BAR_COLOR;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_NAVIGATION_BAR_DIVIDER_COLOR;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_TOOLBAR_COLOR;

import android.os.Bundle;

import androidx.annotation.ColorInt;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Contains visual parameters of an Auth Tab that may depend on the color scheme.
 *
 * @see AuthTabIntent.Builder#setColorSchemeParams(int, AuthTabColorSchemeParams)
 */
public final class AuthTabColorSchemeParams {
    /** Toolbar color. */
    @ColorInt
    private final @Nullable Integer mToolbarColor;

    /** Navigation bar color. */
    @ColorInt
    private final @Nullable Integer mNavigationBarColor;

    /** Navigation bar divider color. */
    @ColorInt
    private final @Nullable Integer mNavigationBarDividerColor;

    private AuthTabColorSchemeParams(@ColorInt @Nullable Integer toolbarColor,
            @ColorInt @Nullable Integer navigationBarColor,
            @ColorInt @Nullable Integer navigationBarDividerColor) {
        mToolbarColor = toolbarColor;
        mNavigationBarColor = navigationBarColor;
        mNavigationBarDividerColor = navigationBarDividerColor;
    }

    @SuppressWarnings("AutoBoxing")
    @ColorInt
    public @Nullable Integer getToolbarColor() {
        return mToolbarColor;
    }

    @SuppressWarnings("AutoBoxing")
    @ColorInt
    public @Nullable Integer getNavigationBarColor() {
        return mNavigationBarColor;
    }

    @SuppressWarnings("AutoBoxing")
    @ColorInt
    public @Nullable Integer getNavigationBarDividerColor() {
        return mNavigationBarDividerColor;
    }

    /**
     * Packs the parameters into a {@link Bundle}.
     * For backward compatibility and ease of use, the names of keys and the structure of the Bundle
     * are the same as that of Intent extras in
     * {@link androidx.browser.customtabs.CustomTabsIntent}.
     */
    @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (mToolbarColor != null) {
            bundle.putInt(EXTRA_TOOLBAR_COLOR, mToolbarColor);
        }
        if (mNavigationBarColor != null) {
            bundle.putInt(EXTRA_NAVIGATION_BAR_COLOR, mNavigationBarColor);
        }
        if (mNavigationBarDividerColor != null) {
            bundle.putInt(EXTRA_NAVIGATION_BAR_DIVIDER_COLOR, mNavigationBarDividerColor);
        }
        return bundle;
    }

    /**
     * Unpacks parameters from a {@link Bundle}. Sets all parameters to null if provided bundle is
     * null.
     */
    @SuppressWarnings("deprecation")
    static @NonNull AuthTabColorSchemeParams fromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle(0);
        }
        // Using bundle.get() instead of bundle.getInt() to default to null without calling
        // bundle.containsKey().
        return new AuthTabColorSchemeParams((Integer) bundle.get(EXTRA_TOOLBAR_COLOR),
                (Integer) bundle.get(EXTRA_NAVIGATION_BAR_COLOR),
                (Integer) bundle.get(EXTRA_NAVIGATION_BAR_DIVIDER_COLOR));
    }

    /**
     * Returns a new {@link AuthTabColorSchemeParams} with the null fields replaced with the
     * provided defaults.
     */
    @NonNull AuthTabColorSchemeParams withDefaults(@NonNull AuthTabColorSchemeParams defaults) {
        return new AuthTabColorSchemeParams(
                mToolbarColor == null ? defaults.mToolbarColor : mToolbarColor,
                mNavigationBarColor == null ? defaults.mNavigationBarColor : mNavigationBarColor,
                mNavigationBarDividerColor == null ? defaults.mNavigationBarDividerColor
                        : mNavigationBarDividerColor);
    }

    /**
     * Builder class for {@link AuthTabColorSchemeParams} objects.
     * The browser's default colors will be used for any unset value.
     */
    public static final class Builder {
        @ColorInt
        private @Nullable Integer mToolbarColor;
        @ColorInt
        private @Nullable Integer mNavigationBarColor;
        @ColorInt
        private @Nullable Integer mNavigationBarDividerColor;

        /**
         * Sets the toolbar color.
         *
         * This color is also applied to the status bar. To ensure good contrast between status bar
         * icons and the background, Auth Tab implementations may use
         * {@link android.view.WindowInsetsController#APPEARANCE_LIGHT_STATUS_BARS}.
         *
         * @param color The color integer. The alpha value will be ignored.
         */
        public @NonNull Builder setToolbarColor(@ColorInt int color) {
            mToolbarColor = color | 0xff000000;
            return this;
        }

        /**
         * Sets the navigation bar color.
         *
         * To ensure good contrast between navigation bar icons and the background, Auth Tab
         * implementations may use
         * {@link android.view.WindowInsetsController#APPEARANCE_LIGHT_NAVIGATION_BARS}.
         *
         * @param color The color integer. The alpha value will be ignored.
         */
        public @NonNull Builder setNavigationBarColor(@ColorInt int color) {
            mNavigationBarColor = color | 0xff000000;
            return this;
        }

        /**
         * Sets the navigation bar divider color.
         *
         * @param color The color integer.
         */
        public @NonNull Builder setNavigationBarDividerColor(@ColorInt int color) {
            mNavigationBarDividerColor = color;
            return this;
        }

        /**
         * Combines all the options that have been set and returns a new
         * {@link AuthTabColorSchemeParams} object.
         */
        public @NonNull AuthTabColorSchemeParams build() {
            return new AuthTabColorSchemeParams(mToolbarColor, mNavigationBarColor,
                    mNavigationBarDividerColor);
        }
    }
}
