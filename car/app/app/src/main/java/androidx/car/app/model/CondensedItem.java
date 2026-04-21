/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A main UI component representing an item in a condensed format.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class CondensedItem implements Item {
    /**
     * The type of images supported within condensed items.
     */
    @RestrictTo(LIBRARY)
    @IntDef(value = {IMAGE_TYPE_ICON, IMAGE_TYPE_SMALL, IMAGE_TYPE_LARGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CondensedItemImageType {
    }

    /**
     * Represents an icon-sized image. The host renders it within small, fixed bounds.
     */
    public static final int IMAGE_TYPE_ICON = 1;

    /**
     * Represents a small image. The host renders it with standard padding and scales
     * the image to fit within the bounds.
     */
    public static final int IMAGE_TYPE_SMALL = 2;

    /**
     * Represents a large image. The host renders it edge-to-edge, scaling the image
     * to fill and potentially crop within the bounds.
     */
    public static final int IMAGE_TYPE_LARGE = 3;

    private final @Nullable CarText mTitle;
    private final @Nullable CarText mText;
    private final @Nullable CarIcon mLeadingImage;
    @CondensedItemImageType
    private final int mLeadingImageType;
    private final @Nullable CarIcon mTrailingImage;
    @CondensedItemImageType
    private final int mTrailingImageType;
    private final @Nullable CondensedItemStyle mStyle;
    private final @Nullable OnClickDelegate mOnClickDelegate;
    private final @Nullable CarProgressBar mProgressBar;

    /**
     * Returns the title of the item, or {@code null} if not set.
     */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the text of the item, or {@code null} if not set.
     */
    public @Nullable CarText getText() {
        return mText;
    }

    /**
     * Returns the leading image of the item, or {@code null} if not set.
     */
    public @Nullable CarIcon getLeadingImage() {
        return mLeadingImage;
    }

    /**
     * Returns the type of the leading image.
     */
    @CondensedItemImageType
    public int getLeadingImageType() {
        return mLeadingImageType;
    }

    /**
     * Returns the trailing image of the item, or {@code null} if not set.
     */
    public @Nullable CarIcon getTrailingImage() {
        return mTrailingImage;
    }

    /**
     * Returns the type of the trailing image.
     */
    @CondensedItemImageType
    public int getTrailingImageType() {
        return mTrailingImageType;
    }

    /**
     * Returns the style of the item, or {@code null} if not set.
     */
    public @Nullable CondensedItemStyle getStyle() {
        return mStyle;
    }

    /**
     * Returns the {@link OnClickDelegate} for the item, or {@code null} if not set.
     */
    public @Nullable OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    /**
     * Returns the {@link CarProgressBar} for the item, or {@code null} if not set.
     */
    public @Nullable CarProgressBar getProgressBar() {
        return mProgressBar;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CondensedItem)) {
            return false;
        }
        CondensedItem otherItem = (CondensedItem) other;
        return Objects.equals(mTitle, otherItem.mTitle)
                && Objects.equals(mText, otherItem.mText)
                && Objects.equals(mLeadingImage, otherItem.mLeadingImage)
                && mLeadingImageType == otherItem.mLeadingImageType
                && Objects.equals(mTrailingImage, otherItem.mTrailingImage)
                && mTrailingImageType == otherItem.mTrailingImageType
                && Objects.equals(mStyle, otherItem.mStyle)
                && Objects.equals(mOnClickDelegate == null, otherItem.mOnClickDelegate == null)
                && Objects.equals(mProgressBar, otherItem.mProgressBar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mText, mLeadingImage, mLeadingImageType, mTrailingImage,
                mTrailingImageType, mStyle, mOnClickDelegate, mProgressBar);
    }

    @Override
    public @NonNull String toString() {
        return "CondensedItem { title: " + mTitle + ", text: " + mText + ", leadingImage: "
                + mLeadingImage + ", leadingImageType: " + mLeadingImageType + ", trailingImage: "
                + mTrailingImage + ", trailingImageType: " + mTrailingImageType + ", style: "
                + mStyle + ", onClickDelegate: " + mOnClickDelegate + ", progressBar: "
                + mProgressBar + " }";
    }

    private CondensedItem(@NonNull Builder builder) {
        mTitle = builder.mTitle;
        mText = builder.mText;
        mLeadingImage = builder.mLeadingImage;
        mLeadingImageType = builder.mLeadingImageType;
        mTrailingImage = builder.mTrailingImage;
        mTrailingImageType = builder.mTrailingImageType;
        mStyle = builder.mStyle;
        mOnClickDelegate = builder.mOnClickDelegate;
        mProgressBar = builder.mProgressBar;
    }

    /** For serialization. */
    private CondensedItem() {
        mTitle = null;
        mText = null;
        mLeadingImage = null;
        mLeadingImageType = IMAGE_TYPE_SMALL;
        mTrailingImage = null;
        mTrailingImageType = IMAGE_TYPE_SMALL;
        mStyle = null;
        mOnClickDelegate = null;
        mProgressBar = null;
    }

    /** A builder for {@link CondensedItem}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder {
        @Nullable CarText mTitle;
        @Nullable CarText mText;
        @Nullable CarIcon mLeadingImage;
        @CondensedItemImageType
        int mLeadingImageType = IMAGE_TYPE_SMALL;
        @Nullable CarIcon mTrailingImage;
        @CondensedItemImageType
        int mTrailingImageType = IMAGE_TYPE_SMALL;
        @Nullable CondensedItemStyle mStyle;
        @Nullable OnClickDelegate mOnClickDelegate;
        @Nullable CarProgressBar mProgressBar;

        /**
         * Sets the title of the item.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            CarText carText = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(carText);
            mTitle = carText;
            return this;
        }

        /**
         * Sets the title of the item.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setTitle(@NonNull CarText title) {
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(requireNonNull(title));
            mTitle = title;
            return this;
        }

        /**
         * Sets the text of the item.
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setText(@NonNull CharSequence text) {
            CarText carText = CarText.create(requireNonNull(text));
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(carText);
            mText = carText;
            return this;
        }

        /**
         * Sets the text of the item.
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setText(@NonNull CarText text) {
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(requireNonNull(text));
            mText = text;
            return this;
        }

        /**
         * Sets the leading image of the item.
         *
         * <p>The leading image will default to {@link #IMAGE_TYPE_SMALL}.
         *
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if {@code image} contains unsupported icon types
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setLeadingImage(@NonNull CarIcon image) {
            return setLeadingImage(requireNonNull(image), IMAGE_TYPE_SMALL);
        }

        /**
         * Sets the leading image of the item.
         *
         * @param image     the {@link CarIcon} for the leading image
         * @param imageType the {@link CondensedItemImageType} for the leading image
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if {@code image} contains unsupported icon types
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setLeadingImage(@NonNull CarIcon image,
                @CondensedItemImageType int imageType) {
            CarIconConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(image));
            mLeadingImage = image;
            mLeadingImageType = imageType;
            return this;
        }

        /**
         * Sets the trailing image of the item.
         *
         * <p>The trailing image will default to {@link #IMAGE_TYPE_SMALL}.
         *
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if {@code image} contains unsupported icon types
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setTrailingImage(@NonNull CarIcon image) {
            return setTrailingImage(requireNonNull(image), IMAGE_TYPE_SMALL);
        }

        /**
         * Sets the trailing image of the item.
         *
         * @param image     the {@link CarIcon} for the trailing image
         * @param imageType the {@link CondensedItemImageType} for the trailing image
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if {@code image} contains unsupported icon types
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setTrailingImage(@NonNull CarIcon image,
                @CondensedItemImageType int imageType) {
            CarIconConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(image));
            mTrailingImage = image;
            mTrailingImageType = imageType;
            return this;
        }

        /**
         * Sets the {@link CondensedItemStyle} for the item.
         *
         * @throws NullPointerException if {@code style} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setStyle(@NonNull CondensedItemStyle style) {
            mStyle = requireNonNull(style);
            return this;
        }

        /**
         * Sets the {@link OnClickListener} for the item.
         *
         * @throws NullPointerException if {@code onClickListener} is {@code null}
         */
        @CanIgnoreReturnValue
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public @NonNull Builder setOnClickListener(@NonNull OnClickListener onClickListener) {
            mOnClickDelegate = OnClickDelegateImpl.create(requireNonNull(onClickListener));
            return this;
        }

        /**
         * Sets the {@link CarProgressBar} for the item.
         *
         * @throws NullPointerException if {@code progressBar} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setProgressBar(@NonNull CarProgressBar progressBar) {
            mProgressBar = requireNonNull(progressBar);
            return this;
        }

        /**
         * Constructs a {@link CondensedItem} from the current state of this builder.
         *
         * @throws IllegalStateException if {@code mTitle}, {@code mText}, {@code mLeadingImage},
         *                               AND {@code mTrailingImage} are all {@code null}.
         */
        public @NonNull CondensedItem build() {
            if (mTitle == null && mText == null && mLeadingImage == null
                    && mTrailingImage == null) {
                throw new IllegalStateException("At least one of title, text, leading image, or "
                        + "trailing image must be set");
            }
            return new CondensedItem(this);
        }
    }
}
