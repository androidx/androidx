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

package androidx.browser.trusted.splashscreens;

import android.os.Bundle;

import androidx.browser.trusted.TrustedWebActivityBuilder;

/**
 * The keys of the entries in the {@link Bundle} passed to
 * {@link TrustedWebActivityBuilder#setSplashScreenParams}. This Bundle can also be assembled
 * manually and added to the launch Intent as an extra with the key
 * {@link TrustedWebActivityBuilder#EXTRA_SPLASH_SCREEN_PARAMS}.
 */
public final class SplashScreenParamKey {
    /**
     * The version of splash screens to use.
     * The value must be one of {@link SplashScreenVersion}.
     */
    public static final String VERSION = "androidx.browser.trusted.KEY_SPLASH_SCREEN_VERSION";

    /**
     * The background color of the splash screen.
     * The value must be an integer representing the color in RGB (alpha channel is ignored if
     * provided). The default is white.
     */
    public static final String BACKGROUND_COLOR =
            "androidx.browser.trusted.trusted.KEY_SPLASH_SCREEN_BACKGROUND_COLOR";

    /**
     * The {@link android.widget.ImageView.ScaleType} to apply to the image on the splash
     * screen.
     * The value must be an integer - the ordinal of the ScaleType.
     * The default is {@link android.widget.ImageView.ScaleType#CENTER}.
     */
    public static final String SCALE_TYPE = "androidx.browser.trusted.KEY_SPLASH_SCREEN_SCALE_TYPE";

    /**
     * The transformation matrix to apply to the image on the splash screen. See
     * {@link android.widget.ImageView#setImageMatrix}. Only needs to be provided if the scale
     * type is {@link android.widget.ImageView.ScaleType#MATRIX}.
     * The value must be an array of 9 floats or null. This array can be retrieved from
     * {@link android.graphics.Matrix#getValues(float[])}. The default is null.
     */
    public static final String IMAGE_TRANSFORMATION_MATRIX =
            "androidx.browser.trusted.KEY_SPLASH_SCREEN_TRANSFORMATION_MATRIX";

    /**
     * The duration of fade out animation in milliseconds to be played when removing splash
     * screen.
     * The value must be provided as an int. The default is 0 (no animation).
     */
    public static final String FADE_OUT_DURATION_MS =
            "androidx.browser.trusted.KEY_SPLASH_SCREEN_FADE_OUT_DURATION";

    private SplashScreenParamKey() {}
}
