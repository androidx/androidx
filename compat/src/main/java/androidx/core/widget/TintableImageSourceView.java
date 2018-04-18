/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Interface which allows an {@link android.widget.ImageView} to receive image tinting calls
 * from {@link ImageViewCompat} when running on API v20 devices or lower.
 *
 * @hide Internal use only
 */
@RestrictTo(LIBRARY_GROUP)
public interface TintableImageSourceView {

    /**
     * Applies a tint to the image drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to the source's image will automatically
     * mutate the drawable and apply the specified tint and tint mode.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getSupportImageTintList()
     */
    void setSupportImageTintList(@Nullable ColorStateList tint);

    /**
     * Return the tint applied to the image drawable, if specified.
     *
     * @return the tint applied to the image drawable
     */
    @Nullable
    ColorStateList getSupportImageTintList();

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setSupportImageTintList(ColorStateList)}} to the image
     * drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #getSupportImageTintMode()
     */
    void setSupportImageTintMode(@Nullable PorterDuff.Mode tintMode);

    /**
     * Return the blending mode used to apply the tint to the image
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the image drawable
     */
    @Nullable
    PorterDuff.Mode getSupportImageTintMode();
}
