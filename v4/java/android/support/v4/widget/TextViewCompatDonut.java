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

package android.support.v4.widget;

import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.Field;

class TextViewCompatDonut {

    private static final String LOG_TAG = "TextViewCompatDonut";
    private static final int LINES = 1;

    private static Field sMaximumField;
    private static boolean sMaximumFieldFetched;
    private static Field sMaxModeField;
    private static boolean sMaxModeFieldFetched;

    private static Field sMinimumField;
    private static boolean sMinimumFieldFetched;
    private static Field sMinModeField;
    private static boolean sMinModeFieldFetched;

    static int getMaxLines(TextView textView) {
        if (!sMaxModeFieldFetched) {
            sMaxModeField = retrieveField("mMaxMode");
            sMaxModeFieldFetched = true;
        }
        if (sMaxModeField != null && retrieveIntFromField(sMaxModeField, textView) == LINES) {
            // If the max mode is using lines, we can grab the maximum value
            if (!sMaximumFieldFetched) {
                sMaximumField = retrieveField("mMaximum");
                sMaximumFieldFetched = true;
            }
            if (sMaximumField != null) {
                return retrieveIntFromField(sMaximumField, textView);
            }
        }
        return -1;
    }

    static int getMinLines(TextView textView) {
        if (!sMinModeFieldFetched) {
            sMinModeField = retrieveField("mMinMode");
            sMinModeFieldFetched = true;
        }
        if (sMinModeField != null && retrieveIntFromField(sMinModeField, textView) == LINES) {
            // If the min mode is using lines, we can grab the maximum value
            if (!sMinimumFieldFetched) {
                sMinimumField = retrieveField("mMinimum");
                sMinimumFieldFetched = true;
            }
            if (sMinimumField != null) {
                return retrieveIntFromField(sMinimumField, textView);
            }
        }
        return -1;
    }

    private static Field retrieveField(String fieldName) {
        Field field = null;
        try {
            field = TextView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, "Could not retrieve " + fieldName + " field.");
        }
        return field;
    }

    private static int retrieveIntFromField(Field field, TextView textView) {
        try {
            return field.getInt(textView);
        } catch (IllegalAccessException e) {
            Log.d(LOG_TAG, "Could not retrieve value of " + field.getName() + " field.");
        }
        return -1;
    }

    static void setTextAppearance(TextView textView, int resId) {
        textView.setTextAppearance(textView.getContext(), resId);
    }
}
