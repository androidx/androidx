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
import android.support.annotation.Nullable;

/**
 * Interface which allows a {@link android.widget.CompoundButton} to receive tinting
 * calls from {@code CompoundButtonCompat} when running on API v20 devices or lower.
 */
public interface TintableCompoundButton {

    /**
     * Applies a tint to the button drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to
     * {@link android.widget.CompoundButton#setButtonDrawable(Drawable) setButtonDrawable(Drawable)}
     * should automatically mutate the drawable and apply the specified tint and tint mode.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     */
    void setSupportButtonTintList(@Nullable ColorStateList tint);

    /**
     * Returns the tint applied to the button drawable
     *
     * @see #setSupportButtonTintList(ColorStateList)
     */
    @Nullable
    ColorStateList getSupportButtonTintList();

    /**
     * Specifies the blending mode which should be used to apply the tint specified by
     * {@link #setSupportButtonTintList(ColorStateList)} to the button drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @see #getSupportButtonTintMode()
     * @see android.support.v4.graphics.drawable.DrawableCompat#setTintMode(Drawable,
     * PorterDuff.Mode)
     */
    void setSupportButtonTintMode(@Nullable PorterDuff.Mode tintMode);

    /**
     * Returns the blending mode used to apply the tint to the button drawable
     *
     * @see #setSupportButtonTintMode(PorterDuff.Mode)
     */
    @Nullable
    PorterDuff.Mode getSupportButtonTintMode();
}
