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
import android.support.v7.mediarouter.R;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

final class MediaRouterThemeHelper {
    private MediaRouterThemeHelper() {
    }

    public static Context createThemedContext(Context context) {
        int style;
        if (isLightTheme(context)) {
            if (isPrimaryColorDark(context)) {
                style = R.style.Theme_MediaRouter_Light_DarkControlPanel;
            } else {
                style = R.style.Theme_MediaRouter_Light;
            }
        } else {
            if (isPrimaryColorDark(context)) {
                style = R.style.Theme_MediaRouter;
            } else {
                style = R.style.Theme_MediaRouter_LightControlPanel;
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

    private static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.isLightTheme, value, true)
                && value.data != 0;
    }

    private static boolean isPrimaryColorDark(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
        return luminance(value.data) < 0.5;
    }

    private static float luminance(int color) {
        double red = Color.red(color) / 255.0;
        red = red < 0.03928 ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);
        double green = Color.green(color) / 255.0;
        green = green < 0.03928 ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);
        double blue = Color.blue(color) / 255.0;
        blue = blue < 0.03928 ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);
        return (float) ((0.2126 * red) + (0.7152 * green) + (0.0722 * blue));
    }
}
