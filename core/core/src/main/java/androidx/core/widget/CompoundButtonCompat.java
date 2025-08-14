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

import androidx.annotation.ReplaceWith;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing {@link CompoundButton}.
 *
 * @deprecated Use {@link CompoundButton} directly.
 */
@Deprecated
public final class CompoundButtonCompat {
    private CompoundButtonCompat() {}

    /**
     * Applies a tint to the button drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link CompoundButton#setButtonDrawable(Drawable)} should
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param button button for which to apply the tint.
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see CompoundButton#setButtonTintList(ColorStateList)
     * @deprecated Call {@link CompoundButton#setButtonTintList(ColorStateList)} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "button.setButtonTintList(tint)")
    public static void setButtonTintList(@NonNull CompoundButton button,
            @Nullable ColorStateList tint) {
        button.setButtonTintList(tint);
    }

    /**
     * Returns the tint applied to the button drawable
     *
     * @see CompoundButton#setButtonTintList(ColorStateList)
     * @deprecated Call {@link CompoundButton#getButtonTintList()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "button.getButtonTintList()")
    public static @Nullable ColorStateList getButtonTintList(@NonNull CompoundButton button) {
        return button.getButtonTintList();
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link CompoundButton#setButtonTintList(ColorStateList)}} to the button drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param button button for which to apply the tint mode.
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see CompoundButton#getButtonTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     * @deprecated Call {@link CompoundButton#setButtonTintMode(PorterDuff.Mode)} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "button.setButtonTintMode(tintMode)")
    public static void setButtonTintMode(@NonNull CompoundButton button,
            PorterDuff.@Nullable Mode tintMode) {
        button.setButtonTintMode(tintMode);
    }

    /**
     * @return the blending mode used to apply the tint to the button drawable
     * @attr name android:buttonTintMode
     * @see CompoundButton#setButtonTintMode(PorterDuff.Mode)
     * @deprecated Call {@link CompoundButton#getButtonTintMode()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "button.getButtonTintMode()")
    public static PorterDuff.@Nullable Mode getButtonTintMode(@NonNull CompoundButton button) {
        return button.getButtonTintMode();
    }

    /**
     * Returns the drawable used as the compound button image
     *
     * @see CompoundButton#setButtonDrawable(Drawable)
     * @deprecated Call {@link CompoundButton#getButtonDrawable()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "button.getButtonDrawable()")
    public static @Nullable Drawable getButtonDrawable(@NonNull CompoundButton button) {
        return button.getButtonDrawable();
    }
}
