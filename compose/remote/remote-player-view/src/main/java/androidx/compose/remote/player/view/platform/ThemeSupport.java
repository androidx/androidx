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

import androidx.annotation.RestrictTo;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

@RestrictTo(LIBRARY_GROUP)
/** Implement color theme support */
public class ThemeSupport {

    RemoteComposeView mInner;
    Context mContext;

    /** Map system colors to document */
    public void mapColors(Context context, RemoteComposeView view) {
        mContext = context;
        mInner = view;

        String[] name = mInner.getNamedColors();

        // make every effort to terminate early
        if (name == null) {
            return;
        }
        boolean found = false;
        for (int i = 0; i < name.length; i++) {
            if (name[i].startsWith("android.")) {
                found = true;
                break;
            }
        }
        if (!found) {
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
        int color = getColorFromResource(id);
        mInner.setColor(name, color);
    }

    private int getColorFromResource(int id) {
        if (android.os.Build.VERSION.SDK_INT // REMOVE IN PLATFORM
                >= android.os.Build.VERSION_CODES.S) { // REMOVE IN PLATFORM
            TypedValue typedValue = new TypedValue();
            try (TypedArray arr =
                    mContext.getApplicationContext()
                            .obtainStyledAttributes(typedValue.data, new int[] {id})) {
                int color = arr.getColor(0, -1);
                return color;
            }
        } // REMOVE IN PLATFORM
        return 0; // REMOVE IN PLATFORM
    }
}
