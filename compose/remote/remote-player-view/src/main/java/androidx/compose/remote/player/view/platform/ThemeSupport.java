/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.ColorTheme;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

/** Implement color theme support */
@RestrictTo(LIBRARY_GROUP)
public class ThemeSupport {
    RemoteComposeView mInner;
    Context mContext;
    HashMap<String, Integer> mColorMap = null;
    HashMap<String, ColorEngine> mColorEngineMap = new HashMap<>();

    { // This is done this way to simplify injecting other color engines
        mColorEngineMap.put("android", new AndroidColorEngine());
    }

    /** Map system colors to document */
    public void mapColors(@NonNull Context context, @NonNull RemoteComposeView view) {
        mContext = context;
        mInner = view;

        ArrayList<ColorTheme> colorTheme = mInner.getThemedColors();

        if (!colorTheme.isEmpty()) {
            for (ColorTheme theme : colorTheme) {
                ColorEngine colorEngine = mColorEngineMap.get(theme.mColorGroupName);
                if (colorEngine == null) {
                    Log.e("THEME", "Color engine not found for " + theme.mColorGroupName);
                    continue;
                }
                colorEngine.getColors(context, theme);

            }
        }
        String[] name = mInner.getNamedColors();

        // make every effort to terminate early
        if (name == null) {
            return;
        }
        boolean found = false;
        String prefix = "color.";
        for (int i = 0; i < name.length; i++) {
            if (name[i].startsWith(prefix)) {
                int id = lookupColor(name[i].substring(prefix.length()));
                if (id != -1) {
                    try {
                        mInner.setColor(name[i], context.getColor(id));
                    } catch (Exception e) {
                        Log.e("THEME", "Color " + name[i] + " not found");
                    }
                }
            }
        }
        for (int i = 0; i < name.length; i++) {
            if (name[i].startsWith("android.")) {
                found = true;
                break;
            }
        }
        if (!found) {
            return;
        }
        if (null == mContext.getApplicationContext()) {
            return;
        }
        for (int i = 0; i < name.length; i++) {
            String s = name[i];
            if (!s.startsWith("android.")) {
                continue;
            }
            String sub = s.substring("android.".length());
            switch (sub) {
                case "actionBarItemBackground":
                    setRColor(s, android.R.attr.actionBarItemBackground);
                    break;
                case "actionModeBackground":
                    setRColor(s, android.R.attr.actionModeBackground);
                    break;
                case "actionModeSplitBackground":
                    setRColor(s, android.R.attr.actionModeSplitBackground);
                    break;
                case "activatedBackgroundIndicator":
                    setRColor(s, android.R.attr.activatedBackgroundIndicator);
                    break;
                case "colorAccent": // Highlight color for interactive elements
                    setRColor(s, android.R.attr.colorAccent);
                    break;
                case "colorActivatedHighlight":
                    setRColor(s, android.R.attr.colorActivatedHighlight);
                    break;
                case "colorBackground": // background color for the app’s window
                    setRColor(s, android.R.attr.colorBackground);
                    break;
                case "colorBackgroundCacheHint":
                    setRColor(s, android.R.attr.colorBackgroundCacheHint);
                    break;
                //  Background color for floating elements
                case "colorBackgroundFloating":
                    setRColor(s, android.R.attr.colorBackgroundFloating);
                    break;
                case "colorButtonNormal": // The default color for buttons
                    setRColor(s, android.R.attr.colorButtonNormal);
                    break;
                // Color for activated (checked) state of controls.
                case "colorControlActivated":
                    setRColor(s, android.R.attr.colorControlActivated);
                    break;
                case "colorControlHighlight": // Color for highlights on controls
                    setRColor(s, android.R.attr.colorControlHighlight);
                    break;
                // Default color for controls in their normal state.
                case "colorControlNormal":
                    setRColor(s, android.R.attr.colorControlNormal);
                    break;
                // Color for edge effects (e.g., overscroll glow)
                case "colorEdgeEffect":
                    setRColor(s, android.R.attr.colorEdgeEffect);
                    break;
                case "colorError":
                    setRColor(s, android.R.attr.colorError);
                    break;
                case "colorFocusedHighlight":
                    setRColor(s, android.R.attr.colorFocusedHighlight);
                    break;
                case "colorForeground": // General foreground color for views.
                    setRColor(s, android.R.attr.colorForeground);
                    break;
                // Foreground color for inverse backgrounds.
                case "colorForegroundInverse":
                    setRColor(s, android.R.attr.colorForegroundInverse);
                    break;
                case "colorLongPressedHighlight":
                    setRColor(s, android.R.attr.colorLongPressedHighlight);
                    break;
                case "colorMultiSelectHighlight":
                    setRColor(s, android.R.attr.colorMultiSelectHighlight);
                    break;
                case "colorPressedHighlight":
                    setRColor(s, android.R.attr.colorPressedHighlight);
                    break;
                case "colorPrimary": // The primary branding color for the app.
                    setRColor(s, android.R.attr.colorPrimary);
                    break;
                case "colorPrimaryDark": // darker variant of the primary color
                    setRColor(s, android.R.attr.colorPrimaryDark);
                    break;
                case "colorSecondary":
                    setRColor(s, android.R.attr.colorSecondary);
                    break;
                case "detailsElementBackground":
                    setRColor(s, android.R.attr.detailsElementBackground);
                    break;
                case "editTextBackground":
                    setRColor(s, android.R.attr.editTextBackground);
                    break;
                case "galleryItemBackground":
                    setRColor(s, android.R.attr.galleryItemBackground);
                    break;
                case "headerBackground":
                    setRColor(s, android.R.attr.headerBackground);
                    break;
                case "itemBackground":
                    setRColor(s, android.R.attr.itemBackground);
                    break;
                case "numbersBackgroundColor":
                    setRColor(s, android.R.attr.numbersBackgroundColor);
                    break;
                case "panelBackground":
                    setRColor(s, android.R.attr.panelBackground);
                    break;
                case "panelColorBackground":
                    setRColor(s, android.R.attr.panelColorBackground);
                    break;
                case "panelFullBackground":
                    setRColor(s, android.R.attr.panelFullBackground);
                    break;
                case "popupBackground":
                    setRColor(s, android.R.attr.popupBackground);
                    break;
                case "queryBackground":
                    setRColor(s, android.R.attr.queryBackground);
                    break;
                case "selectableItemBackground":
                    setRColor(s, android.R.attr.selectableItemBackground);
                    break;
                case "submitBackground":
                    setRColor(s, android.R.attr.submitBackground);
                    break;
                case "textColor":
                    setRColor(s, android.R.attr.textColor);
                    break;
                case "windowBackground":
                    setRColor(s, android.R.attr.windowBackground);
                    break;
                case "windowBackgroundFallback":
                    setRColor(s, android.R.attr.windowBackgroundFallback);
                    break;
                // Primary text color for inverse backgrounds
                case "textColorPrimaryInverse":
                    setRColor(s, android.R.attr.textColorPrimaryInverse);
                    break;
                // Secondary text color for inverse backgrounds
                case "textColorSecondaryInverse":
                    setRColor(s, android.R.attr.textColorSecondaryInverse);
                    break;
                // Tertiary text color for less important text.
                case "textColorTertiary":
                    setRColor(s, android.R.attr.textColorTertiary);
                    break;
                // Tertiary text color for inverse backgrounds
                case "textColorTertiaryInverse":
                    setRColor(s, android.R.attr.textColorTertiaryInverse);
                    break;
                // Text highlight color (e.g., selected text background).
                case "textColorHighlight":
                    setRColor(s, android.R.attr.textColorHighlight);
                    break;
                // Color for hyperlinks.
                case "textColorLink":
                    setRColor(s, android.R.attr.textColorLink);
                    break;
                //  Color for hint text.
                case "textColorHint":
                    setRColor(s, android.R.attr.textColorHint);
                    break;
                // text color for inverse backgrounds..
                case "textColorHintInverse":
                    setRColor(s, android.R.attr.textColorHintInverse);
                    break;
                // Default color for the thumb of switches.
                case "colorSwitchThumbNormal":
                    setRColor(s, android.R.attr.colorControlNormal);
                    break;
                case "textColorPrimary": // 1.1
                    setRColor(s, android.R.attr.textColorPrimary);
                    break;
                case "textColorSecondary": // 1.1
                    setRColor(s, android.R.attr.textColorSecondary);
                    break;
            }
        }
    }

    private void setRColor(String name, int id) {
        if (null != mContext.getApplicationContext()) {
            int color = getColorFromResource(id);
            mInner.setColor(name, color);
        }
    }

    private int getColorFromResource(int id) {
        if (android.os.Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                >= android.os.Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
            TypedValue typedValue = new TypedValue();
            try (TypedArray arr =
                         mContext.getApplicationContext()
                                 .obtainStyledAttributes(typedValue.data, new int[]{id})) {
                int color = arr.getColor(0, -1);
                return color;
            }
        } // REMOVE IN PLATFORM
        return 0; // REMOVE IN PLATFORM
    }


    static class AndroidColors {

        int[] mId = new int[]{
                android.R.color.background_dark,
                android.R.color.background_light,
                android.R.color.black,
                android.R.color.darker_gray,
                android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark,
                android.R.color.holo_blue_light,
                android.R.color.holo_green_dark,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_dark,
                android.R.color.holo_orange_light,
                android.R.color.holo_purple,
                android.R.color.holo_red_dark,
                android.R.color.holo_red_light,
                android.R.color.system_accent1_0,
                android.R.color.system_accent1_10,
                android.R.color.system_accent1_100,
                android.R.color.system_accent1_1000,
                android.R.color.system_accent1_200,
                android.R.color.system_accent1_300,
                android.R.color.system_accent1_400,
                android.R.color.system_accent1_50,
                android.R.color.system_accent1_500,
                android.R.color.system_accent1_600,
                android.R.color.system_accent1_700,
                android.R.color.system_accent1_800,
                android.R.color.system_accent1_900,
                android.R.color.system_accent2_0,
                android.R.color.system_accent2_10,
                android.R.color.system_accent2_100,
                android.R.color.system_accent2_1000,
                android.R.color.system_accent2_200,
                android.R.color.system_accent2_300,
                android.R.color.system_accent2_400,
                android.R.color.system_accent2_50,
                android.R.color.system_accent2_500,
                android.R.color.system_accent2_600,
                android.R.color.system_accent2_700,
                android.R.color.system_accent2_800,
                android.R.color.system_accent2_900,
                android.R.color.system_accent3_0,
                android.R.color.system_accent3_10,
                android.R.color.system_accent3_100,
                android.R.color.system_accent3_1000,
                android.R.color.system_accent3_200,
                android.R.color.system_accent3_300,
                android.R.color.system_accent3_400,
                android.R.color.system_accent3_50,
                android.R.color.system_accent3_500,
                android.R.color.system_accent3_600,
                android.R.color.system_accent3_700,
                android.R.color.system_accent3_800,
                android.R.color.system_accent3_900,
                android.R.color.system_background_dark,
                android.R.color.system_background_light,
                android.R.color.system_control_activated_dark,
                android.R.color.system_control_activated_light,
                android.R.color.system_control_highlight_dark,
                android.R.color.system_control_highlight_light,
                android.R.color.system_control_normal_dark,
                android.R.color.system_control_normal_light,
                android.R.color.system_error_0,
                android.R.color.system_error_10,
                android.R.color.system_error_100,
                android.R.color.system_error_1000,
                android.R.color.system_error_200,
                android.R.color.system_error_300,
                android.R.color.system_error_400,
                android.R.color.system_error_50,
                android.R.color.system_error_500,
                android.R.color.system_error_600,
                android.R.color.system_error_700,
                android.R.color.system_error_800,
                android.R.color.system_error_900,
                android.R.color.system_error_container_dark,
                android.R.color.system_error_container_light,
                android.R.color.system_error_dark,
                android.R.color.system_error_light,
                android.R.color.system_neutral1_0,
                android.R.color.system_neutral1_10,
                android.R.color.system_neutral1_100,
                android.R.color.system_neutral1_1000,
                android.R.color.system_neutral1_200,
                android.R.color.system_neutral1_300,
                android.R.color.system_neutral1_400,
                android.R.color.system_neutral1_50,
                android.R.color.system_neutral1_500,
                android.R.color.system_neutral1_600,
                android.R.color.system_neutral1_700,
                android.R.color.system_neutral1_800,
                android.R.color.system_neutral1_900,
                android.R.color.system_neutral2_0,
                android.R.color.system_neutral2_10,
                android.R.color.system_neutral2_100,
                android.R.color.system_neutral2_1000,
                android.R.color.system_neutral2_200,
                android.R.color.system_neutral2_300,
                android.R.color.system_neutral2_400,
                android.R.color.system_neutral2_50,
                android.R.color.system_neutral2_500,
                android.R.color.system_neutral2_600,
                android.R.color.system_neutral2_700,
                android.R.color.system_neutral2_800,
                android.R.color.system_neutral2_900,
                android.R.color.system_on_background_dark,
                android.R.color.system_on_background_light,
                android.R.color.system_on_error_container_dark,
                android.R.color.system_on_error_container_light,
                android.R.color.system_on_error_dark,
                android.R.color.system_on_error_light,
                android.R.color.system_on_primary_container_dark,
                android.R.color.system_on_primary_container_light,
                android.R.color.system_on_primary_dark,
                android.R.color.system_on_primary_fixed,
                android.R.color.system_on_primary_fixed_variant,
                android.R.color.system_on_primary_light,
                android.R.color.system_on_secondary_container_dark,
                android.R.color.system_on_secondary_container_light,
                android.R.color.system_on_secondary_dark,
                android.R.color.system_on_secondary_fixed,
                android.R.color.system_on_secondary_fixed_variant,
                android.R.color.system_on_secondary_light,
                android.R.color.system_on_surface_dark,
                android.R.color.system_on_surface_disabled,
                android.R.color.system_on_surface_light,
                android.R.color.system_on_surface_variant_dark,
                android.R.color.system_on_surface_variant_light,
                android.R.color.system_on_tertiary_container_dark,
                android.R.color.system_on_tertiary_container_light,
                android.R.color.system_on_tertiary_dark,
                android.R.color.system_on_tertiary_fixed,
                android.R.color.system_on_tertiary_fixed_variant,
                android.R.color.system_on_tertiary_light,
                android.R.color.system_outline_dark,
                android.R.color.system_outline_disabled,
                android.R.color.system_outline_light,
                android.R.color.system_outline_variant_dark,
                android.R.color.system_outline_variant_light,
                android.R.color.system_palette_key_color_neutral_dark,
                android.R.color.system_palette_key_color_neutral_light,
                android.R.color.system_palette_key_color_neutral_variant_dark,
                android.R.color.system_palette_key_color_neutral_variant_light,
                android.R.color.system_palette_key_color_primary_dark,
                android.R.color.system_palette_key_color_primary_light,
                android.R.color.system_palette_key_color_secondary_dark,
                android.R.color.system_palette_key_color_secondary_light,
                android.R.color.system_palette_key_color_tertiary_dark,
                android.R.color.system_palette_key_color_tertiary_light,
                android.R.color.system_primary_container_dark,
                android.R.color.system_primary_container_light,
                android.R.color.system_primary_dark,
                android.R.color.system_primary_fixed,
                android.R.color.system_primary_fixed_dim,
                android.R.color.system_primary_light,
                android.R.color.system_secondary_container_dark,
                android.R.color.system_secondary_container_light,
                android.R.color.system_secondary_dark,
                android.R.color.system_secondary_fixed,
                android.R.color.system_secondary_fixed_dim,
                android.R.color.system_secondary_light,
                android.R.color.system_surface_bright_dark,
                android.R.color.system_surface_bright_light,
                android.R.color.system_surface_container_dark,
                android.R.color.system_surface_container_high_dark,
                android.R.color.system_surface_container_high_light,
                android.R.color.system_surface_container_highest_dark,
                android.R.color.system_surface_container_highest_light,
                android.R.color.system_surface_container_light,
                android.R.color.system_surface_container_low_dark,
                android.R.color.system_surface_container_low_light,
                android.R.color.system_surface_container_lowest_dark,
                android.R.color.system_surface_container_lowest_light,
                android.R.color.system_surface_dark,
                android.R.color.system_surface_dim_dark,
                android.R.color.system_surface_dim_light,
                android.R.color.system_surface_disabled,
                android.R.color.system_surface_light,
                android.R.color.system_surface_variant_dark,
                android.R.color.system_surface_variant_light,
                android.R.color.system_tertiary_container_dark,
                android.R.color.system_tertiary_container_light,
                android.R.color.system_tertiary_dark,
                android.R.color.system_tertiary_fixed,
                android.R.color.system_tertiary_fixed_dim,
                android.R.color.system_tertiary_light,
                android.R.color.system_text_hint_inverse_dark,
                android.R.color.system_text_hint_inverse_light,
                android.R.color.system_text_primary_inverse_dark,
                android.R.color.system_text_primary_inverse_disable_only_dark,
                android.R.color.system_text_primary_inverse_disable_only_light,
                android.R.color.system_text_primary_inverse_light,
                android.R.color.system_text_secondary_and_tertiary_inverse_dark,
                android.R.color.system_text_secondary_and_tertiary_inverse_disabled_dark,
                android.R.color.system_text_secondary_and_tertiary_inverse_disabled_light,
                android.R.color.system_text_secondary_and_tertiary_inverse_light,
                android.R.color.tab_indicator_text
        };


        String[] mName = {
                "background_dark",
                "background_light",
                "black",
                "darker_gray",
                "holo_blue_bright",
                "holo_blue_dark",
                "holo_blue_light",
                "holo_green_dark",
                "holo_green_light",
                "holo_orange_dark",
                "holo_orange_light",
                "holo_purple",
                "holo_red_dark",
                "holo_red_light",
                "system_accent1_0",
                "system_accent1_10",
                "system_accent1_100",
                "system_accent1_1000",
                "system_accent1_200",
                "system_accent1_300",
                "system_accent1_400",
                "system_accent1_50",
                "system_accent1_500",
                "system_accent1_600",
                "system_accent1_700",
                "system_accent1_800",
                "system_accent1_900",
                "system_accent2_0",
                "system_accent2_10",
                "system_accent2_100",
                "system_accent2_1000",
                "system_accent2_200",
                "system_accent2_300",
                "system_accent2_400",
                "system_accent2_50",
                "system_accent2_500",
                "system_accent2_600",
                "system_accent2_700",
                "system_accent2_800",
                "system_accent2_900",
                "system_accent3_0",
                "system_accent3_10",
                "system_accent3_100",
                "system_accent3_1000",
                "system_accent3_200",
                "system_accent3_300",
                "system_accent3_400",
                "system_accent3_50",
                "system_accent3_500",
                "system_accent3_600",
                "system_accent3_700",
                "system_accent3_800",
                "system_accent3_900",
                "system_background_dark",
                "system_background_light",
                "system_control_activated_dark",
                "system_control_activated_light",
                "system_control_highlight_dark",
                "system_control_highlight_light",
                "system_control_normal_dark",
                "system_control_normal_light",
                "system_error_0",
                "system_error_10",
                "system_error_100",
                "system_error_1000",
                "system_error_200",
                "system_error_300",
                "system_error_400",
                "system_error_50",
                "system_error_500",
                "system_error_600",
                "system_error_700",
                "system_error_800",
                "system_error_900",
                "system_error_container_dark",
                "system_error_container_light",
                "system_error_dark",
                "system_error_light",
                "system_neutral1_0",
                "system_neutral1_10",
                "system_neutral1_100",
                "system_neutral1_1000",
                "system_neutral1_200",
                "system_neutral1_300",
                "system_neutral1_400",
                "system_neutral1_50",
                "system_neutral1_500",
                "system_neutral1_600",
                "system_neutral1_700",
                "system_neutral1_800",
                "system_neutral1_900",
                "system_neutral2_0",
                "system_neutral2_10",
                "system_neutral2_100",
                "system_neutral2_1000",
                "system_neutral2_200",
                "system_neutral2_300",
                "system_neutral2_400",
                "system_neutral2_50",
                "system_neutral2_500",
                "system_neutral2_600",
                "system_neutral2_700",
                "system_neutral2_800",
                "system_neutral2_900",
                "system_on_background_dark",
                "system_on_background_light",
                "system_on_error_container_dark",
                "system_on_error_container_light",
                "system_on_error_dark",
                "system_on_error_light",
                "system_on_primary_container_dark",
                "system_on_primary_container_light",
                "system_on_primary_dark",
                "system_on_primary_fixed",
                "system_on_primary_fixed_variant",
                "system_on_primary_light",
                "system_on_secondary_container_dark",
                "system_on_secondary_container_light",
                "system_on_secondary_dark",
                "system_on_secondary_fixed",
                "system_on_secondary_fixed_variant",
                "system_on_secondary_light",
                "system_on_surface_dark",
                "system_on_surface_disabled",
                "system_on_surface_light",
                "system_on_surface_variant_dark",
                "system_on_surface_variant_light",
                "system_on_tertiary_container_dark",
                "system_on_tertiary_container_light",
                "system_on_tertiary_dark",
                "system_on_tertiary_fixed",
                "system_on_tertiary_fixed_variant",
                "system_on_tertiary_light",
                "system_outline_dark",
                "system_outline_disabled",
                "system_outline_light",
                "system_outline_variant_dark",
                "system_outline_variant_light",
                "system_palette_key_color_neutral_dark",
                "system_palette_key_color_neutral_light",
                "system_palette_key_color_neutral_variant_dark",
                "system_palette_key_color_neutral_variant_light",
                "system_palette_key_color_primary_dark",
                "system_palette_key_color_primary_light",
                "system_palette_key_color_secondary_dark",
                "system_palette_key_color_secondary_light",
                "system_palette_key_color_tertiary_dark",
                "system_palette_key_color_tertiary_light",
                "system_primary_container_dark",
                "system_primary_container_light",
                "system_primary_dark",
                "system_primary_fixed",
                "system_primary_fixed_dim",
                "system_primary_light",
                "system_secondary_container_dark",
                "system_secondary_container_light",
                "system_secondary_dark",
                "system_secondary_fixed",
                "system_secondary_fixed_dim",
                "system_secondary_light",
                "system_surface_bright_dark",
                "system_surface_bright_light",
                "system_surface_container_dark",
                "system_surface_container_high_dark",
                "system_surface_container_high_light",
                "system_surface_container_highest_dark",
                "system_surface_container_highest_light",
                "system_surface_container_light",
                "system_surface_container_low_dark",
                "system_surface_container_low_light",
                "system_surface_container_lowest_dark",
                "system_surface_container_lowest_light",
                "system_surface_dark",
                "system_surface_dim_dark",
                "system_surface_dim_light",
                "system_surface_disabled",
                "system_surface_light",
                "system_surface_variant_dark",
                "system_surface_variant_light",
                "system_tertiary_container_dark",
                "system_tertiary_container_light",
                "system_tertiary_dark",
                "system_tertiary_fixed",
                "system_tertiary_fixed_dim",
                "system_tertiary_light",
                "system_text_hint_inverse_dark",
                "system_text_hint_inverse_light",
                "system_text_primary_inverse_dark",
                "system_text_primary_inverse_disable_only_dark",
                "system_text_primary_inverse_disable_only_light",
                "system_text_primary_inverse_light",
                "system_text_secondary_and_tertiary_inverse_dark",
                "system_text_secondary_and_tertiary_inverse_disabled_dark",
                "system_text_secondary_and_tertiary_inverse_disabled_light",
                "system_text_secondary_and_tertiary_inverse_light",
                "tab_indicator_text"
        };
    }

    AndroidColors mAndroid = new AndroidColors();

    private int lookupColor(String name) {
        if (mColorMap == null) {
            mColorMap = new HashMap<>();
            for (int i = 0; i < mAndroid.mName.length; i++) {
                mColorMap.put(mAndroid.mName[i], mAndroid.mId[i]);
            }
        }
        Integer ret = mColorMap.get(name);
        if (ret != null) {
            return ret;
        }
        return -1;
    }

    interface ColorEngine {
        void getColors(Context context, ColorTheme theme);
    }

    static class AndroidColorEngine implements ColorEngine {
        AndroidColors mColors = new AndroidColors();

        @Override
        public void getColors(Context context, ColorTheme theme) {
            if (theme.mDarkModeIndex >= 0 && theme.mDarkModeIndex < mColors.mId.length) {
                try {
                    theme.mDarkMode = context.getColor(mColors.mId[theme.mDarkModeIndex]);
                } catch (Exception ex) {
                    Log.e("THEME", "Color " + mColors.mName[theme.mDarkModeIndex] + " not found");
                }
            } else {
                Log.e("THEME", "mDarkModeId " + theme.mDarkModeIndex + " not found "
                        + theme.mColorGroupName);

            }
            if (theme.mLightModeIndex >= 0 && theme.mLightModeIndex < mColors.mId.length) {
                try {
                    theme.mLightMode = context.getColor(mColors.mId[theme.mLightModeIndex]);
                } catch (Exception ex) {
                    Log.e("THEME", "Color " + mColors.mName[theme.mLightModeIndex] + " not found");
                }
            } else {

                Log.e("THEME", "mLightMode " + theme.mLightModeIndex + " not found "
                        + theme.mColorGroupName);

            }
        }
    }
}
