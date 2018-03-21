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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for accessing features in {@link ImageView}.
 */
public class ImageViewCompat {
    /**
     * Return the tint applied to the image drawable, if specified.
     */
    @Nullable
    public static ColorStateList getImageTintList(@NonNull ImageView view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getImageTintList();
        }
        return (view instanceof TintableImageSourceView)
                ? ((TintableImageSourceView) view).getSupportImageTintList()
                : null;
    }

    /**
     * Applies a tint to the image drawable.
     */
    public static void setImageTintList(@NonNull ImageView view,
            @Nullable ColorStateList tintList) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setImageTintList(tintList);

            if (Build.VERSION.SDK_INT == 21) {
                // Work around a bug in L that did not update the state of the image source
                // after applying the tint
                Drawable imageViewDrawable = view.getDrawable();
                boolean hasTint = (view.getImageTintList() != null)
                        && (view.getImageTintMode() != null);
                if ((imageViewDrawable != null) && hasTint) {
                    if (imageViewDrawable.isStateful()) {
                        imageViewDrawable.setState(view.getDrawableState());
                    }
                    view.setImageDrawable(imageViewDrawable);
                }
            }
        } else if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintList(tintList);
        }
    }

    /**
     * Return the blending mode used to apply the tint to the image drawable, if specified.
     */
    @Nullable
    public static PorterDuff.Mode getImageTintMode(@NonNull ImageView view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getImageTintMode();
        }
        return (view instanceof TintableImageSourceView)
                ? ((TintableImageSourceView) view).getSupportImageTintMode()
                : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setImageTintList(android.widget.ImageView, android.content.res.ColorStateList)}
     * to the image drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     */
    public static void setImageTintMode(@NonNull ImageView view, @Nullable PorterDuff.Mode mode) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setImageTintMode(mode);

            if (Build.VERSION.SDK_INT == 21) {
                // Work around a bug in L that did not update the state of the image source
                // after applying the tint
                Drawable imageViewDrawable = view.getDrawable();
                boolean hasTint = (view.getImageTintList() != null)
                        && (view.getImageTintMode() != null);
                if ((imageViewDrawable != null) && hasTint) {
                    if (imageViewDrawable.isStateful()) {
                        imageViewDrawable.setState(view.getDrawableState());
                    }
                    view.setImageDrawable(imageViewDrawable);
                }
            }
        } else if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintMode(mode);
        }
    }

    private ImageViewCompat() {}
}
