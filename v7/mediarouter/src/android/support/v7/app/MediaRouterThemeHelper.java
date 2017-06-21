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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.mediarouter.R;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class MediaRouterThemeHelper {
    private static final float MIN_CONTRAST = 3.0f;

    @IntDef({COLOR_DARK_ON_LIGHT_BACKGROUND, COLOR_WHITE_ON_DARK_BACKGROUND})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ControllerColorType {}

    static final int COLOR_DARK_ON_LIGHT_BACKGROUND = 0xDE000000; /* Opacity of 87% */
    static final int COLOR_WHITE_ON_DARK_BACKGROUND = Color.WHITE;

    private MediaRouterThemeHelper() {
    }

    /**
     * Creates a themed context based on the explicit style resource or the parent context's default
     * theme.
     * <p>
     * The theme which will be applied on top of the parent {@code context}'s theme is determined
     * by the primary color defined in the given {@code style}, or in the parent {@code context}.
     *
     * @param context the parent context
     * @param style the resource ID of the style against which to inflate this context, or
     *              {@code 0} to use the parent {@code context}'s default theme.
     * @return The themed context.
     */
    static Context createThemedContext(Context context, int style) {
        // First, apply dialog property overlay.
        Context themedContext =
                new ContextThemeWrapper(context, getStyledRouterThemeId(context, style));
        int customizedThemeId = getThemeResource(context, R.attr.mediaRouteTheme);
        return customizedThemeId == 0 ? themedContext
                : new ContextThemeWrapper(themedContext, customizedThemeId);
    }

    /**
     * Creates the theme resource ID intended to be used by dialogs.
     */
    static int createThemeForDialog(Context context, int style) {
        int customizedThemeId = getThemeResource(context, R.attr.mediaRouteTheme);
        return customizedThemeId != 0 ? customizedThemeId : getStyledRouterThemeId(context, style);
    }

    public static int getThemeResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attr, value, true) ? value.resourceId : 0;
    }

    public static float getDisabledAlpha(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, value, true)
                ? value.getFloat() : 0.5f;
    }

    public static @ControllerColorType int getControllerColor(Context context, int style) {
        int primaryColor = getThemeColor(context, style,
                android.support.v7.appcompat.R.attr.colorPrimary);
        if (ColorUtils.calculateContrast(COLOR_WHITE_ON_DARK_BACKGROUND, primaryColor)
                >= MIN_CONTRAST) {
            return COLOR_WHITE_ON_DARK_BACKGROUND;
        }
        return COLOR_DARK_ON_LIGHT_BACKGROUND;
    }

    public static int getButtonTextColor(Context context) {
        int primaryColor = getThemeColor(context, 0,
                android.support.v7.appcompat.R.attr.colorPrimary);
        int backgroundColor = getThemeColor(context, 0, android.R.attr.colorBackground);

        if (ColorUtils.calculateContrast(primaryColor, backgroundColor) < MIN_CONTRAST) {
            // Default to colorAccent if the contrast ratio is low.
            return getThemeColor(context, 0, android.support.v7.appcompat.R.attr.colorAccent);
        }
        return primaryColor;
    }

    public static void setMediaControlsBackgroundColor(
            Context context, View mainControls, View groupControls, boolean hasGroup) {
        int primaryColor = getThemeColor(context, 0,
                android.support.v7.appcompat.R.attr.colorPrimary);
        int primaryDarkColor = getThemeColor(context, 0,
                android.support.v7.appcompat.R.attr.colorPrimaryDark);
        if (hasGroup && getControllerColor(context, 0) == COLOR_DARK_ON_LIGHT_BACKGROUND) {
            // Instead of showing dark controls in a possibly dark (i.e. the primary dark), model
            // the white dialog and use the primary color for the group controls.
            primaryDarkColor = primaryColor;
            primaryColor = Color.WHITE;
        }
        mainControls.setBackgroundColor(primaryColor);
        groupControls.setBackgroundColor(primaryDarkColor);
        // Also store the background colors to the view tags. They are used in
        // setVolumeSliderColor() below.
        mainControls.setTag(primaryColor);
        groupControls.setTag(primaryDarkColor);
    }

    public static void setVolumeSliderColor(
            Context context, MediaRouteVolumeSlider volumeSlider, View backgroundView) {
        int controllerColor = getControllerColor(context, 0);
        if (Color.alpha(controllerColor) != 0xFF) {
            // Composite with the background in order not to show the underlying progress bar
            // through the thumb.
            int backgroundColor = (int) backgroundView.getTag();
            controllerColor = ColorUtils.compositeColors(controllerColor, backgroundColor);
        }
        volumeSlider.setColor(controllerColor);
    }

    // This is copied from {@link AlertDialog#resolveDialogTheme} to pre-evaluate theme in advance.
    public static int getAlertDialogResolvedTheme(Context context, int themeResId) {
        if (themeResId >= 0x01000000) {   // start of real resource IDs.
            return themeResId;
        } else {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(
                    android.support.v7.appcompat.R.attr.alertDialogTheme, outValue, true);
            return outValue.resourceId;
        }
    }

    private static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(
                android.support.v7.appcompat.R.attr.isLightTheme, value, true)
                && value.data != 0;
    }

    private static int getThemeColor(Context context, int style, int attr) {
        if (style != 0) {
            int[] attrs = { attr };
            TypedArray ta = context.obtainStyledAttributes(style, attrs);
            int color = ta.getColor(0, 0);
            ta.recycle();
            if (color != 0) {
                return color;
            }
        }
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return context.getResources().getColor(value.resourceId);
        }
        return value.data;
    }

    private static int getStyledRouterThemeId(Context context, int style) {
        int themeId;
        if (isLightTheme(context)) {
            if (getControllerColor(context, style) == COLOR_DARK_ON_LIGHT_BACKGROUND) {
                themeId = R.style.Theme_MediaRouter_Light;
            } else {
                themeId = R.style.Theme_MediaRouter_Light_DarkControlPanel;
            }
        } else {
            if (getControllerColor(context, style) == COLOR_DARK_ON_LIGHT_BACKGROUND) {
                themeId = R.style.Theme_MediaRouter_LightControlPanel;
            } else {
                themeId = R.style.Theme_MediaRouter;
            }
        }
        return themeId;
    }
}
