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
     * Represents an icon to be displayed in the condensed item.
     *
     * <p>A tint color is expected to be provided via {@link CarIcon.Builder#setTint}. Otherwise, a
     * default tint color as determined by the host will be applied.
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
    private final boolean mIndexable;

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

    /**
     * Returns whether the item can be included in indexed lists.
     *
     * @see Builder#setIndexable(boolean)
     */
    public boolean isIndexable() {
        return mIndexable;
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
                && mIndexable == otherItem.mIndexable
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
        mIndexable = builder.mIndexable;
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
        mIndexable = true;
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
        boolean mIndexable = true;

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
         * <p>{@code title} must conform to {@link CarTextConstraints.TEXT_AND_ICON}.
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
         * <p>{@code text} must conform to {@link CarTextConstraints.TEXT_WITH_COLORS_AND_ICON}.
         *
         * <p><strong>Note:</strong> This field is mutually exclusive with {@link #setProgressBar}.
         * If both are set, {@link #build()} will throw an {@link IllegalStateException}.
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setText(@NonNull CharSequence text) {
            setText(CarText.create(requireNonNull(text)));
            return this;
        }

        /**
         * Sets the text of the item.
         *
         * <p>{@code text} must conform to {@link CarTextConstraints.TEXT_WITH_COLORS_AND_ICON}.
         *
         * <p><strong>Note:</strong> This field is mutually exclusive with {@link #setProgressBar}.
         * If both are set, {@link #build()} will throw an {@link IllegalStateException}.
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
         * <p><strong>Note:</strong> This field is mutually exclusive with {@link #setText}.
         * If both are set, {@link #build()} will throw an {@link IllegalStateException}.
         *
         * @throws NullPointerException if {@code progressBar} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setProgressBar(@NonNull CarProgressBar progressBar) {
            mProgressBar = requireNonNull(progressBar);
            return this;
        }

        /**
         * Sets whether this item can be included in indexed lists. By default, this is set to
         * {@code true}.
         *
         * <p>The host creates indexed lists to help users navigate through long lists more easily
         * by sorting, filtering, or some other means.
         *
         * <p>For example, a media app may, by default, show a user's playlists sorted by date
         * created. If the app provides these playlists via the {@code SectionedItemTemplate} and
         * enables {@code #isAlphabeticalIndexingAllowed}, the user will be able to select a letter
         * on a keyboard to jump to their playlists that start with that letter. When this happens,
         * the list is reconstructed and sorted alphabetically, then shown to the user, jumping down
         * to the letter. Items that are set to {@code #setIndexable(false)}, do not show up in this
         * new sorted list. Sticking with the media example, a media app may choose to hide things
         * like "autogenerated playlists" from the list and only keep user created playlists.
         *
         * <p>Individual items can be set to be included or excluded from filtered lists, but it's
         * also possible to enable/disable the creation of filtered lists as a whole via the
         * template's API (eg. {@code SectionedItemTemplate
         * .Builder#setAlphabeticalIndexingStrategy(int)}).
         */
        public @NonNull Builder setIndexable(boolean indexable) {
            mIndexable = indexable;
            return this;
        }

        /**
         * Constructs a {@link CondensedItem} from the current state of this builder.
         *
         * @throws IllegalStateException if {@code mTitle}, {@code mText}, {@code mLeadingImage},
         *                               AND {@code mTrailingImage} are all {@code null}.
         * @throws IllegalStateException if both {@code mText} and {@code mProgressBar} are set.
         */
        public @NonNull CondensedItem build() {
            if (mTitle == null && mText == null && mLeadingImage == null
                    && mTrailingImage == null) {
                throw new IllegalStateException("At least one of title, text, leading image, or "
                        + "trailing image must be set");
            }

            if (mText != null && mProgressBar != null) {
                throw new IllegalStateException(
                        "Both text and progress bar cannot be set on CondensedItem");
            }
            return new CondensedItem(this);
        }
    }
}
