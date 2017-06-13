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

package android.support.v4.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

/**
 * Helper for accessing features in {@link ImageView}.
 */
public class ImageViewCompat {
    interface ImageViewCompatImpl {
        ColorStateList getImageTintList(ImageView view);

        void setImageTintList(ImageView view, ColorStateList tintList);

        PorterDuff.Mode getImageTintMode(ImageView view);

        void setImageTintMode(ImageView view, PorterDuff.Mode mode);
    }

    static class BaseViewCompatImpl implements ImageViewCompatImpl {
        @Override
        public ColorStateList getImageTintList(ImageView view) {
            return (view instanceof TintableImageSourceView)
                    ? ((TintableImageSourceView) view).getSupportImageTintList()
                    : null;
        }

        @Override
        public void setImageTintList(ImageView view, ColorStateList tintList) {
            if (view instanceof TintableImageSourceView) {
                ((TintableImageSourceView) view).setSupportImageTintList(tintList);
            }
        }

        @Override
        public void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
            if (view instanceof TintableImageSourceView) {
                ((TintableImageSourceView) view).setSupportImageTintMode(mode);
            }
        }

        @Override
        public PorterDuff.Mode getImageTintMode(ImageView view) {
            return (view instanceof TintableImageSourceView)
                    ? ((TintableImageSourceView) view).getSupportImageTintMode()
                    : null;
        }
    }

    @RequiresApi(21)
    static class LollipopViewCompatImpl extends BaseViewCompatImpl {
        @Override
        public ColorStateList getImageTintList(ImageView view) {
            return view.getImageTintList();
        }

        @Override
        public void setImageTintList(ImageView view, ColorStateList tintList) {
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
        }

        @Override
        public void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
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
        }

        @Override
        public PorterDuff.Mode getImageTintMode(ImageView view) {
            return view.getImageTintMode();
        }
    }

    static final ImageViewCompatImpl IMPL;
    static {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            IMPL = new LollipopViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

    /**
     * Return the tint applied to the image drawable, if specified.
     */
    public static ColorStateList getImageTintList(ImageView view) {
        return IMPL.getImageTintList(view);
    }

    /**
     * Applies a tint to the image drawable.
     */
    public static void setImageTintList(ImageView view, ColorStateList tintList) {
        IMPL.setImageTintList(view, tintList);
    }

    /**
     * Return the blending mode used to apply the tint to the image drawable, if specified.
     */
    public static PorterDuff.Mode getImageTintMode(ImageView view) {
        return IMPL.getImageTintMode(view);
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setImageTintList(android.widget.ImageView, android.content.res.ColorStateList)}
     * to the image drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     */
    public static void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
        IMPL.setImageTintMode(view, mode);
    }

    private ImageViewCompat() {}
}
