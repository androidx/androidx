/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v4.content.res;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.AnyRes;
import android.support.annotation.StyleableRes;
import android.util.TypedValue;

/**
 * Compat methods for accessing TypedArray values.
 *
 * @hide
 */
public class TypedArrayUtils {
    public static boolean getBoolean(TypedArray a, @StyleableRes int index,
            @StyleableRes int fallbackIndex, boolean defaultValue) {
        boolean val = a.getBoolean(fallbackIndex, defaultValue);
        return a.getBoolean(index, val);
    }

    public static Drawable getDrawable(TypedArray a, @StyleableRes int index,
            @StyleableRes int fallbackIndex) {
        Drawable val = a.getDrawable(index);
        if (val == null) {
            val = a.getDrawable(fallbackIndex);
        }
        return val;
    }

    public static int getInt(TypedArray a, @StyleableRes int index,
            @StyleableRes int fallbackIndex, int defaultValue) {
        int val = a.getInt(fallbackIndex, defaultValue);
        return a.getInt(index, val);
    }

    public static @AnyRes int getResourceId(TypedArray a, @StyleableRes int index,
            @StyleableRes int fallbackIndex, @AnyRes int defaultValue) {
        int val = a.getResourceId(fallbackIndex, defaultValue);
        return a.getResourceId(index, val);
    }

    public static String getString(TypedArray a, @StyleableRes int index,
            @StyleableRes int fallbackIndex) {
        String val = a.getString(index);
        if (val == null) {
            val = a.getString(fallbackIndex);
        }
        return val;
    }

    public static CharSequence[] getTextArray(TypedArray a, @StyleableRes int index,
            @StyleableRes int fallbackIndex) {
        CharSequence[] val = a.getTextArray(index);
        if (val == null) {
            val = a.getTextArray(fallbackIndex);
        }
        return val;
    }

    public static int getAttr(Context context, int attr, int fallbackAttr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return attr;
        }
        return fallbackAttr;
    }
}
