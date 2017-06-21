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
import android.widget.ImageView;

/**
 * Helper for accessing features in {@link ImageView} introduced in later platform releases
 * in a backwards compatible fashion.
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
            return ImageViewCompatBase.getImageTintList(view);
        }

        @Override
        public void setImageTintList(ImageView view, ColorStateList tintList) {
            ImageViewCompatBase.setImageTintList(view, tintList);
        }

        @Override
        public void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
            ImageViewCompatBase.setImageTintMode(view, mode);
        }

        @Override
        public PorterDuff.Mode getImageTintMode(ImageView view) {
            return ImageViewCompatBase.getImageTintMode(view);
        }
    }

    static class LollipopViewCompatImpl extends BaseViewCompatImpl {
        @Override
        public ColorStateList getImageTintList(ImageView view) {
            return ImageViewCompatLollipop.getImageTintList(view);
        }

        @Override
        public void setImageTintList(ImageView view, ColorStateList tintList) {
            ImageViewCompatLollipop.setImageTintList(view, tintList);
        }

        @Override
        public void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
            ImageViewCompatLollipop.setImageTintMode(view, mode);
        }

        @Override
        public PorterDuff.Mode getImageTintMode(ImageView view) {
            return ImageViewCompatLollipop.getImageTintMode(view);
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
