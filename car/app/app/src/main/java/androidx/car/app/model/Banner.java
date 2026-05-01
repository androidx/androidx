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

import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.ActionsConstraints;
import androidx.car.app.model.constraints.BackgroundConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private static final int MAX_TRAILING_ELEMENTS = 2;

    private final @Nullable CarText mTitle;
    private final @Nullable CarText mSubtitle;
    private final @Nullable OnClickDelegate mOnClickDelegate;
    private final @Nullable Background mBackground;
    private final @Nullable BannerElement mLeadingElement;
    private final List<BannerElement> mTrailingElements;
    private final List<Action> mBelowActions;

    Banner(Builder builder) {
        mTitle = builder.mTitle;
        mSubtitle = builder.mSubtitle;
        mOnClickDelegate = builder.mOnClickDelegate;
        mBackground = builder.mBackground;
        mLeadingElement = builder.mLeadingElement;
        mTrailingElements = Collections.unmodifiableList(builder.mTrailingElements);
        mBelowActions = Collections.unmodifiableList(builder.mBelowActions);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Banner() {
        mTitle = null;
        mSubtitle = null;
        mOnClickDelegate = null;
        mBackground = null;
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
     * <p>The title is automatically truncated if it's too long; however, shorter variants can be
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
     * Returns the background of the banner.
     *
     * @see Builder#setBackground(Background)
     */
    public @Nullable Background getBackground() {
        return mBackground;
    }

    /**
     * Returns the leading element of the banner.
     *
     * <p>This is currently restricted to icons and images only.
     *
     * @see Builder#setLeadingImage(CarIcon)
     * @see Builder#setLeadingIcon(CarIcon)
     *
     */
    @RestrictTo(LIBRARY)
    public @Nullable BannerElement getLeadingElement() {
        return mLeadingElement;
    }

    /**
     * Returns the list of trailing elements of the banner.
     *
     * @see Builder#addTrailingAction(Action)
     * @see Builder#addTrailingIcon(CarIcon)
     * @see Builder#addTrailingImage(CarIcon)
     *
     */
    @RestrictTo(LIBRARY)
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
        return "[title: " + CarText.toShortString(mTitle) + ", subtitle: "
                + CarText.toShortString(mSubtitle) + ", has click listener: "
                + (mOnClickDelegate != null) + ", background color: "
                + mBackground + ", leading element: " + mLeadingElement
                + ", trailing elements: " + mTrailingElements + ", below actions: "
                + mBelowActions + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mSubtitle, mOnClickDelegate == null, mBackground,
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
                && Objects.equals(mBackground, otherBanner.mBackground)
                && Objects.equals(mLeadingElement, otherBanner.mLeadingElement)
                && Objects.equals(mTrailingElements, otherBanner.mTrailingElements)
                && Objects.equals(mBelowActions, otherBanner.mBelowActions);
    }

    /** A builder of {@link Banner}. */
    public static final class Builder {
        @Nullable CarText mTitle;
        @Nullable CarText mSubtitle;
        @Nullable OnClickDelegate mOnClickDelegate;
        @Nullable Background mBackground;
        @Nullable BannerElement mLeadingElement;
        List<BannerElement> mTrailingElements = new ArrayList<>();
        List<Action> mBelowActions = new ArrayList<>();

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

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
         * Sets the {@link Background} of the banner.
         *
         * <p>The {@code background} must conform to {@link BackgroundConstraints#COLOR_ONLY}.
         *
         * @throws NullPointerException     if {@code background} is {@code null}
         * @throws IllegalArgumentException if an unsupported background is added
         */
        public @NonNull Builder setBackground(@NonNull Background background) {
            BackgroundConstraints.COLOR_ONLY.validateOrThrow(requireNonNull(background));
            mBackground = background;
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
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setLeadingIcon(@NonNull CarIcon icon) {
            BannerElement element =
                    new BannerElement(
                            BannerElement.TYPE_ICON, /* action= */ null, requireNonNull(icon));
            mLeadingElement = element;
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
            BannerElement element =
                    new BannerElement(
                            BannerElement.TYPE_IMAGE, /* action= */ null, requireNonNull(image));
            mLeadingElement = element;
            return this;
        }

        /**
         * Adds an {@link Action} to the trailing part of the banner.
         *
         * <p>A banner can have at most 2 trailing elements
         *
         * <p>{@code action} must conform to
         * {@link ActionsConstraints#ACTION_CONSTRAINTS_BANNER_TRAILING}.
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addTrailingAction(@NonNull Action action) {
            BannerElement element =
                    new BannerElement(
                            BannerElement.TYPE_ACTION, requireNonNull(action), /* icon= */ null);
            validateNewTrailingElement(element);
            mTrailingElements.add(element);
            return this;
        }

        /**
         * Adds a {@link CarIcon} to be displayed as an icon to the trailing part of the banner.
         *
         * <p>A banner can have at most 2 trailing elements
         *
         * @throws NullPointerException     if {@code icon} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         */
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
         * <p>A banner can have at most 2 trailing elements
         *
         * @throws NullPointerException     if {@code image} is {@code null}
         * @throws IllegalArgumentException if there are already 2 trailing elements
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addTrailingImage(@NonNull CarIcon image) {
            BannerElement element =
                    new BannerElement(
                            BannerElement.TYPE_IMAGE, /* action= */ null, requireNonNull(image));
            validateNewTrailingElement(element);
            mTrailingElements.add(element);
            return this;
        }

        /**
         * Adds an {@link Action} below the title and subtitle of the {@link Banner}.
         *
         * <p>A {@link Banner}'s below actions must conform to
         * {@link ActionsConstraints#ACTION_CONSTRAINTS_BANNER_BELOW}
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} does not conform to
         * {@link ActionsConstraints#ACTION_CONSTRAINTS_BANNER_BELOW}
         */
        public @NonNull Builder addBelowAction(@NonNull Action action) {
            List<Action> actionsCopy = new ArrayList<>(mBelowActions);
            actionsCopy.add(requireNonNull(action));
            ActionsConstraints.ACTION_CONSTRAINTS_BANNER_BELOW.validateOrThrow(actionsCopy);
            mBelowActions.add(requireNonNull(action));
            return this;
        }

        /**
         * Constructs the {@link Banner} defined by this builder.
         *
         * @throws IllegalStateException if the title is null or empty
         * @throws IllegalStateException if there are more than 4 elements across the banner's
         *                               leading and trailing elements lists OR more than 2 of
         *                               these elements are {@link Action}s
         */
        public @NonNull Banner build() {
            if (CarText.isNullOrEmpty(mTitle)) {
                throw new IllegalArgumentException("A title must be provided");
            }

            return new Banner(this);
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
