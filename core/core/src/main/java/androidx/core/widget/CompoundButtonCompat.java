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

package androidx.core.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.DrawableCompat;

import java.lang.reflect.Field;

/**
 * Helper for accessing {@link CompoundButton}.
 */
public final class CompoundButtonCompat {
    private static final String TAG = "CompoundButtonCompat";

    private static Field sButtonDrawableField;
    private static boolean sButtonDrawableFieldFetched;

    private CompoundButtonCompat() {}

    /**
     * Applies a tint to the button drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link CompoundButton#setButtonDrawable(Drawable)} should
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link DrawableCompat#setTintList(Drawable, ColorStateList)}.
     *
     * @param button button for which to apply the tint.
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #setButtonTintList(CompoundButton, ColorStateList)
     */
    public static void setButtonTintList(@NonNull CompoundButton button,
            @Nullable ColorStateList tint) {
        if (Build.VERSION.SDK_INT >= 21) {
            Api21Impl.setButtonTintList(button, tint);
        } else if (button instanceof TintableCompoundButton) {
            ((TintableCompoundButton) button).setSupportButtonTintList(tint);
        }
    }

    /**
     * Returns the tint applied to the button drawable
     *
     * @see #setButtonTintList(CompoundButton, ColorStateList)
     */
    @Nullable
    public static ColorStateList getButtonTintList(@NonNull CompoundButton button) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.getButtonTintList(button);
        }
        if (button instanceof TintableCompoundButton) {
            return ((TintableCompoundButton) button).getSupportButtonTintList();
        }
        return null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setButtonTintList(CompoundButton, ColorStateList)}} to the button drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param button button for which to apply the tint mode.
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @see #getButtonTintMode(CompoundButton)
     * @see DrawableCompat#setTintMode(Drawable, PorterDuff.Mode)
     */
    public static void setButtonTintMode(@NonNull CompoundButton button,
            @Nullable PorterDuff.Mode tintMode) {
        if (Build.VERSION.SDK_INT >= 21) {
            Api21Impl.setButtonTintMode(button, tintMode);
        } else if (button instanceof TintableCompoundButton) {
            ((TintableCompoundButton) button).setSupportButtonTintMode(tintMode);
        }
    }

    /**
     * @return the blending mode used to apply the tint to the button drawable
     * @attr name android:buttonTintMode
     * @see #setButtonTintMode(CompoundButton, PorterDuff.Mode)
     */
    @Nullable
    public static PorterDuff.Mode getButtonTintMode(@NonNull CompoundButton button) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.getButtonTintMode(button);
        }
        if (button instanceof TintableCompoundButton) {
            return ((TintableCompoundButton) button).getSupportButtonTintMode();
        }
        return null;
    }

    /**
     * Returns the drawable used as the compound button image
     *
     * @see CompoundButton#setButtonDrawable(Drawable)
     */
    @Nullable
    public static Drawable getButtonDrawable(@NonNull CompoundButton button) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Api23Impl.getButtonDrawable(button);
        }

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

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setButtonTintList(CompoundButton compoundButton, ColorStateList tint) {
            compoundButton.setButtonTintList(tint);
        }

        @DoNotInline
        static ColorStateList getButtonTintList(CompoundButton compoundButton) {
            return compoundButton.getButtonTintList();
        }

        @DoNotInline
        static void setButtonTintMode(CompoundButton compoundButton, PorterDuff.Mode tintMode) {
            compoundButton.setButtonTintMode(tintMode);
        }

        @DoNotInline
        static PorterDuff.Mode getButtonTintMode(CompoundButton compoundButton) {
            return compoundButton.getButtonTintMode();
        }
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Drawable getButtonDrawable(CompoundButton compoundButton) {
            return compoundButton.getButtonDrawable();
        }
    }
}
