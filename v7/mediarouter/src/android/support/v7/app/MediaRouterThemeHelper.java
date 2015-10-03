/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.mediarouter.R;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

final class MediaRouterThemeHelper {

    // http://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html
    private static final float MIN_CONTRAST = 4.5f;

    private static final float LIGHT_LUMINANCE_THRESHOLD = 0.5f;

    private static final float COLOR_LIGHTNESS_MULTIPLIER = 1.15f;

    private MediaRouterThemeHelper() {
    }

    public static Context createThemedContext(Context context) {
        int style;
        if (isLightTheme(context)) {
            if (isPrimaryColorLight(context)) {
                style = R.style.Theme_MediaRouter_Light;
            } else {
                style = R.style.Theme_MediaRouter_Light_DarkControlPanel;
            }
        } else {
            if (isPrimaryColorLight(context)) {
                style = R.style.Theme_MediaRouter_LightControlPanel;
            } else {
                style = R.style.Theme_MediaRouter;
            }
        }
        return new ContextThemeWrapper(context, style);
    }

    public static int getThemeResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attr, value, true) ? value.resourceId : 0;
    }

    public static Drawable getThemeDrawable(Context context, int attr) {
        int res = getThemeResource(context, attr);
        return res != 0 ? context.getResources().getDrawable(res) : null;
    }

    public static int getControllerColor(Context context) {
        return isPrimaryColorLight(context) ? Color.BLACK : Color.WHITE;
    }

    public static int getButtonTextColor(Context context) {
        int primaryColor = getThemeColor(context, R.attr.colorPrimary);
        int backgroundColor = getThemeColor(context, android.R.attr.colorBackground);

        double contrast = ColorUtils.calculateContrast(primaryColor, backgroundColor);
        if (contrast < MIN_CONTRAST) {
            // Default to colorAccent if the contrast ratio is low.
            return getThemeColor(context, R.attr.colorAccent);
        }
        return primaryColor;
    }

    public static int getVolumeGroupListBackgroundColor(Context context) {
        int primaryColor = getThemeColor(context, R.attr.colorPrimary);
        int primaryDarkColor = getThemeColor(context, R.attr.colorPrimaryDark);

        if (ColorUtils.calculateLuminance(primaryColor) >= LIGHT_LUMINANCE_THRESHOLD) {
            // This means the volume sliders are colored black.
            double contrast = ColorUtils.calculateContrast(Color.BLACK, primaryDarkColor);
            if (contrast < MIN_CONTRAST) {
                // Use a lighter color for better contrast.
                return adjustColorLightness(primaryColor, COLOR_LIGHTNESS_MULTIPLIER);
            }
        }
        return primaryDarkColor;
    }

    private static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.isLightTheme, value, true)
                && value.data != 0;
    }

    private static boolean isPrimaryColorLight(Context context) {
        int primaryColor = getThemeColor(context, R.attr.colorPrimary);
        return ColorUtils.calculateLuminance(primaryColor) >= LIGHT_LUMINANCE_THRESHOLD;
    }

    private static int adjustColorLightness(int color, float lightness) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        // Clip the lightness to 100%
        hsl[2] = Math.min(1f, hsl[2] * lightness);
        return ColorUtils.HSLToColor(hsl);
    }

    private static int getThemeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return context.getResources().getColor(value.resourceId);
        }
        return value.data;
    }
}
