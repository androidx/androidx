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

package androidx.mediarouter.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.core.graphics.ColorUtils;
import androidx.mediarouter.R;

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

    static Context createThemedButtonContext(Context context) {
        // Apply base Media Router theme.
        context = new ContextThemeWrapper(context, getRouterThemeId(context));

        // Apply custom Media Router theme.
        int style = getThemeResource(context, R.attr.mediaRouteTheme);
        if (style != 0) {
            context = new ContextThemeWrapper(context, style);
        }

        return context;
    }

    /*
     * The following two methods are to be used in conjunction. They should be used to prepare
     * the context and theme for a super class constructor (the latter method relies on the
     * former method to properly prepare the context):
     *   super(context = createThemedDialogContext(context, theme),
     *           createThemedDialogStyle(context));
     *
     * It will apply theme in the following order (style lookups will be done in reverse):
     *   1) Current theme
     *   2) Supplied theme
     *   3) Base Media Router theme
     *   4) Custom Media Router theme, if provided
     */
    static Context createThemedDialogContext(Context context, int theme, boolean alertDialog) {
        // 1) Current theme is already applied to the context

        // 2) If no theme is supplied, look it up from the context (dialogTheme/alertDialogTheme)
        if (theme == 0) {
            theme = getThemeResource(context, !alertDialog
                    ? androidx.appcompat.R.attr.dialogTheme
                    : androidx.appcompat.R.attr.alertDialogTheme);
        }
        //    Apply it
        context = new ContextThemeWrapper(context, theme);

        // 3) If a custom Media Router theme is provided then apply the base theme
        if (getThemeResource(context, R.attr.mediaRouteTheme) != 0) {
            context = new ContextThemeWrapper(context, getRouterThemeId(context));
        }

        return context;
    }
    // This method should be used in conjunction with the previous method.
    static int createThemedDialogStyle(Context context) {
        // 4) Apply the custom Media Router theme
        int theme = getThemeResource(context, R.attr.mediaRouteTheme);
        if (theme == 0) {
            // 3) No custom MediaRouther theme was provided so apply the base theme instead
            theme = getRouterThemeId(context);
        }

        return theme;
    }
    // END. Previous two methods should be used in conjunction.

    static int getThemeResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attr, value, true) ? value.resourceId : 0;
    }

    static float getDisabledAlpha(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, value, true)
                ? value.getFloat() : 0.5f;
    }

    static @ControllerColorType int getControllerColor(Context context, int style) {
        int primaryColor = getThemeColor(context, style,
                androidx.appcompat.R.attr.colorPrimary);
        if (ColorUtils.calculateContrast(COLOR_WHITE_ON_DARK_BACKGROUND, primaryColor)
                >= MIN_CONTRAST) {
            return COLOR_WHITE_ON_DARK_BACKGROUND;
        }
        return COLOR_DARK_ON_LIGHT_BACKGROUND;
    }

    static int getButtonTextColor(Context context) {
        int primaryColor = getThemeColor(context, 0,
                androidx.appcompat.R.attr.colorPrimary);
        int backgroundColor = getThemeColor(context, 0, android.R.attr.colorBackground);

        if (ColorUtils.calculateContrast(primaryColor, backgroundColor) < MIN_CONTRAST) {
            // Default to colorAccent if the contrast ratio is low.
            return getThemeColor(context, 0, androidx.appcompat.R.attr.colorAccent);
        }
        return primaryColor;
    }

    static void setMediaControlsBackgroundColor(
            Context context, View mainControls, View groupControls, boolean hasGroup) {
        int primaryColor = getThemeColor(context, 0,
                androidx.appcompat.R.attr.colorPrimary);
        int primaryDarkColor = getThemeColor(context, 0,
                androidx.appcompat.R.attr.colorPrimaryDark);
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

    static void setVolumeSliderColor(
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

    private static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(androidx.appcompat.R.attr.isLightTheme,
                value, true) && value.data != 0;
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

    private static int getRouterThemeId(Context context) {
        int themeId;
        if (isLightTheme(context)) {
            if (getControllerColor(context, 0) == COLOR_DARK_ON_LIGHT_BACKGROUND) {
                themeId = R.style.Theme_MediaRouter_Light;
            } else {
                themeId = R.style.Theme_MediaRouter_Light_DarkControlPanel;
            }
        } else {
            if (getControllerColor(context, 0) == COLOR_DARK_ON_LIGHT_BACKGROUND) {
                themeId = R.style.Theme_MediaRouter_LightControlPanel;
            } else {
                themeId = R.style.Theme_MediaRouter;
            }
        }
        return themeId;
    }
}
