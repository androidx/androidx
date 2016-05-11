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


package android.support.v4.app;

import android.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Log;

class ActionBarDrawerToggleJellybeanMR2 {
    private static final String TAG = "ActionBarDrawerToggleImplJellybeanMR2";

    private static final int[] THEME_ATTRS = new int[] {
            R.attr.homeAsUpIndicator
    };

    public static Object setActionBarUpIndicator(Object info, Activity activity,
            Drawable drawable, int contentDescRes) {
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(drawable);
            actionBar.setHomeActionContentDescription(contentDescRes);
        }
        return info;
    }

    public static Object setActionBarDescription(Object info, Activity activity,
            int contentDescRes) {
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setHomeActionContentDescription(contentDescRes);
        }
        return info;
    }

    public static Drawable getThemeUpIndicator(Activity activity) {
        final ActionBar actionBar = activity.getActionBar();
        final Context context;
        if (actionBar != null) {
            context = actionBar.getThemedContext();
        } else {
            context = activity;
        }

        final TypedArray a = context.obtainStyledAttributes(null, THEME_ATTRS,
                R.attr.actionBarStyle, 0);
        final Drawable result = a.getDrawable(0);
        a.recycle();
        return result;
    }
}
