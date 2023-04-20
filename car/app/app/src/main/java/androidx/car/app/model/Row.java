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
import static androidx.car.app.model.Metadata.EMPTY_METADATA;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.ActionsConstraints;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.utils.CollectionUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a row with a title, several lines of text, an optional image, and an optional action
 * or switch.
 */
@CarProtocol
@KeepFields
public final class Row implements Item {
    /** A boat that belongs to you. */
    private static final String YOUR_BOAT = "\uD83D\uDEA3"; // 🚣

    /** An integer value indicating no decoration should be shown. */
    public static final int NO_DECORATION = -1;

    /**
     * The type of images supported within rows.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef(value = {IMAGE_TYPE_SMALL, IMAGE_TYPE_ICON, IMAGE_TYPE_LARGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RowImageType {
    }

    /**
     * Represents a small image to be displayed in the row.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * images targeting a 88 x 88 dp bounding box. If necessary, the image will be scaled down while
     * preserving its aspect ratio.
     */
    public static final int IMAGE_TYPE_SMALL = (1 << 0);

    /**
     * Represents a large image to be displayed in the row.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * images targeting a 224 x 224 dp bounding box. If necessary, the image will be scaled down
     * while preserving its aspect ratio.
     */
    public static final int IMAGE_TYPE_LARGE = (1 << 1);

    /**
     * Represents a small image to be displayed in the row.
     *
     * <p>To minimize scaling artifacts across a wide range of car screens, apps should provide
     * images targeting a 88 x 88 dp bounding box. If necessary, the icon will be scaled down while
     * preserving its aspect ratio.
     *
     * <p>A tint color is expected to be provided via {@link CarIcon.Builder#setTint}. Otherwise, a
     * default tint color as determined by the host will be applied.
     */
    public static final int IMAGE_TYPE_ICON = (1 << 2);

    private final boolean mIsEnabled;
    @Nullable
    private final CarText mTitle;
    private final List<CarText> mTexts;
    @Nullable
    private final CarIcon mImage;
    private final List<Action> mActions;
    private final int mNumericDecoration;
    @Nullable
    private final Toggle mToggle;
    @Nullable
    private final OnClickDelegate mOnClickDelegate;
    private final Metadata mMetadata;
    private final boolean mIsBrowsable;
    @RowImageType
    private final int mRowImageType;

    /**
     * Returns the title of the row or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the list of text below the title.
     *
     * @see Builder#addText(CharSequence)
     */
    @NonNull
    public List<CarText> getTexts() {
        return CollectionUtils.emptyIfNull(mTexts);
    }

    /**
     * Returns the image to display in the row or {@code null} if the row does not contain an
     * image.
     *
     * @see Builder#setImage(CarIcon)
     * @see Builder#setImage(CarIcon, int)
     */
    @Nullable
    public CarIcon getImage() {
        return mImage;
    }

    /**
     * Returns the list of additional actions.
     *
     * <p> Actions are displayed at the end of the row.
     *
     * @see Builder#addAction(Action)
     */
    @NonNull
    @RequiresCarApi(6)
    public List<Action> getActions() {
        return mActions;
    }

    /** Returns the type of the image in the row. */
    @RowImageType
    public int getRowImageType() {
        return mRowImageType;
    }

    /**
     * Returns the numeric decoration.
     *
     * <p> Numeric decorations are displayed at the end of the row, but before any actions.
     *
     * <p> Numeric decorations are only allowed in full-width lists.
     *
     * <p> {@link Row#NO_DECORATION} will be returned if the row does not contain a decoration.
     *
     * @see Builder#setNumericDecoration(int)
     */
    @RequiresCarApi(6)
    public int getNumericDecoration() {
        return mNumericDecoration;
    }

    /**
     * Returns the {@link Toggle} in the row or {@code null} if the row does not contain a
     * toggle.
     *
     * @see Builder#setToggle(Toggle)
     */
    @Nullable
    public Toggle getToggle() {
        return mToggle;
    }

    /**
     * Returns whether the row is browsable.
     *
     * <p>If a row is browsable, then no {@link Action} or {@link Toggle} can be added to it.
     *
     * @see Builder#isBrowsable()
     */
    public boolean isBrowsable() {
        return mIsBrowsable;
    }

    /**
     * Returns the {@link OnClickListener} to be called back when the row is clicked or {@code
     * null} if the row is non-clickable.
     */
    @Nullable
    public OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    /**
     * Returns the {@link Metadata} associated with the row or {@code null} if there is no
     * metadata associated with the row.
     */
    @Nullable
    public Metadata getMetadata() {
        return mMetadata;
    }

    /**
     * Rows your boat.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * row.row().row().yourBoat(); // gently down the stream
     * }</pre>
     */
    @NonNull
    public CharSequence yourBoat() {
        return YOUR_BOAT;
    }

    /** Returns a {@link Row} for rowing {@link #yourBoat()} */
    @NonNull
    public Row row() {
        return this;
    }

    /**
     * Returns {@code true} if the row is enabled.
     */
    @RequiresCarApi(5)
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    @NonNull
    public String toString() {
        return "[title: "
                + CarText.toShortString(mTitle)
                + ", text count: "
                + (mTexts != null ? mTexts.size() : 0)
                + ", image: "
                + mImage
                + ", isBrowsable: "
                + mIsBrowsable
                + ", isEnabled: "
                + mIsEnabled
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mTitle,
                mTexts,
                mImage,
                mToggle,
                mOnClickDelegate == null,
                mMetadata,
                mIsBrowsable,
                mRowImageType,
                mIsEnabled);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Row)) {
            return false;
        }
        Row otherRow = (Row) other;

        // Don't compare listener, only the fact whether it's present.
        return Objects.equals(mTitle, otherRow.mTitle)
                && Objects.equals(mTexts, otherRow.mTexts)
                && Objects.equals(mImage, otherRow.mImage)
                && Objects.equals(mToggle, otherRow.mToggle)
                && Objects.equals(mOnClickDelegate == null, otherRow.mOnClickDelegate == null)
                && Objects.equals(mMetadata, otherRow.mMetadata)
                && mIsBrowsable == otherRow.mIsBrowsable
                && mRowImageType == otherRow.mRowImageType
                && mIsEnabled == otherRow.isEnabled();
    }

    Row(Builder builder) {
        mTitle = builder.mTitle;
        mTexts = CollectionUtils.unmodifiableCopy(builder.mTexts);
        mImage = builder.mImage;
        mActions = CollectionUtils.unmodifiableCopy(builder.mActions);
        mNumericDecoration = builder.mDecoration;
        mToggle = builder.mToggle;
        mOnClickDelegate = builder.mOnClickDelegate;
        mMetadata = builder.mMetadata;
        mIsBrowsable = builder.mIsBrowsable;
        mRowImageType = builder.mRowImageType;
        mIsEnabled = builder.mIsEnabled;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Row() {
        mTitle = null;
        mTexts = Collections.emptyList();
        mImage = null;
        mActions = Collections.emptyList();
        mNumericDecoration = NO_DECORATION;
        mToggle = null;
        mOnClickDelegate = null;
        mMetadata = EMPTY_METADATA;
        mIsBrowsable = false;
        mRowImageType = IMAGE_TYPE_SMALL;
        mIsEnabled = true;
    }

    /** A builder of {@link Row}. */
    public static final class Builder {
        boolean mIsEnabled = true;
        @Nullable
        CarText mTitle;
        final List<CarText> mTexts = new ArrayList<>();
        @Nullable
        CarIcon mImage;
        final List<Action> mActions = new ArrayList<>();
        int mDecoration = Row.NO_DECORATION;
        @Nullable
        Toggle mToggle;
        @Nullable
        OnClickDelegate mOnClickDelegate;
        Metadata mMetadata = EMPTY_METADATA;
        boolean mIsBrowsable;
        @RowImageType
        int mRowImageType = IMAGE_TYPE_SMALL;

        /**
         * Sets the title of the row.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} is empty, of if it contains
         *                                  unsupported spans
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            CarText titleText = CarText.create(requireNonNull(title));
            if (titleText.isEmpty()) {
                throw new IllegalArgumentException("The title cannot be null or empty");
            }
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(titleText);
            mTitle = titleText;
            return this;
        }

        /**
         * Sets the title of the row, with support for multiple length variants.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} is empty, of if it contains
         *                                  unsupported spans
         */
        @NonNull
        public Builder setTitle(@NonNull CarText title) {
            if (requireNonNull(title).isEmpty()) {
                throw new IllegalArgumentException("The title cannot be null or empty");
            }
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(title);
            mTitle = title;
            return this;
        }

        /**
         * Adds a text string to the row below the title.
         *
         * <p>The text can be customized with {@link ForegroundCarColorSpan},
         * {@link androidx.car.app.model.DistanceSpan}, and
         * {@link androidx.car.app.model.DurationSpan} instances, any other spans will be ignored
         * by the host.
         *
         * <p>Most templates allow up to 2 text strings, but this may vary. This limit is
         * documented in each individual template.
         *
         * <h4>Text Wrapping</h4>
         *
         * Each string added with this method will not wrap more than 1 line in the UI, with
         * one exception: if the template allows a maximum number of text strings larger than 1, and
         * the app adds a single text string, then this string will wrap up to the maximum.
         *
         * <p>For example, assuming 2 lines are allowed in the template where the row will be
         * used, this code:
         *
         * <pre>{@code
         * rowBuilder
         *     .addText("This is a rather long line of text")
         *     .addText("More text")
         * }</pre>
         *
         * <p>would wrap the text like this:
         *
         * <pre>
         * This is a rather long li...
         * More text
         * </pre>
         *
         * In contrast, this code:
         *
         * <pre>{@code
         * rowBuilder
         *     .addText("This is a rather long line of text. More text")
         * }</pre>
         *
         * <p>would wrap the single line of text at a maximum of 2 lines, producing a different
         * result:
         *
         * <pre>
         * This is a rather long line
         * of text. More text
         * </pre>
         *
         * <p>Note that when using a single line, a line break character can be used to break it
         * into two, but the results may be unpredictable depending on the width the text is
         * wrapped at:
         *
         * <pre>{@code
         * rowBuilder
         *     .addText("This is a rather long line of text\nMore text")
         * }</pre>
         *
         * <p>would produce a result that may loose the "More text" string:
         *
         * <pre>
         * This is a rather long line
         * of text
         * </pre>
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         * @see ForegroundCarColorSpan
         */
        @NonNull
        public Builder addText(@NonNull CharSequence text) {
            CarText carText = CarText.create(requireNonNull(text));
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(carText);
            mTexts.add(CarText.create(requireNonNull(text)));
            return this;
        }

        /**
         * Adds a text string to the row below the title, with support for multiple length variants.
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         * @see Builder#addText(CharSequence)
         */
        @NonNull
        public Builder addText(@NonNull CarText text) {
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(requireNonNull(text));
            mTexts.add(text);
            return this;
        }

        /**
         * Sets an image to show in the row with the default size {@link #IMAGE_TYPE_SMALL}.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         * @see #setImage(CarIcon, int)
         */
        @NonNull
        public Builder setImage(@NonNull CarIcon image) {
            return setImage(requireNonNull(image), IMAGE_TYPE_SMALL);
        }

        /**
         * Sets an image to show in the row with the given image type.
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
         * @param image     the {@link CarIcon} to display or {@code null} to not display one
         * @param imageType one of {@link #IMAGE_TYPE_ICON}, {@link #IMAGE_TYPE_SMALL} or {@link
         *                  #IMAGE_TYPE_LARGE}
         * @throws NullPointerException if {@code image} is {@code null}
         */
        @NonNull
        public Builder setImage(@NonNull CarIcon image, @RowImageType int imageType) {
            CarIconConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(image));
            mImage = image;
            mRowImageType = imageType;
            return this;
        }

        /**
         * Adds an additional action to the end of the row.
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} contains unsupported Action types,
         *                                  exceeds the maximum number of allowed actions or does
         *                                  not contain a valid {@link CarIcon}.
         */
        //TODO(b/260557014): Update docs when half-list UX is defined
        @NonNull
        @RequiresCarApi(6)
        public Builder addAction(@NonNull Action action) {
            List<Action> mActionsCopy = new ArrayList<>(mActions);
            mActionsCopy.add(requireNonNull(action));
            ActionsConstraints.ACTIONS_CONSTRAINTS_ROW.validateOrThrow(mActionsCopy);
            mActions.add(action);
            return this;
        }

        /**
         * Sets a numeric decoration to display in the row.
         *
         * <p> Numeric decorations are displayed at the end of the row, but before any actions.
         * Numeric decorations are not displayed in half-list templates.
         *
         * <p> Numeric decorations typically represent a quantity of unseen content. For example, a
         * decoration might represent a number of missed notifications, or a number of unread
         * messages in a conversation.
         *
         * @param decoration the {@code int} to display. Must be positive, zero, or equal to
         *                   {@link Row#NO_DECORATION}.
         * @throws IllegalArgumentException if {@code decoration} is invalid
         */
        //TODO(b/260557014): Update docs when half-list UX is defined
        @NonNull
        @RequiresCarApi(6)
        @IntRange(from = 0)
        public Builder setNumericDecoration(int decoration) {
            if (decoration < 0 && decoration != NO_DECORATION) {
                throw new IllegalArgumentException(
                        String.format(
                                "Decoration should be positive, zero, or equal to NO_DECORATION. "
                                        + "Instead, was %d",
                                decoration
                        )
                );
            }

            mDecoration = decoration;
            return this;
        }

        /**
         * Sets a {@link Toggle} to show in the row.
         *
         * <p> If a row has a toggle set, then no {@link Action}, {@link OnClickListener} or
         * numeric decoration can be set.
         *
         * @throws NullPointerException if {@code toggle} is {@code null}
         */
        @NonNull
        public Builder setToggle(@NonNull Toggle toggle) {
            mToggle = requireNonNull(toggle);
            return this;
        }

        /**
         * Shows an icon at the end of the row that indicates that the row is browsable.
         *
         * <p>Browsable rows can be used, for example, to represent the parent row in a hierarchy of
         * lists with child lists.
         *
         * <p>If a row is browsable, then no {@link Action} or {@link Toggle} can be added to it. A
         * browsable row must have an OnClickListener set.
         */
        @NonNull
        public Builder setBrowsable(boolean isBrowsable) {
            mIsBrowsable = isBrowsable;
            return this;
        }

        /**
         * Sets the {@link OnClickListener} to be called back when the row is clicked.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
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
         * Sets the {@link Metadata} associated with the row.
         *
         * @param metadata The metadata to set with the row. Pass {@link Metadata#EMPTY_METADATA}
         *                 to not associate any metadata with the row
         */
        @NonNull
        public Builder setMetadata(@NonNull Metadata metadata) {
            mMetadata = metadata;
            return this;
        }

        /**
         * Sets the initial enabled state for {@link Row}.
         *
         * <p>The default state of a {@link Row} is enabled.
         */
        @NonNull
        @RequiresCarApi(5)
        public Builder setEnabled(boolean enabled) {
            mIsEnabled = enabled;
            return this;
        }

        /**
         * Constructs the {@link Row} defined by this builder.
         *
         * @throws IllegalStateException if the row's title is not set or if the row is not set
         *                               correctly. See {@link #setToggle} and
         *                               {@link #setBrowsable}.
         */
        @NonNull
        public Row build() {
            if (mTitle == null) {
                throw new IllegalStateException("A title must be set on the row");
            }

            if (mIsBrowsable) {
                if (mToggle != null) {
                    throw new IllegalStateException("A browsable row must not have a toggle set");
                }
                if (mOnClickDelegate == null) {
                    throw new IllegalStateException(
                            "A browsable row must have its onClickListener set");
                }
                if (!mActions.isEmpty()) {
                    throw new IllegalStateException("A browsable row must not have a secondary "
                            + "action set");
                }

            }

            if (mToggle != null) {
                if (mOnClickDelegate != null) {
                    throw new IllegalStateException(
                            "If a row contains a toggle, it must not have an onClickListener set");
                }

                if (mDecoration != NO_DECORATION) {
                    throw new IllegalStateException("If a row contains a toggle, it must not have"
                            + " a numeric decoration set");
                }

                if (!mActions.isEmpty()) {
                    throw new IllegalStateException("If a row contains a toggle, it must not have "
                            + "a secondary action set");
                }
            }

            return new Row(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}