/*
 * Copyright 2022 The Android Open Source Project
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

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;

import java.util.Objects;

/**
 * Represents a tab with a title and an image. {@link Tab} instances are used by TabTemplate to
 * display tab headers.
 */
@CarProtocol
@ExperimentalCarApi
@RequiresCarApi(6)
@KeepFields
public final class Tab implements Content {
    /** Content ID for an empty Tab object. */
    private static final String EMPTY_TAB_CONTENT_ID = "EMPTY_TAB_CONTENT_ID";

    private final boolean mIsActive;
    @Nullable
    private final CarText mTitle;
    @Nullable
    private final CarIcon mIcon;
    @NonNull
    private final String mContentId;

    /**
     * Returns the title of the tab.
     *
     * @see Tab.Builder#setTitle(CharSequence)
     */
    @NonNull
    public CarText getTitle() {
        return requireNonNull(mTitle);
    }

    /**
     * Returns the content ID associated with the tab.
     *
     * @see Tab.Builder#setContentId(String)
     */
    @NonNull
    @Override
    public String getContentId() {
        return requireNonNull(mContentId);
    }

    /**
     * Returns the image to display in the tab
     *
     * @see Tab.Builder#setIcon(CarIcon)
     */
    @NonNull
    public CarIcon getIcon() {
        return requireNonNull(mIcon);
    }

    /**
     * Indicates if this is the currently active tab.
     *
     * @see Tab.Builder#setActive(boolean)
     * @deprecated use {@link TabTemplate#getActiveTabContentId()} instead.
     */
    @Deprecated
    public boolean isActive() {
        return mIsActive;
    }

    @Override
    @NonNull
    public String toString() {
        return "[title: "
                + CarText.toShortString(mTitle)
                + ", contentId: "
                + mContentId
                + ", icon: "
                + mIcon
                + ", isActive "
                + mIsActive
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mTitle,
                mContentId,
                mIcon,
                mIsActive);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tab)) {
            return false;
        }
        Tab otherTab = (Tab) other;

        return Objects.equals(mTitle, otherTab.mTitle)
                && Objects.equals(mContentId, otherTab.mContentId)
                && Objects.equals(mIcon, otherTab.mIcon)
                && mIsActive == otherTab.isActive();
    }

    /**
     * Creates and returns a new {@link Builder} initialized with this {@link Tab}'s data.
     */
    @NonNull
    public Tab.Builder toBuilder() {
        return new Tab.Builder(this);
    }

    Tab(Tab.Builder builder) {
        mTitle = builder.mTitle;
        mIcon = builder.mIcon;
        mIsActive = builder.mIsActive;

        if (builder.mContentId != null) {
            mContentId = builder.mContentId;
        } else {
            mContentId = EMPTY_TAB_CONTENT_ID;
        }
    }

    /** Constructs an empty instance, used by serialization code. */
    private Tab() {
        mTitle = null;
        mContentId = EMPTY_TAB_CONTENT_ID;
        mIcon = null;
        mIsActive = false;
    }

    /** A builder of {@link Tab}. */
    public static final class Builder {
        boolean mIsActive;

        @Nullable
        CarText mTitle;

        @Nullable
        CarIcon mIcon;

        @Nullable
        String mContentId;

        /**
         * Sets the title of the tab.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} is empty, of if it contains
         *                                  unsupported spans
         */
        @NonNull
        public Tab.Builder setTitle(@NonNull CharSequence title) {
            CarText titleText = CarText.create(requireNonNull(title));
            if (titleText.isEmpty()) {
                throw new IllegalArgumentException("The title cannot be null or empty");
            }
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(titleText);
            mTitle = titleText;
            return this;
        }

        /**
         * Sets the content ID of the tab.
         */
        @NonNull
        public Tab.Builder setContentId(@NonNull String contentId) {
            if (requireNonNull(contentId).isEmpty()) {
                throw new IllegalArgumentException("The content ID cannot be null or empty");
            }
            mContentId = contentId;
            return this;
        }

        /**
         * Sets the icon to display in the tab.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * icons targeting a 36 x 36 dp bounding box. If the icon exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        @NonNull
        public Tab.Builder setIcon(@NonNull CarIcon icon) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(icon));
            mIcon = icon;
            return this;
        }

        /**
         * Sets the active state of the tab.
         *
         * @deprecated use {@link TabTemplate.Builder#setActiveTabContentId(String)} instead.
         */
        @NonNull
        @Deprecated
        public Tab.Builder setActive(boolean isActive) {
            mIsActive = isActive;
            return this;
        }

        /**
         * Constructs the {@link Tab} defined by this builder.
         *
         * @throws IllegalStateException if the tab's title, icon or content ID is not set.
         */
        @NonNull
        public Tab build() {
            if (mTitle == null) {
                throw new IllegalStateException("A title must be set for the tab");
            }

            if (mIcon == null) {
                throw new IllegalStateException("A icon must be set for the tab");
            }

            if (mContentId == null) {
                throw new IllegalStateException(
                        "A content ID must be set for the tab");
            }

            return new Tab(this);
        }

        /** Returns an empty {@link Tab.Builder} instance. */
        public Builder() {
        }

        /** Creates a new {@link Builder}, populated from the input {@link Tab} */
        Builder(@NonNull Tab tab) {
            requireNonNull(tab);
            mIsActive = tab.isActive();
            mContentId = tab.getContentId();
            mIcon = tab.getIcon();
            mTitle = tab.getTitle();
        }
    }
}
