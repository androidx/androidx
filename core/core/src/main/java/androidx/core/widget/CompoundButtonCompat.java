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
import android.widget.CompoundButton;

import androidx.core.graphics.drawable.DrawableCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing {@link CompoundButton}.
 */
public final class CompoundButtonCompat {
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
        Api21Impl.setButtonTintList(button, tint);
    }

    /**
     * Returns the tint applied to the button drawable
     *
     * @see #setButtonTintList(CompoundButton, ColorStateList)
     */
    public static @Nullable ColorStateList getButtonTintList(@NonNull CompoundButton button) {
        return Api21Impl.getButtonTintList(button);
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
            PorterDuff.@Nullable Mode tintMode) {
        Api21Impl.setButtonTintMode(button, tintMode);
    }

    /**
     * @return the blending mode used to apply the tint to the button drawable
     * @attr name android:buttonTintMode
     * @see #setButtonTintMode(CompoundButton, PorterDuff.Mode)
     */
    public static PorterDuff.@Nullable Mode getButtonTintMode(@NonNull CompoundButton button) {
        return Api21Impl.getButtonTintMode(button);
    }

    /**
     * Returns the drawable used as the compound button image
     *
     * @see CompoundButton#setButtonDrawable(Drawable)
     */
    public static @Nullable Drawable getButtonDrawable(@NonNull CompoundButton button) {
        return button.getButtonDrawable();
    }

    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        static void setButtonTintList(CompoundButton compoundButton, ColorStateList tint) {
            compoundButton.setButtonTintList(tint);
        }

        static ColorStateList getButtonTintList(CompoundButton compoundButton) {
            return compoundButton.getButtonTintList();
        }

        static void setButtonTintMode(CompoundButton compoundButton, PorterDuff.Mode tintMode) {
            compoundButton.setButtonTintMode(tintMode);
        }

        static PorterDuff.Mode getButtonTintMode(CompoundButton compoundButton) {
            return compoundButton.getButtonTintMode();
        }
    }
}
