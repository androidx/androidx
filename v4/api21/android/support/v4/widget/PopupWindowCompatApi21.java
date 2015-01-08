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
import android.widget.PopupWindow;

import java.lang.reflect.Field;

class PopupWindowCompatApi21 {

    private static final String TAG = "PopupWindowCompatApi21";

    private static Field sOverlapAnchorField;

    static {
        try {
            sOverlapAnchorField = PopupWindow.class.getDeclaredField("mOverlapAnchor");
            sOverlapAnchorField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.i(TAG, "Could not fetch mOverlapAnchor field from PopupWindow", e);
        }
    }

    static void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor) {
        if (sOverlapAnchorField != null) {
            try {
                sOverlapAnchorField.set(popupWindow, overlapAnchor);
            } catch (IllegalAccessException e) {
                Log.i(TAG, "Could not set overlap anchor field in PopupWindow", e);
            }
        }
    }

    static boolean getOverlapAnchor(PopupWindow popupWindow) {
        if (sOverlapAnchorField != null) {
            try {
                return (Boolean) sOverlapAnchorField.get(popupWindow);
            } catch (IllegalAccessException e) {
                Log.i(TAG, "Could not get overlap anchor field in PopupWindow", e);
            }
        }
        return false;
    }

}
