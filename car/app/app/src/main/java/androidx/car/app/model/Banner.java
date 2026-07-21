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
import androidx.car.app.model.constraints.ActionsConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A banner element that's meant to be visually distinct from other normal elements on the screen.
 *
 * <p>For example, a banner can be used to tell the user whether the app is running in an online vs
 * offline mode, or promote a specific event or announcement.
 */
@ExperimentalCarApi
@CarProtocol
@KeepFields
@RequiresCarApi(9)
public final class Banner implements Item {
    @RestrictTo(LIBRARY)
    @IntDef(value = {IMAGE_TYPE_ICON, IMAGE_TYPE_SMALL, IMAGE_TYPE_LARGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BannerImageType {
    }

    /**
     * Represents an icon to be displayed in the banner.
     *
     * <p>A tint color is expected to be provided via {@link CarIcon.Builder#setTint}. Otherwise, a
     * default tint color as determined by the host will be applied.
     */
    public static final int IMAGE_TYPE_ICON = 1;

    /**
     * Represents a small image to be displayed in the banner.
     *
     * <p>The host renders it with standard padding and scales the image to fit within the bounds.
     */
    public static final int IMAGE_TYPE_SMALL = 2;

    /**
     * Represents a large edge-to-edge image to be displayed in the banner. Scales the image
     * to fill the container and potentially clip within the bounds if a shape is applied.
     *
     * <p>This image type cannot be used in combination with {@link Builder#addBelowAction(Action)}.
     */
    public static final int IMAGE_TYPE_LARGE = 3;

    private static final int MAX_TRAILING_ELEMENTS = 2;

    private final @Nullable CarText mTitle;
    private final @Nullable CarText mSubtitle;
    private final @Nullable OnClickDelegate mOnClickDelegate;
    private final @Nullable BannerStyle mStyle;
    private final @Nullable BannerElement mLeadingElement;
    private final List<BannerElement> mTrailingElements;
    private final List<Action> mBelowActions;

    Banner(Builder builder) {
        mTitle = builder.mTitle;
        mSubtitle = builder.mSubtitle;
        mOnClickDelegate = builder.mOnClickDelegate;
        mStyle = builder.mStyle;
        mLeadingElement = builder.mLeadingElement;
        mTrailingElements = Collections.unmodifiableList(builder.mTrailingElements);
        mBelowActions = Collections.unmodifiableList(builder.mBelowActions);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Banner() {
        mTitle = null;
        mSubtitle = null;
        mOnClickDelegate = null;
        mStyle = null;
        mLeadingElement = null;
        mTrailingElements = Collections.emptyList();
        mBelowActions = Collections.emptyList();
    }

    /**
     * Returns the title of the banner.
     *
     * <p>The title is automatically truncated if it's too long; however, shorter variants can be
     * added via {@link CarText.Builder#addVariant(CharSequence)}.
     *
     * @see Builder#setTitle(CharSequence)
     */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the subtitle of the banner.
     *
     * <p>The subtitle is automatically truncated if it's too long; however, shorter variants can be
     * added via {@link CarText.Builder#addVariant(CharSequence)}.
     *
     * @see Builder#setSubtitle(CharSequence)
     */
    public @Nullable CarText getSubtitle() {
        return mSubtitle;
    }

    /**
     * Returns the {@link OnClickDelegate} to be called back when the banner is clicked.
     *
     * @see Builder#setOnClickListener(OnClickListener)
     */
    public @Nullable OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    /**
     * Returns the {@link BannerStyle} of the banner, or {@code null} if not set.
     *
     * @see Builder#setStyle(BannerStyle)
     */
    public @Nullable BannerStyle getStyle() {
        return mStyle;
    }

    /**
     * Returns the leading element of the banner.
     *
     * <p>This is currently restricted to icons and images only.
     *
     * @see Builder#setLeadingImage(CarIcon)
     * @see Builder#setLeadingIcon(CarIcon)
     */
    public @Nullable BannerElement getLeadingElement() {
        return mLeadingElement;
    }

    /**
     * Returns the list of trailing elements of the banner.
     *
     * @see Builder#addTrailingAction(Action)
     * @see Builder#addTrailingIcon(CarIcon)
     * @see Builder#addTrailingImage(CarIcon)
     */
    public @NonNull List<BannerElement> getTrailingElements() {
        return mTrailingElements;
    }

    /**
     * Returns the list of actions below the title and subtitle.
     *
     * @see Builder#addBelowAction(Action)
     */
    public @NonNull List<Action> getBelowActions() {
        return mBelowActions;
    }

    @Override
    public @NonNull String toString() {
        return "Banner { title: " + CarText.toShortString(mTitle) + ", subtitle: "
                + CarText.toShortString(mSubtitle) + ", has click listener: "
                + (mOnClickDelegate != null) + ", style: "
                + mStyle + ", leading element: " + mLeadingElement
                + ", trailing elements: " + mTrailingElements + ", below actions: "
                + mBelowActions + " }";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mSubtitle, mOnClickDelegate == null, mStyle,
                mLeadingElement, mTrailingElements, mBelowActions);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Banner)) {
            return false;
        }
        Banner otherBanner = (Banner) other;

        // Don't compare listener, only the fact whether it's present.
        return Objects.equals(mTitle, otherBanner.mTitle)
                && Objects.equals(mSubtitle, otherBanner.mSubtitle)
                && Objects.equals(mOnClickDelegate == null, otherBanner.mOnClickDelegate == null)
                && Objects.equals(mStyle, otherBanner.mStyle)
                && Objects.equals(mLeadingElement, otherBanner.mLeadingElement)
                && Objects.equals(mTrailingElements, otherBanner.mTrailingElements)
                && Objects.equals(mBelowActions, otherBanner.mBelowActions);
    }

    /** A builder of {@link Banner}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder {
        @Nullable CarText mTitle;
        @Nullable CarText mSubtitle;
        @Nullable OnClickDelegate mOnClickDelegate;
        @Nullable BannerStyle mStyle;
        @Nullable BannerElement mLeadingElement;
        List<BannerElement> mTrailingElements = new ArrayList<>();
        List<Action> mBelowActions = new ArrayList<>();

        /**
         * Sets the title of the banner.
         *
         * <p>The title allows {@link CarTextConstraints#TEXT_AND_ICON} spans to be added.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if any unsupported span types are added
         */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            setTitle(CarText.create(requireNonNull(title)));
            return this;
        }

        /**
         * Sets the title of the banner.
         *
         * <p>The title allows {@link CarTextConstraints#TEXT_AND_ICON} spans to be added.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if any unsupported span types are added
         */
        public @NonNull Builder setTitle(@NonNull CarText title) {
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(requireNonNull(title));
            mTitle = title;
            return this;
        }

        /**
         * Sets the subtitle of the banner.
         *
         * <p>The subtitle allows {@link CarTextConstraints#TEXT_WITH_COLORS_AND_ICON} spans to be
         * added.
         *
         * <p>The subtitle is automatically truncated if it's too long.
         *
         * @throws NullPointerException     if {@code subtitle} is {@code null}
         * @throws IllegalArgumentException if any unsupported span types are added
         */
        public @NonNull Builder setSubtitle(@NonNull CharSequence subtitle) {
            setSubtitle(CarText.create(requireNonNull(subtitle)));
            return this;
        }

        /**
         * Sets the subtitle of the banner.
         *
         * <p>The subtitle allows {@link CarTextConstraints#TEXT_WITH_COLORS_AND_ICON} spans to be
         * added.
         *
         * <p>The subtitle is automatically truncated if it's too long.
         *
         * @throws NullPointerException     if {@code subtitle} is {@code null}
         * @throws IllegalArgumentException if any unsupported span types are added
         */
        public @NonNull Builder setSubtitle(@NonNull CarText subtitle) {
            CarTextConstraints.TEXT_WITH_COLORS_AND_ICON.validateOrThrow(requireNonNull(subtitle));
            mSubtitle = subtitle;
            return this;
        }

        /**
         * Sets the {@link OnClickListener} to be called back when the banner is clicked.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link android.os.Looper#getMainLooper()}.
         *
         * @throws NullPointerException if {@code onClickListener} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public @NonNull Builder setOnClickListener(@NonNull OnClickListener onClickListener) {
            mOnClickDelegate = OnClickDelegateImpl.create(requireNonNull(onClickListener));
            return this;
        }

        /**
         * Sets the {@link BannerStyle} of the banner.
         *
         * <p>If a style is not provided via this method, a host default style will be used.
         *
         * @throws NullPointerException if {@code style} is {@code null}
         */
        public @NonNull Builder setStyle(@NonNull BannerStyle style) {
            mStyle = requireNonNull(style);
            return this;
        }

        /**
         * Sets the leading element in this banner to be a {@link CarIcon} displayed as an icon.
         *
         * <p>Only a single leading icon or image can be set, so this will overwrite calls to
         * {@link #setLeadingImage(CarIcon)}.
         *
         * <p>This is visually distinct from {@link #setLeadingImage(CarIcon)} as icons are smaller
         * due to added padding, and are expected to be tinted.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         * @deprecated Use {@link #setLeadingImage(CarIcon, int)} with {@link #IMAGE_TYPE_ICON}
         */
        @Deprecated
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setLeadingIcon(@NonNull CarIcon icon) {
            mLeadingElement = new BannerElement(
                    BannerElement.TYPE_ICON, /* action= */ null, requireNonNull(icon));
            return this;
        }

        /**
         * Sets the leading element in this banner to be a {@link CarIcon} displayed as an image.
         *
         * <p>Only a single leading icon or image can be set, so this will overwrite calls to
         * {@link #setLeadingIcon(CarIcon)}.
         *
         * <p>This is visually distinct from {@link #setLeadingIcon(CarIcon)} as images have no
         * added padding, and are not expected to be tinted.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setLeadingImage(@NonNull CarIcon image) {
            return setLeadingImage(requireNonNull(image), IMAGE_TYPE_SMALL);
        }

        /**
         * Sets the leading element in this banner to be a {@link CarIcon} displayed as an image.
         *
         * <p>Only a single leading icon or image can be set, so this will overwrite calls to
         * {@link #setLeadingIcon(CarIcon)}.
         *
         * <p>A large image cannot be used in combination with
         * {@link Builder#addBelowAction(Action)}.
         *
         * @param image     the {@link CarIcon} for the leading image
         * @param imageType the {@link BannerImageType} for the leading image
         * @throws NullPointerException if {@code image} is {@code null}
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setLeadingImage(@NonNull CarIcon image,
                @BannerImageType int imageType) {
            mLeadingElement = BannerElement.createForImageType(requireNonNull(image), imageType);
            return this;
        }

        /**
         * Adds an {@link Action} to the trailing part of the banner.
         *
         * <p>A banner can have at most 2 trailing elements.
         *
         * <p>{@code action} must conform to
         * {@link ActionsConstraints#ACTION_CONSTRAINTS_BANNER_TRAILING}.
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addTrailingAction(@NonNull Action action) {
            BannerElement element = BannerElement.createForAction(requireNonNull(action));
            validateNewTrailingElement(element);
            mTrailingElements.add(element);
            return this;
        }

        /**
         * Adds a {@link CarIcon} to be displayed as an icon to the trailing part of the banner.
         *
         * <p>A banner can have at most 2 trailing elements.
         *
         * @throws NullPointerException     if {@code icon} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         * @deprecated Use {@link #addTrailingImage(CarIcon, int)} with {@link #IMAGE_TYPE_ICON}
         */
        @Deprecated
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addTrailingIcon(@NonNull CarIcon icon) {
            BannerElement element =
                    new BannerElement(
                            BannerElement.TYPE_ICON, /* action= */ null, requireNonNull(icon));
            validateNewTrailingElement(element);
            mTrailingElements.add(element);
            return this;
        }

        /**
         * Adds a {@link CarIcon} to be displayed as an image to the trailing part of the banner.
         *
         * <p>A banner can have at most 2 trailing elements.
         *
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addTrailingImage(@NonNull CarIcon image) {
            return addTrailingImage(requireNonNull(image), IMAGE_TYPE_SMALL);
        }

        /**
         * Adds a {@link CarIcon} to be displayed as an image to the trailing part of the banner.
         *
         * <p>A banner can have at most 2 trailing elements.
         *
         * <p>A large image cannot be used in combination with
         * {@link Builder#addBelowAction(Action)}.
         *
         * <p>A large trailing image cannot be used in combination with
         * another large trailing image.
         *
         * @param image     the {@link CarIcon} for the trailing image
         * @param imageType the {@link BannerImageType} for the trailing image
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         * @throws IllegalArgumentException if a large trailing image is combined with anything
         *                                  other than a trailing icon or action
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addTrailingImage(@NonNull CarIcon image,
                @BannerImageType int imageType) {
            BannerElement element = BannerElement.createForImageType(
                    requireNonNull(image), imageType);
            validateNewTrailingElement(element);
            mTrailingElements.add(element);
            return this;
        }

        /**
         * Adds an {@link Action} below the title and subtitle of the {@link Banner}.
         *
         * <p>A {@link Banner}'s below actions must conform to
         * {@link ActionsConstraints#ACTION_CONSTRAINTS_BANNER_BELOW}.
         *
         * <p>Below actions cannot be used in combination with a large image.
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} does not conform to
         * {@link ActionsConstraints#ACTION_CONSTRAINTS_BANNER_BELOW}
         */
        public @NonNull Builder addBelowAction(@NonNull Action action) {
            List<Action> actionsCopy = new ArrayList<>(mBelowActions);
            actionsCopy.add(requireNonNull(action));
            ActionsConstraints.ACTION_CONSTRAINTS_BANNER_BELOW.validateOrThrow(actionsCopy);
            mBelowActions.add(action);
            return this;
        }

        /**
         * Constructs the {@link Banner} defined by this builder.
         *
         * @throws IllegalArgumentException if the title is null or empty
         * @throws IllegalArgumentException if below actions are used in combination with a large
         *                                  image
         * @throws IllegalStateException    if there are more than 4 elements across the banner's
         *                                  leading and trailing elements lists OR more than 2 of
         *                                  these elements are {@link Action}s
         */
        public @NonNull Banner build() {
            if (CarText.isNullOrEmpty(mTitle)) {
                throw new IllegalArgumentException("A title must be provided");
            }

            if (!mBelowActions.isEmpty() && hasLargeImage()) {
                throw new IllegalArgumentException(
                        "Below actions cannot be combined with a large image");
            }

            return new Banner(this);
        }

        private boolean hasLargeImage() {
            return isLargeImage(mLeadingElement) || getLargeImageCount(mTrailingElements) > 0;
        }

        private boolean isLargeImage(@Nullable BannerElement element) {
            return element != null
                    && element.getType() == BannerElement.TYPE_IMAGE
                    && element.getImageType() == IMAGE_TYPE_LARGE;
        }

        private int getLargeImageCount(List<BannerElement> elements) {
            int count = 0;
            for (BannerElement element : elements) {
                if (isLargeImage(element)) {
                    count++;
                }
            }
            return count;
        }

        private boolean hasSmallImage(List<BannerElement> elements) {
            for (BannerElement element : elements) {
                if (element.getType() == BannerElement.TYPE_IMAGE
                        && element.getImageType() == IMAGE_TYPE_SMALL) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Validates that the banner's trailing + {@code newElement} list conforms to
         * constraints.
         *
         * @param newElement the new element to add to the trailing area
         */
        private void validateNewTrailingElement(@NonNull BannerElement newElement) {
            // Validate max elements
            List<BannerElement> allElements = new ArrayList<>(mTrailingElements);
            allElements.add(newElement);

            if (allElements.size() > MAX_TRAILING_ELEMENTS) {
                throw new IllegalStateException(
                        "Total number of trailing elements in a banner must not exceed "
                                + MAX_TRAILING_ELEMENTS + ", found " + mTrailingElements.size());
            }

            int largeImageCount = getLargeImageCount(allElements);
            if (largeImageCount >= 2) {
                throw new IllegalArgumentException(
                        "Too many large images, only one large image can be set at a time");
            }
            if (largeImageCount > 0 && hasSmallImage(allElements)) {
                throw new IllegalArgumentException(
                        "A large trailing image can only be combined with a trailing icon"
                                + " or action");
            }

            // Validate actions
            List<Action> allActions = new ArrayList<>();
            for (BannerElement element : allElements) {
                if (element.getType() != BannerElement.TYPE_ACTION) {
                    continue;
                }
                allActions.add(requireNonNull(element.getAction()));
            }
            ActionsConstraints.ACTION_CONSTRAINTS_BANNER_TRAILING.validateOrThrow(
                    allActions);
        }
    }
}