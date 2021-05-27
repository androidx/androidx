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

package androidx.car.app.navigation.model;

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ModelUtils;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template that supports showing a list of places alongside a custom drawn map.
 *
 * <p>The template itself does not expose a drawing surface. In order to draw on the canvas, use
 * {@link androidx.car.app.AppManager#setSurfaceCallback(SurfaceCallback)}.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in {@link Screen#onGetTemplate()}, this template
 * is considered a refresh of a previous one if:
 *
 * <ul>
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}, or
 *   <li>The template title has not changed, and the number of rows and the title (not
 *       counting spans) of each row between the previous and new {@link ItemList}s have not
 *       changed.
 * </ul>
 *
 * <p>In order to use this template your car app <b>MUST</b> declare that it uses the {@code
 * androidx.car.app.NAVIGATION_TEMPLATES} permission in the manifest.
 */
@CarProtocol
public final class PlaceListNavigationTemplate implements Template {
    @Keep
    private final boolean mIsLoading;
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final ItemList mItemList;
    @Keep
    @Nullable
    private final Action mHeaderAction;
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
     * Returns the {@link Action} that is set to be displayed in the header of the template or
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
     * Returns the list of items to display alongside the map or {@code null} if the list is not
     * set.
     *
     * @see Builder#setItemList(ItemList)
     */
    @Nullable
    public ItemList getItemList() {
        return mItemList;
    }

    @NonNull
    @Override
    public String toString() {
        return "PlaceListNavigationTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mIsLoading, mItemList, mHeaderAction, mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlaceListNavigationTemplate)) {
            return false;
        }
        PlaceListNavigationTemplate otherTemplate = (PlaceListNavigationTemplate) other;

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mItemList, otherTemplate.mItemList)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    PlaceListNavigationTemplate(Builder builder) {
        mTitle = builder.mTitle;
        mIsLoading = builder.mIsLoading;
        mItemList = builder.mItemList;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private PlaceListNavigationTemplate() {
        mTitle = null;
        mIsLoading = false;
        mItemList = null;
        mHeaderAction = null;
        mActionStrip = null;
    }

    /** A builder of {@link PlaceListNavigationTemplate}. */
    public static final class Builder {
        @Nullable
        CarText mTitle;
        boolean mIsLoading;
        @Nullable
        ItemList mItemList;
        @Nullable
        Action mHeaderAction;
        @Nullable
        ActionStrip mActionStrip;

        /**
         * Sets the title of the template.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is null
         * @see CarText
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets the title of the template, with support for multiple length variants.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is null
         * @see CarText
         */
        @NonNull
        public Builder setTitle(@NonNull CarText title) {
            mTitle = requireNonNull(title);
            return this;
        }

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will show a loading indicator where the list content
         * would be otherwise. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} and send the new template content to the
         * host once the data is ready. If set to {@code false}, the UI shows the {@link ItemList}
         * contents added via {@link #setItemList}.
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
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets an {@link ItemList} to show in the list view along with the map.
         *
         * <p>To show a marker corresponding to a point of interest represented by a row, set the
         * {@link Place} instance via {@link Row.Builder#setMetadata}. The host will render the
         * {@link PlaceMarker} in the list view as the row become visible. The app should
         * synchronize with the list's behavior by rendering the same marker on the map surface.
         *
         * <h4>Requirements</h4>
         *
         * The number of items in the {@link ItemList} should be smaller or equal than the limit
         * provided by
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_PLACE_LIST}. The
         * host will ignore any items over that limit. The list itself cannot be selectable as
         * set via {@link ItemList.Builder#setOnSelectedListener}. Each {@link Row} can add up to
         * 2 lines of texts via {@link Row.Builder#addText} and cannot contain a {@link Toggle}.
         *
         * <p>Images of type {@link Row#IMAGE_TYPE_LARGE} are not allowed in this template.
         *
         * <p>Rows are not allowed to have both and an image and a place marker.
         *
         * <p>All non-browsable rows must have a {@link DistanceSpan} attached to either its
         * title or texts, to indicate the distance of the point of interest from the current
         * location. Where in the title or text the span is attached to is up to the app.
         *
         * @throws IllegalArgumentException if {@code itemList} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code itemList} is {@code null}
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        @NonNull
        public Builder setItemList(@NonNull ItemList itemList) {
            List<Item> items = requireNonNull(itemList).getItems();
            ROW_LIST_CONSTRAINTS_SIMPLE.validateOrThrow(itemList);
            ModelUtils.validateAllNonBrowsableRowsHaveDistance(items);
            ModelUtils.validateAllRowsHaveOnlySmallImages(items);
            ModelUtils.validateNoRowsHaveBothMarkersAndImages(items);
            mItemList = itemList;
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template, or {@code null} to not display an {@link
         * ActionStrip}.
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
         * Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalArgumentException if the template is in a loading state but the list is
         *                                  set, or vice versa
         * @throws IllegalStateException    if the template does not have either a title or header
         *                                  {@link Action} set
         */
        @NonNull
        public PlaceListNavigationTemplate build() {
            boolean hasList = mItemList != null;
            if (mIsLoading == hasList) {
                throw new IllegalArgumentException(
                        "Template is in a loading state but a list is set, or vice versa");
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new PlaceListNavigationTemplate(this);
        }

        /** Constructs an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
