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

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.IntDef;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
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
    private static final int COLOR_DARK_ON_LIGHT_BACKGROUND_RES_ID =
            R.color.mr_dynamic_dialog_icon_light;

    private MediaRouterThemeHelper() {
    }

    static Drawable getMuteButtonDrawableIcon(Context context) {
        return getIconByDrawableId(context, R.drawable.mr_cast_mute_button);
    }

    static Drawable getCheckBoxDrawableIcon(Context context) {
        return getIconByDrawableId(context, R.drawable.mr_cast_checkbox);
    }

    static Drawable getDefaultDrawableIcon(Context context) {
        return getIconByAttrId(context, R.attr.mediaRouteDefaultIconDrawable);
    }

    static Drawable getTvDrawableIcon(Context context) {
        return getIconByAttrId(context, R.attr.mediaRouteTvIconDrawable);
    }

    static Drawable getSpeakerDrawableIcon(Context context) {
        return getIconByAttrId(context, R.attr.mediaRouteSpeakerIconDrawable);
    }

    static Drawable getSpeakerGroupDrawableIcon(Context context) {
        return getIconByAttrId(context, R.attr.mediaRouteSpeakerGroupIconDrawable);
    }

    private static Drawable getIconByDrawableId(Context context, int drawableId) {
        Drawable icon = AppCompatResources.getDrawable(context, drawableId);
        icon = DrawableCompat.wrap(icon);

        if (isLightTheme(context)) {
            int tintColor = ContextCompat.getColor(context, COLOR_DARK_ON_LIGHT_BACKGROUND_RES_ID);
            DrawableCompat.setTint(icon, tintColor);
        }
        return icon;
    }

    private static Drawable getIconByAttrId(Context context, int attrId) {
        TypedArray styledAttributes = context.obtainStyledAttributes(new int[] { attrId });
        Drawable icon = AppCompatResources.getDrawable(context,
                styledAttributes.getResourceId(0, 0));
        icon = DrawableCompat.wrap(icon);

        // Since Chooser(Controller)Dialog and DevicePicker(Cast)Dialog is using same shape but
        // different color icon for LightTheme, change color of the icon for the latter.
        if (isLightTheme(context)) {
            int tintColor = ContextCompat.getColor(context, COLOR_DARK_ON_LIGHT_BACKGROUND_RES_ID);
            DrawableCompat.setTint(icon, tintColor);
        }
        styledAttributes.recycle();

        return icon;
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

    static TypedArray getStyledAttributes(Context context) {
        return context.obtainStyledAttributes(new int[]{
                R.attr.mediaRouteDefaultIconDrawable,
                R.attr.mediaRouteTvIconDrawable,
                R.attr.mediaRouteSpeakerIconDrawable,
                R.attr.mediaRouteSpeakerGroupIconDrawable});
    }

    static void setDialogBackgroundColor(Context context, Dialog dialog) {
        View dialogView = dialog.getWindow().getDecorView();
        int backgroundColor = ContextCompat.getColor(context, isLightTheme(context)
                ? R.color.mr_dynamic_dialog_background_light
                : R.color.mr_dynamic_dialog_background_dark);
        dialogView.setBackgroundColor(backgroundColor);
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

    /**
     * This method is used by MediaRouteControllerDialog to set color of the volume slider
     * appropriate for the color of controller and backgroundView.
     */
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

    /**
     * This method is used by MediaRouteDynamicControllerDialog to set color of the volume slider
     * according to current theme.
     */
    static void setVolumeSliderColor(Context context, MediaRouteVolumeSlider volumeSlider) {
        int progressAndThumbColor, backgroundColor;
        if (isLightTheme(context)) {
            progressAndThumbColor = ContextCompat.getColor(context,
                    R.color.mr_cast_progressbar_progress_and_thumb_light);
            backgroundColor = ContextCompat.getColor(context,
                    R.color.mr_cast_progressbar_background_light);
        } else {
            progressAndThumbColor = ContextCompat.getColor(context,
                    R.color.mr_cast_progressbar_progress_and_thumb_dark);
            backgroundColor = ContextCompat.getColor(context,
                    R.color.mr_cast_progressbar_background_dark);
        }
        volumeSlider.setColor(progressAndThumbColor, backgroundColor);
    }

    static void setIndeterminateProgressBarColor(Context context, ProgressBar progressBar) {
        if (!progressBar.isIndeterminate()) {
            return;
        }
        int progressColor = ContextCompat.getColor(context, isLightTheme(context)
                ? R.color.mr_cast_progressbar_progress_and_thumb_light :
                R.color.mr_cast_progressbar_progress_and_thumb_dark);
        progressBar.getIndeterminateDrawable().setColorFilter(progressColor,
                PorterDuff.Mode.SRC_IN);
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
