/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline.common;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Specifies the style for an  {@link ImageView}.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public final class ImageViewStyle extends ViewStyle {
    private static final String TAG = "ImageViewStyle";

    private static final String KEY_IMAGE_VIEW_STYLE = "image_view_style";
    private static final String KEY_IMAGE_SCALE_TYPE = "image_scale_type";
    private static final String KEY_IMAGE_MAX_WIDTH = "image_max_width";
    private static final String KEY_IMAGE_MAX_HEIGHT = "image_max_height";
    private static final String KEY_IMAGE_TINT_LIST = "image_tint_list";

    /**
     * This is made public so it can be used by the renderer to converted the received bundle to
     * a style. It does not validate the provided bundle. {@link #isValid()} or
     * {@link #assertIsValid()} can be used for validation.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ImageViewStyle(@NonNull Bundle bundle) {
        super(bundle);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    @Override
    protected String getStyleKey() {
        return KEY_IMAGE_VIEW_STYLE;
    }

    /**
     * Applies the specified style on the {@code imageView}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressWarnings("deprecation")
    public void applyStyleOnImageViewIfValid(@NonNull ImageView imageView) {
        if (!isValid()) {
            return;
        }
        super.applyStyleOnViewIfValid(imageView);
        if (mBundle.containsKey(KEY_IMAGE_MAX_WIDTH)) {
            imageView.setMaxWidth(mBundle.getInt(KEY_IMAGE_MAX_WIDTH));
            imageView.setAdjustViewBounds(true);
        }
        if (mBundle.containsKey(KEY_IMAGE_MAX_HEIGHT)) {
            imageView.setMaxHeight(mBundle.getInt(KEY_IMAGE_MAX_HEIGHT));
            imageView.setAdjustViewBounds(true);
        }
        if (mBundle.containsKey(KEY_IMAGE_TINT_LIST)) {
            ColorStateList imageTintList = mBundle.getParcelable(KEY_IMAGE_TINT_LIST);
            if (imageTintList != null) {
                imageView.setImageTintList(imageTintList);
            }
        }
        if (mBundle.containsKey(KEY_IMAGE_SCALE_TYPE)) {
            String scaleTypeString = mBundle.getString(KEY_IMAGE_SCALE_TYPE);
            if (scaleTypeString != null) {
                try {
                    imageView.setScaleType(ImageView.ScaleType.valueOf(scaleTypeString));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Cannot recognize the scale type: " + scaleTypeString);
                }
            }
        }
    }

    /**
     * Builder for the {@link ImageViewStyle}.
     */
    public static final class Builder extends BaseBuilder<ImageViewStyle, Builder> {

        public Builder() {
            super(KEY_IMAGE_VIEW_STYLE);
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        @Override
        protected Builder getThis() {
            return this;
        }

        /**
         * Sets the scale type of the image view.
         *
         * @param scaleType The desired scaling mode.
         * @see ImageView#setScaleType(ImageView.ScaleType)
         */
        @NonNull
        public Builder setScaleType(@NonNull ImageView.ScaleType scaleType) {
            Preconditions.checkNotNull(scaleType, "scaleType should not be null");
            mBundle.putString(KEY_IMAGE_SCALE_TYPE, scaleType.name());
            return this;
        }

        /**
         * Sets a maximum width for the image view.
         *
         * @param maxWidth maximum width for this view
         * @see ImageView#setMaxWidth(int)
         */
        @NonNull
        public Builder setMaxWidth(int maxWidth) {
            mBundle.putInt(KEY_IMAGE_MAX_WIDTH, maxWidth);
            return this;
        }

        /**
         * Sets a maximum height for the image view.
         *
         * @param maxHeight maximum height for this view
         * @see ImageView#setMaxHeight(int)
         */
        @NonNull
        public Builder setMaxHeight(int maxHeight) {
            mBundle.putInt(KEY_IMAGE_MAX_HEIGHT, maxHeight);
            return this;
        }

        /**
         * Sets a tint color to the image view.
         *
         * @param imageTintList the tint color to apply to the image
         * @see ImageView#setImageTintList(ColorStateList)
         */
        @NonNull
        public Builder setTintList(@NonNull ColorStateList imageTintList) {
            Preconditions.checkNotNull(imageTintList, "imageTintList should not be null");
            mBundle.putParcelable(KEY_IMAGE_TINT_LIST, imageTintList);
            return this;
        }

        @NonNull
        @Override
        public ImageViewStyle build() {
            return new ImageViewStyle(mBundle);
        }
    }
}
