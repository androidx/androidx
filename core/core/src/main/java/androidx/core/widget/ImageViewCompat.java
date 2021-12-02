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

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

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
            return Api21Impl.getImageTintList(view);
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
            Api21Impl.setImageTintList(view, tintList);

            if (Build.VERSION.SDK_INT == 21) {
                // Work around a bug in L that did not update the state of the image source
                // after applying the tint
                Drawable imageViewDrawable = view.getDrawable();
                if ((imageViewDrawable != null) && (Api21Impl.getImageTintList(view) != null)) {
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
            return Api21Impl.getImageTintMode(view);
        }
        return (view instanceof TintableImageSourceView)
                ? ((TintableImageSourceView) view).getSupportImageTintMode()
                : null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setImageTintList(ImageView, ColorStateList)}
     * to the image drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     */
    public static void setImageTintMode(@NonNull ImageView view, @Nullable PorterDuff.Mode mode) {
        if (Build.VERSION.SDK_INT >= 21) {
            Api21Impl.setImageTintMode(view, mode);

            if (Build.VERSION.SDK_INT == 21) {
                // Work around a bug in L that did not update the state of the image source
                // after applying the tint
                Drawable imageViewDrawable = view.getDrawable();
                if ((imageViewDrawable != null) && (Api21Impl.getImageTintList(view) != null)) {
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

    private ImageViewCompat() {
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static ColorStateList getImageTintList(ImageView imageView) {
            return imageView.getImageTintList();
        }

        @DoNotInline
        static void setImageTintList(ImageView imageView, ColorStateList tint) {
            imageView.setImageTintList(tint);
        }

        @DoNotInline
        static PorterDuff.Mode getImageTintMode(ImageView imageView) {
            return imageView.getImageTintMode();
        }

        @DoNotInline
        static void setImageTintMode(ImageView imageView, PorterDuff.Mode tintMode) {
            imageView.setImageTintMode(tintMode);
        }
    }
}
