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

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.CompoundButton;

import java.lang.reflect.Field;

class CompoundButtonCompatDonut {

    private static final String TAG = "CompoundButtonCompatDonut";

    private static Field sButtonDrawableField;
    private static boolean sButtonDrawableFieldFetched;

    static void setButtonTintList(CompoundButton button, ColorStateList tint) {
        if (button instanceof TintableCompoundButton) {
            ((TintableCompoundButton) button).setSupportButtonTintList(tint);
        }
    }

    static ColorStateList getButtonTintList(CompoundButton button) {
        if (button instanceof TintableCompoundButton) {
             return((TintableCompoundButton) button).getSupportButtonTintList();
        }
        return null;
    }

    static void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode) {
        if (button instanceof TintableCompoundButton) {
            ((TintableCompoundButton) button).setSupportButtonTintMode(tintMode);
        }
    }

    static PorterDuff.Mode getButtonTintMode(CompoundButton button) {
        if (button instanceof TintableCompoundButton) {
            return ((TintableCompoundButton) button).getSupportButtonTintMode();
        }
        return null;
    }

    static Drawable getButtonDrawable(CompoundButton button) {
        if (!sButtonDrawableFieldFetched) {
            try {
                sButtonDrawableField = CompoundButton.class.getDeclaredField("mButtonDrawable");
                sButtonDrawableField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.i(TAG, "Failed to retrieve mButtonDrawable field", e);
            }
            sButtonDrawableFieldFetched = true;
        }

        if (sButtonDrawableField != null) {
            try {
                return (Drawable) sButtonDrawableField.get(button);
            } catch (IllegalAccessException e) {
                Log.i(TAG, "Failed to get button drawable via reflection", e);
                sButtonDrawableField = null;
            }
        }
        return null;
    }

}
