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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.Screen;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents a grid item with an image and an optional title.
 */
@CarProtocol
@KeepFields
public final class GridItem implements Item {
    /**
     * The type of images supported within grid items.
     */
    @RestrictTo(LIBRARY)
    @IntDef(value = {IMAGE_TYPE_ICON, IMAGE_TYPE_LARGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GridItemImageType {
    }

    /**
     * Represents an icon to be displayed in the grid item.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * icons targeting a 128 x 128 dp bounding box. If necessary, the icon will be scaled down while
     * preserving its aspect ratio.
     *
     * <p>A tint color is expected to be provided via {@link CarIcon.Builder#setTint}. Otherwise, a
     * default tint color as determined by the host will be applied.
     */
    public static final int IMAGE_TYPE_ICON = (1 << 0);

    /**
     * Represents a large image to be displayed in the grid item.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * images targeting a 128 x 128 dp bounding box. If necessary, the image will be scaled down
     * while preserving its aspect ratio.
     */
    public static final int IMAGE_TYPE_LARGE = (1 << 1);

    private final boolean mIsLoading;
    @Nullable
    private final CarText mTitle;
    @Nullable
    private final CarText mText;
    @Nullable
    private final CarIcon mImage;
    @GridItemImageType
    private final int mImageType;
    @Nullable
    private final OnClickDelegate mOnClickDelegate;
    @Nullable
    private final Badge mBadge;
    private final boolean mIndexable;

    /**
     * Returns whether the grid item is in a loading state.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the title of the grid item or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the text to display below the title or {@code null} if no text will be displayed
     * below the title.
     *
     * @see Builder#setText(CharSequence)
     */
    @Nullable
    public CarText getText() {
        return mText;
    }

    /**
     * Returns the image of the grid item or {@code null} if not set.
     *
     * @see Builder#setImage(CarIcon)
     */
    @Nullable
    public CarIcon getImage() {
        return mImage;
    }

    /** Returns the image type of the grid item. */
    @GridItemImageType
    public int getImageType() {
        return mImageType;
    }

    /**
     * Returns the {@link OnClickDelegate} to be called back when the grid item is clicked or
     * {@code null} if the grid item is non-clickable.
     */
    @Nullable
    public OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    /**
     * Returns the {@link Badge} that is displayed over the grid item image or {@code null} if not
     * set.
     *
     * @see Builder#setBadge(Badge)
     */
    @ExperimentalCarApi
    @Nullable
    public Badge getBadge() {
        return mBadge;
    }

    /**
     * Returns whether this item can be included in indexed lists.
     *
     * @see Builder#setIndexable(boolean)
     */
    @ExperimentalCarApi
    public boolean isIndexable() {
        return mIndexable;
    }

    @Override
    @NonNull
    public String toString() {
        return "[title: "
                + CarText.toShortString(mTitle)
                + ", text: "
                + CarText.toShortString(mText)
                + ", image: "
                + mImage
                + ", isLoading: "
                + mIsLoading
                + ", badge: "
                + mBadge
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mIsLoading,
                mTitle,
                mImage,
                mImageType,
                mOnClickDelegate == null,
                mBadge,
                mIndexable
        );
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GridItem)) {
            return false;
        }
        GridItem otherGridItem = (GridItem) other;

        return mIsLoading == otherGridItem.mIsLoading
                && Objects.equals(mTitle, otherGridItem.mTitle)
                && Objects.equals(mText, otherGridItem.mText)
                && Objects.equals(mImage, otherGridItem.mImage)
                && Objects.equals(mOnClickDelegate == null, otherGridItem.mOnClickDelegate == null)
                && Objects.equals(mBadge, otherGridItem.mBadge)
                && mImageType == otherGridItem.mImageType
                && mIndexable == otherGridItem.mIndexable;
    }

    GridItem(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mText = builder.mText;
        mImage = builder.mImage;
        mImageType = builder.mImageType;
        mOnClickDelegate = builder.mOnClickDelegate;
        mBadge = builder.mBadge;
        mIndexable = builder.mIndexable;
    }

    /** Constructs an empty instance, used by serialization code. */
    private GridItem() {
        mIsLoading = false;
        mTitle = null;
        mText = null;
        mImage = null;
        mImageType = IMAGE_TYPE_LARGE;
        mOnClickDelegate = null;
        mBadge = null;
        mIndexable = true;
    }

    /** A builder of {@link GridItem}. */
    public static final class Builder {
        @Nullable
        CarText mTitle;
        @Nullable
        CarText mText;
        @Nullable
        CarIcon mImage;
        @GridItemImageType
        int mImageType = IMAGE_TYPE_LARGE;
        @Nullable
        OnClickDelegate mOnClickDelegate;
        boolean mIsLoading;
        @Nullable
        Badge mBadge;
        boolean mIndexable = true;

        /**
         * Sets whether the item is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where the grid item would be
         * otherwise. The caller is expected to call {@link Screen#invalidate()} and send
         * the new template content to the host once the data is ready. If set to {@code false},
         * the UI shows the item  contents.
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the title of the {@link GridItem}.
         *
         * <p>{@code title} must conform to {@link CarTextConstraints.TEXT_ONLY} in Car API 7 and
         * below, and {@link CarTextConstraints.TEXT_AND_ICON} in Car API 8 and above.
         *
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            if (title == null) {
                mTitle = null;
                return this;
            }
            CarText titleText = CarText.create(title);
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(titleText);
            mTitle = titleText;
            return this;
        }

        /**
         * Sets the title of the {@link GridItem}, with support for multiple length variants.
         *
         * <p>{@code title} must conform to {@link CarTextConstraints.TEXT_ONLY} in Car API 7 and
         * below, and {@link CarTextConstraints.TEXT_AND_ICON} in Car API 8 and above.
         *
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         */
        @NonNull
        public Builder setTitle(@Nullable CarText title) {
            if (title == null) {
                mTitle = null;
                return this;
            }
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(title);
            mTitle = title;
            return this;
        }

        /**
         * Sets a secondary text string to the grid item that is displayed below the title.
         *
         * <p>{@code text} must conform to {@link CarTextConstraints.TEXT_WITH_COLORS} in Car API
         * 7 and below, and {@link CarTextConstraints.TEXT_WITH_COLORS_AND_ICON} in Car API 8 and
         * above.
         *
         * <h2>Text Wrapping</h2>
         *
         * This text is truncated at the end to fit in a single line below the title
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @NonNull
        public Builder setText(@NonNull CharSequence text) {
            mText = CarText.create(requireNonNull(text));
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(mText);
            return this;
        }

        /**
         * Sets a secondary text string to the grid item that is displayed below the title, with
         * support for multiple length variants.
         *
         * <p>{@code text} must conform to {@link CarTextConstraints.TEXT_WITH_COLORS} in Car API
         * 7 and below, and {@link CarTextConstraints.TEXT_WITH_COLORS_AND_ICON} in Car API 8 and
         * above.
         *
         * <h2>Text Wrapping</h2>
         *
         * This text is truncated at the end to fit in a single line below the title
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @NonNull
        public Builder setText(@NonNull CarText text) {
            mText = requireNonNull(text);
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(mText);
            return this;
        }

        /**
         * Sets an image to show in the grid item with the default size {@link #IMAGE_TYPE_LARGE}.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         * @see #setImage(CarIcon, int)
         */
        @NonNull
        public Builder setImage(@NonNull CarIcon image) {
            return setImage(requireNonNull(image), IMAGE_TYPE_LARGE);
        }

        /**
         * Sets an image to show in the grid item with the given {@link Badge} to be displayed over
         * the image, with the default size {@link #IMAGE_TYPE_LARGE}.
         *
         * <p>A dot badge denotes some sort of call to action or notification and is
         * displayed in the upper right corner of the image. An icon badge gives additional
         * context about the image and is displayed in the lower right corner.
         *
         * @throws NullPointerException if {@code image} or {@code badge} is {@code null}
         * @see #setImage(CarIcon, int)
         */
        @NonNull
        @ExperimentalCarApi
        public Builder setImage(@NonNull CarIcon image, @NonNull Badge badge) {
            requireNonNull(badge);
            mBadge = badge;
            return setImage(requireNonNull(image));
        }

        /**
         * Sets an image to show in the grid item with the given {@code imageType} and given
         * {@link Badge} to be displayed over the image.
         *
         * <p>A dot badge denotes a call to action or notification and is
         * displayed in the upper right corner of the image. An icon badge gives additional
         * context about the image and is displayed in the lower right corner.
         *
         * @throws NullPointerException if {@code image} or {@code badge} is {@code null}
         * @see #setImage(CarIcon, int)
         */
        @NonNull
        @ExperimentalCarApi
        public Builder setImage(@NonNull CarIcon image, @GridItemImageType int imageType,
                @NonNull Badge badge) {
            requireNonNull(badge);
            mBadge = badge;
            return setImage(requireNonNull(image), imageType);
        }

        /**
         * Sets an image to show in the grid item with the given {@code imageType}.
         *
         * <p>For a custom {@link CarIcon}, its {@link androidx.core.graphics.drawable.IconCompat}
         * instance can be of {@link androidx.core.graphics.drawable.IconCompat#TYPE_BITMAP},
         * {@link androidx.core.graphics.drawable.IconCompat#TYPE_RESOURCE}, or
         * {@link androidx.core.graphics.drawable.IconCompat#TYPE_URI}.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * <p>If the input image's size exceeds the sizing requirements for the given image type in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @param image     the {@link CarIcon} to display
         * @param imageType one of {@link #IMAGE_TYPE_ICON} or {@link #IMAGE_TYPE_LARGE}
         * @throws NullPointerException if {@code image} is {@code null}
         */
        @NonNull
        public Builder setImage(@NonNull CarIcon image, @GridItemImageType int imageType) {
            CarIconConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(image));
            mImage = image;
            mImageType = imageType;
            return this;
        }

        /**
         * Sets the {@link OnClickListener} to be called back when the grid item is clicked, or
         * {@code null} to make the grid item non-clickable.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}
         *
         * @throws NullPointerException if {@code onClickListener} is {@code null}
         */
        @NonNull
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public Builder setOnClickListener(@NonNull OnClickListener onClickListener) {
            mOnClickDelegate = OnClickDelegateImpl.create(onClickListener);
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
         * .Builder#setAlphabeticalIndexingAllowed(Boolean)}).
         */
        @ExperimentalCarApi
        @NonNull
        public Builder setIndexable(boolean indexable) {
            mIndexable = indexable;
            return this;
        }

        /**
         * Constructs the {@link GridItem} defined by this builder.
         *
         * @throws IllegalStateException if the grid item's image is set when it is loading or vice
         *                               versa, if the grid item is loading but the click listener
         *                               is set, or if a badge is set and an image is not set
         */
        @NonNull
        public GridItem build() {
            if (mIsLoading == (mImage != null)) {
                throw new IllegalStateException(
                        "When a grid item is loading, the image must not be set and vice versa");
            }

            if (mIsLoading && mOnClickDelegate != null) {
                throw new IllegalStateException(
                        "The click listener must not be set on the grid item when it is loading");
            }

            if (mImage == null && mBadge != null) {
                throw new IllegalStateException("A badge can only be set when a grid item image "
                        + "is also provided");
            }

            return new GridItem(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
