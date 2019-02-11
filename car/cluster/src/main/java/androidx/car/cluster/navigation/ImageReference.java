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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Reference to an image. This class encapsulates a 'content://' style URI plus metadata that allows
 * consumers to know the image they will receive and how to handle it.
 *
 * <ul>
 * <li><b>Sizing:</b> Producers will always provide an image "original" size which defines the image
 * aspect ratio. When requesting these images, consumers must always specify a desired size (width
 * and height) based on UI available space and the provided aspect ration. Producers can use this
 * "requested" size to select the best version of the requested image, and producers can optionally
 * resize the image to exactly match the "requested" size provided, but consumers should not assume
 * that the received image will match such size. Instead, consumers should always assume that the
 * image will require additional scaling.
 * <li><b>Content:</b> Producers should avoid including margins around the image content.
 * <li><b>Format:</b> Content URI must reference a file with MIME type 'image/png', 'image/jpeg'
 * or 'image/bmp' (vector images are not supported).
 * <li><b>Color:</b> Images can be either "tintable" or not. A "tintable" image is such that all its
 * content is defined in its alpha channel, while its color (all other channels) can be altered
 * without losing information (e.g.: icons). A non "tintable" images contains information in all its
 * channels (e.g.: photos).
 * <li><b>Caching:</b> Given the same image reference and the same requested size, producers must
 * return the exact same image. This means that it should be safe for the consumer to cache an image
 * once downloaded and use this image reference plus requested size as key, for as long as they
 * need. If a producer needs to provide a different version of a certain image, they must provide a
 * different image reference (e.g. producers can opt to include version information as part of the
 * content URI).
 * </ul>
 */
@VersionedParcelize
public class ImageReference implements VersionedParcelable {
    private static final String SCHEME = "content://";
    private static final String WIDTH_HINT_PARAMETER = "w";
    private static final String HEIGHT_HINT_PARAMETER = "h";

    @ParcelField(1)
    String mContentUri;
    @ParcelField(2)
    int mOriginalWidth;
    @ParcelField(3)
    int mOriginalHeight;
    @ParcelField(4)
    boolean mIsTintable;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    ImageReference() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    ImageReference(@NonNull String contentUri,
            @IntRange(from = 1, to = Integer.MAX_VALUE) int originalWidth,
            @IntRange(from = 1, to = Integer.MAX_VALUE) int originalHeight,
            boolean isTintable) {
        mContentUri = Preconditions.checkNotNull(contentUri);
        mOriginalWidth = Preconditions.checkArgumentInRange(originalWidth, 1,
                Integer.MAX_VALUE, "originalWidth");
        mOriginalHeight = Preconditions.checkArgumentInRange(originalHeight, 1,
                Integer.MAX_VALUE, "originalHeight");
        mIsTintable = isTintable;
    }

    /**
     * Builder for creating an {@link ImageReference}.
     */
    public static final class Builder {
        private String mContentUri;
        private int mOriginalWidth;
        private int mOriginalHeight;
        private boolean mIsTintable;

        /**
         * Sets a 'content://' style URI
         *
         * @return this object for chaining
         * @throws NullPointerException if the provided {@code contentUri} is null
         * @throws IllegalArgumentException if the provided {@code contentUri} doesn't start with
         *                                  'content://'.
         */
        @NonNull
        public Builder setContentUri(@NonNull String contentUri) {
            Preconditions.checkNotNull(contentUri);
            Preconditions.checkArgument(contentUri.startsWith(SCHEME));
            mContentUri = contentUri;
            return this;
        }

        /**
         * Sets the aspect ratio of this image, expressed as with and height sizes. Both dimensions
         * must be greater than 0.
         *
         * @return this object for chaining
         * @throws IllegalArgumentException if any of the dimensions is not positive.
         */
        @NonNull
        public Builder setOriginalSize(@IntRange(from = 1, to = Integer.MAX_VALUE) int width,
                @IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
            Preconditions.checkArgumentInRange(width, 1, Integer.MAX_VALUE, "width");
            Preconditions.checkArgumentInRange(height, 1, Integer.MAX_VALUE, "height");
            mOriginalWidth = width;
            mOriginalHeight = height;
            return this;
        }

        /**
         * Sets whether this image is "tintable" or not. An image is "tintable" when all its
         * content is defined in its alpha-channel, designed to be colorized (e.g. using
         * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition).
         * If this method is not used, images will be non "tintable" by default.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setIsTintable(boolean isTintable) {
            mIsTintable = isTintable;
            return this;
        }

        /**
         * Returns a {@link ImageReference} built with the provided information. Calling
         * {@link ImageReference.Builder#setContentUri(String)} and
         * {@link ImageReference.Builder#setOriginalSize(int, int)} before calling this method is
         * mandatory.
         *
         * @return an {@link ImageReference} instance
         * @throws NullPointerException if content URI is not provided.
         * @throws IllegalArgumentException if original size is not set.
         */
        @NonNull
        public ImageReference build() {
            return new ImageReference(mContentUri, mOriginalWidth, mOriginalHeight, mIsTintable);
        }
    }

    /**
     * Returns a 'content://' style URI that can be used to retrieve the actual image, or an empty
     * string if the URI provided by the producer doesn't comply with the format requirements. If
     * this URI is used as-is, the size of the resulting image is undefined.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public String getRawContentUri() {
        String value = Common.nonNullOrEmpty(mContentUri);
        return value.startsWith(SCHEME) ? value : "";
    }

    /**
     * Returns a fully formed {@link Uri} that can be used to retrieve the actual image, including
     * size constraints, or null if this image reference is not properly formed.
     * <p>
     * Producers can optionally use these size constraints to provide an optimized version of the
     * image, but the resulting image might still not match the requested size.
     * <p>
     * Consumers must confirm the size of the received image and scale it proportionally (
     * maintaining the aspect ratio of the received image) if it doesn't match the desired
     * dimensions.
     *
     * @param width desired maximum width (must be greater than 0)
     * @param height desired maximum height (must be greater than 0)
     * @return fully formed {@link Uri}, or null if this image reference can not be used.
     */
    @Nullable
    public Uri getContentUri(@IntRange(from = 1, to = Integer.MAX_VALUE) int width,
            @IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
        Preconditions.checkArgumentInRange(width, 1, Integer.MAX_VALUE, "width");
        Preconditions.checkArgumentInRange(height, 1, Integer.MAX_VALUE, "height");
        String contentUri = getRawContentUri();
        if (contentUri.isEmpty()) {
            // We have an invalid content URI.
            return null;
        }
        return Uri.parse(contentUri).buildUpon()
                .appendQueryParameter(WIDTH_HINT_PARAMETER, String.valueOf(width))
                .appendQueryParameter(HEIGHT_HINT_PARAMETER, String.valueOf(height))
                .build();
    }

    /**
     * Returns the image width, which should only be used to determine the image aspect ratio.
     */
    public int getOriginalWidth() {
        return mOriginalWidth;
    }

    /**
     * Returns the image height, which should only be used to determine the image aspect ratio.
     */
    public int getOriginalHeight() {
        return mOriginalHeight;
    }

    /**
     * Returns whether this image is "tintable" or not. An image is "tintable" when all its
     * content is defined in its alpha-channel, designed to be colorized (e.g. using
     * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition).
     */
    public boolean isTintable() {
        return mIsTintable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImageReference image = (ImageReference) o;
        return Objects.equals(getRawContentUri(), image.getRawContentUri())
                && getOriginalWidth() == image.getOriginalWidth()
                && getOriginalHeight() == image.getOriginalHeight()
                && isTintable() == image.isTintable();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawContentUri(), getOriginalWidth(), getOriginalHeight(),
                isTintable());
    }

    // DefaultLocale suppressed as this method is only offered for debugging purposes.
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{contentUri: '%s', originalWidth: %d, originalHeight: %d, "
                        + "isTintable: %s}",
                mContentUri, mOriginalWidth, mOriginalHeight, mIsTintable);
    }
}
