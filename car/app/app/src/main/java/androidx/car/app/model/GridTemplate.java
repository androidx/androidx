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
import androidx.car.app.annotations.CarProtocol;

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
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}, or
 *   <li>The template title has not changed, and the number of grid items and the title of each
 *       grid item have not changed.
 * </ul>
 */
@CarProtocol
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

    /**
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template, or
     * {@code null} if not set.
     *
     * @see Builder#setHeaderAction(Action)
     */
    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    /**
     * Returns whether the template is loading.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the {@link ItemList} instance that contains the grid items to display or {@code
     * null} if not set.
     *
     * @see Builder#setSingleList(ItemList)
     */
    @Nullable
    public ItemList getSingleList() {
        return mSingleList;
    }

    @NonNull
    @Override
    public String toString() {
        return "GridTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsLoading, mTitle, mHeaderAction, mSingleList, mActionStrip);
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
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    GridTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mHeaderAction = builder.mHeaderAction;
        mSingleList = builder.mSingleList;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private GridTemplate() {
        mIsLoading = false;
        mTitle = null;
        mHeaderAction = null;
        mSingleList = null;
        mActionStrip = null;
    }

    /** A builder of {@link GridTemplate}. */
    public static final class Builder {
        boolean mIsLoading;
        @Nullable
        ItemList mSingleList;
        @Nullable
        CarText mTitle;
        @Nullable
        Action mHeaderAction;
        @Nullable
        ActionStrip mActionStrip;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where the grid content
         * would be otherwise. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} and send the new template content to the
         * host once the data is ready. If set to {@code false}, the UI shows the
         * {@link ItemList} contents added via {@link #setSingleList}.
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template.
         *
         * <p>Unless set with this method, the template will not have a header action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either one of {@link Action#APP_ICON} and
         * {@link Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         */
        @NonNull
        public Builder setHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(Collections.singletonList(headerAction));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the title of the template.
         *
         * <p>Unless set with this method, the template will not have a title.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is null
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets a single {@link ItemList} to show in the template.
         *
         * @throws NullPointerException if {@code list} is null
         */
        @NonNull
        public Builder setSingleList(@NonNull ItemList list) {
            mSingleList = requireNonNull(list);
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template.
         *
         * <p>Unless set with this method, the template will not have an action strip.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        @NonNull
        public Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * The number of items in the {@link ItemList} should be smaller or equal than the limit
         * provided by
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_GRID}. The
         * host will ignore any items over that limit.
         *
         * <p>Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalStateException    if the template is in a loading state but there are
         *                                  lists added, or vice versa, or if the template does not
         *                                  have either a title or header {@link Action} set.
         * @throws IllegalArgumentException if the added {@link ItemList} does not meet the
         *                                  template's requirements.
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        @NonNull
        public GridTemplate build() {
            boolean hasList = mSingleList != null;
            if (mIsLoading == hasList) {
                throw new IllegalStateException(
                        "Template is in a loading state but lists are added, or vice versa");
            }

            if (mSingleList != null) {
                for (Item gridItemObject : mSingleList.getItems()) {
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

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
