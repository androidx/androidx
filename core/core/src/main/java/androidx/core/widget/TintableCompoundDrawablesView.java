/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.Nullable;

/**
 * Interface which allows {@link android.widget.TextView} and subclasses to tint compound drawables
 * with {@link TextViewCompat} when running on API v22 devices or lower.
 */
/*
 * When used with androidx.resourceinspection.annotation.AppCompatShadowedAttributes, this
 * interface implies that AppCompat shadows the platform's compound drawable tint attributes.
 * See androidx.resourceinspection.processor for more details and a full mapping of attributes.
 */
public interface TintableCompoundDrawablesView {
    /**
     * Applies a tint to the compound drawables. Does not modify the
     * current tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to
     * {@link android.widget.TextView#setCompoundDrawables(Drawable, Drawable, Drawable, Drawable)}
     * and related methods will automatically mutate the drawables and apply
     * the specified tint and tint mode.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getSupportCompoundDrawablesTintList()
     */
    void setSupportCompoundDrawablesTintList(@Nullable ColorStateList tint);

    /**
     * Return the tint applied to the compound drawables, if specified.
     *
     * @return the tint applied to the compound drawables
     */
    @Nullable
    ColorStateList getSupportCompoundDrawablesTintList();

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setSupportCompoundDrawablesTintList(ColorStateList)} to the compound
     * drawables. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #getSupportCompoundDrawablesTintMode()
     * @see #setSupportCompoundDrawablesTintList(ColorStateList)
     */
    void setSupportCompoundDrawablesTintMode(@Nullable PorterDuff.Mode tintMode);

    /**
     * Returns the blending mode used to apply the tint to the compound
     * drawables, if specified.
     *
     * @return the blending mode used to apply the tint to the compound
     * drawables
     * @see #setSupportCompoundDrawablesTintMode(PorterDuff.Mode)
     */
    @Nullable
    PorterDuff.Mode getSupportCompoundDrawablesTintMode();
}
