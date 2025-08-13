/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.widget.CheckedTextView;

import androidx.core.graphics.drawable.DrawableCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing {@link CheckedTextView}.
 */
public final class CheckedTextViewCompat {

    private CheckedTextViewCompat() {
    }

    /**
     * Applies a tint to the check mark drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link CheckedTextView#setCheckMarkDrawable(Drawable)} should
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link DrawableCompat#setTintList(Drawable, ColorStateList)}.
     *
     * @param textView CheckedTextView for which to apply the tint.
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #setCheckMarkTintList(CheckedTextView, ColorStateList)
     */
    public static void setCheckMarkTintList(@NonNull CheckedTextView textView,
            @Nullable ColorStateList tint) {
        textView.setCheckMarkTintList(tint);
    }

    /**
     * Returns the tint applied to the check mark drawable
     *
     * @see #setCheckMarkTintList(CheckedTextView, ColorStateList)
     */
    public static @Nullable ColorStateList getCheckMarkTintList(@NonNull CheckedTextView textView) {
        return textView.getCheckMarkTintList();
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setCheckMarkTintList(CheckedTextView, ColorStateList)}} to the check mark drawable.
     * The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param textView CheckedTextView for which to apply the tint mode.
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #getCheckMarkTintMode(CheckedTextView)
     * @see DrawableCompat#setTintMode(Drawable, PorterDuff.Mode)
     */
    public static void setCheckMarkTintMode(@NonNull CheckedTextView textView,
            PorterDuff.@Nullable Mode tintMode) {
        textView.setCheckMarkTintMode(tintMode);
    }

    /**
     * @return the blending mode used to apply the tint to the check mark drawable
     * @attr name android:checkMarkTintMode
     * @see #setCheckMarkTintMode(CheckedTextView, PorterDuff.Mode)
     */
    public static PorterDuff.@Nullable Mode getCheckMarkTintMode(
            @NonNull CheckedTextView textView) {
        return textView.getCheckMarkTintMode();
    }

    /**
     * Returns the drawable used as the check mark image
     *
     * @see CheckedTextView#setCheckMarkDrawable(Drawable)
     * @deprecated Call {@link CheckedTextView#getCheckMarkDrawable()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "textView.getCheckMarkDrawable()")
    public static @Nullable Drawable getCheckMarkDrawable(@NonNull CheckedTextView textView) {
        return textView.getCheckMarkDrawable();
    }
}
