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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.util.Collections;
import java.util.Objects;

/**
 * A template representing a grid of items.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link androidx.car.app.Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The template title has not changed, and
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}, or the
 *       number of grid items and the string contents (title, texts) of each grid item have not
 *       changed.
 *   <li>For grid items that contain a {@link Toggle}, updates to the title, text and image are also
 *       allowed if the toggle state has changed between the previous and new templates.
 * </ul>
 */
public final class GridTemplate implements Template {
    @Keep
    private final boolean mIsLoading;
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final Action mHeaderAction;
    @Keep
    @Nullable
    private final ItemList mSingleList;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;
    @Keep
    @Nullable
    private final CarIcon mBackgroundImage;

    /** Constructs a new builder of {@link GridTemplate}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    @Nullable
    public ItemList getSingleList() {
        return mSingleList;
    }

    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    @Nullable
    public CarIcon getBackgroundImage() {
        return mBackgroundImage;
    }

    @NonNull
    @Override
    public String toString() {
        return "GridTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsLoading, mTitle, mHeaderAction, mSingleList, mActionStrip,
                mBackgroundImage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GridTemplate)) {
            return false;
        }
        GridTemplate otherTemplate = (GridTemplate) other;

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mSingleList, otherTemplate.mSingleList)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mBackgroundImage, otherTemplate.mBackgroundImage);
    }

    private GridTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mHeaderAction = builder.mHeaderAction;
        mSingleList = builder.mSingleList;
        mActionStrip = builder.mActionStrip;
        mBackgroundImage = builder.mBackgroundImage;
    }

    /** Constructs an empty instance, used by serialization code. */
    private GridTemplate() {
        mIsLoading = false;
        mTitle = null;
        mHeaderAction = null;
        mSingleList = null;
        mActionStrip = null;
        mBackgroundImage = null;
    }

    /** A builder of {@link GridTemplate}. */
    public static final class Builder {
        private boolean mIsLoading;
        @Nullable
        private ItemList mSingleList;
        @Nullable
        private CarText mTitle;
        @Nullable
        private Action mHeaderAction;
        @Nullable
        private ActionStrip mActionStrip;

        /** For internal, host-side use only. */
        @Nullable
        private CarIcon mBackgroundImage;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where the grid content
         * would be
         * otherwise. The caller is expected to call {@link androidx.car.app.Screen#invalidate()}
         * and send the new template content to the host once the data is ready. If set to {@code
         * false}, the UI shows the {@link ItemList} contents added via {@link #setSingleList}.
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            this.mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template, or
         * {@code null}
         * to not display an action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either either one of {@link Action#APP_ICON} and {@link
         * Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements.
         */
        @NonNull
        public Builder setHeaderAction(@Nullable Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    headerAction == null ? Collections.emptyList()
                            : Collections.singletonList(headerAction));
            this.mHeaderAction = headerAction;
            return this;
        }

        /** Sets the {@link CharSequence} to show as title, or {@code null} to not show a title. */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /**
         * Sets a single {@link ItemList} to show in the template.
         *
         * @throws NullPointerException if {@code list} is null.
         */
        @NonNull
        public Builder setSingleList(@NonNull ItemList list) {
            mSingleList = requireNonNull(list);
            return this;
        }

        /** Resets the list that was added via {@link #setSingleList}. */
        @NonNull
        public Builder clearAllLists() {
            mSingleList = null;
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template, or {@code null} to not display an {@link
         * ActionStrip}.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}.
         * Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements.
         */
        @NonNull
        public Builder setActionStrip(@Nullable ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(
                    actionStrip == null ? Collections.emptyList() : actionStrip.getActions());
            this.mActionStrip = actionStrip;
            return this;
        }

        /**
         * Sets a {@link CarIcon} to be shown as background of the template.
         *
         * <p>For internal, host-side use only.
         */
        @NonNull
        public Builder setBackgroundImage(@Nullable CarIcon backgroundImage) {
            CarIconConstraints.UNCONSTRAINED.validateOrThrow(backgroundImage);
            this.mBackgroundImage = backgroundImage;
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 6 {@link GridItem}s total in the {@link ItemList}(s). The host
         * will ignore any items over that limit.
         *
         * <p>Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalStateException    if the template is in a loading state but there are
         *                                  lists
         *                                  added, or vice versa.
         * @throws IllegalArgumentException if the added {@link ItemList} does not meet the
         *                                  template's
         *                                  requirements.
         * @throws IllegalStateException    if the template does not have either a title or header
         *                                  {@link
         *                                  Action} set.
         */
        @NonNull
        public GridTemplate build() {
            boolean hasList = mSingleList != null;
            if (mIsLoading == hasList) {
                throw new IllegalStateException(
                        "Template is in a loading state but lists are added, or vice versa");
            }

            if (mSingleList != null) {
                for (Object gridItemObject : mSingleList.getItems()) {
                    if (!(gridItemObject instanceof GridItem)) {
                        throw new IllegalArgumentException(
                                "All the items in grid template's item list must be grid items");
                    }
                }
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new GridTemplate(this);
        }

        private Builder() {
        }
    }
}
