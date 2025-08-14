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

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.widget.ImageView;

import androidx.annotation.ReplaceWith;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing features in {@link ImageView}.
 *
 * @deprecated Use {@link ImageView} directly.
 */
@Deprecated
public class ImageViewCompat {
    /**
     * Return the tint applied to the image drawable, if specified.
     *
     * @deprecated Call {@link ImageView#getImageTintList()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "view.getImageTintList()")
    public static @Nullable ColorStateList getImageTintList(@NonNull ImageView view) {
        return view.getImageTintList();
    }

    /**
     * Applies a tint to the image drawable.
     *
     * @deprecated Call {@link ImageView#setImageTintList(ColorStateList)} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "view.setImageTintList(tintLint)")
    public static void setImageTintList(@NonNull ImageView view,
            @Nullable ColorStateList tintList) {
        view.setImageTintList(tintList);
    }

    /**
     * Return the blending mode used to apply the tint to the image drawable, if specified.
     *
     * @deprecated Call {@link ImageView#getImageTintMode()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "view.getImageTintMode()")
    public static PorterDuff.@Nullable Mode getImageTintMode(@NonNull ImageView view) {
        return view.getImageTintMode();
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link ImageView#setImageTintList(ColorStateList)}
     * to the image drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @deprecated Call {@link ImageView#setImageTintMode(PorterDuff.Mode)} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "view.setImageTintMode(mode)")
    public static void setImageTintMode(@NonNull ImageView view, PorterDuff.@Nullable Mode mode) {
        view.setImageTintMode(mode);
    }

    private ImageViewCompat() {
    }
}
