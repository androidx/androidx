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
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.Screen;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarTextConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template that displays a map along with a list of places.
 *
 * <p>The map can display markers corresponding to the places in the list. See {@link
 * Builder#setItemList} for details.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link androidx.car.app.Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}, or
 *   <li>The template title has not changed, and the number of rows and the title (not counting
 *       spans) of each row between the previous and new {@link ItemList}s have not changed.
 *   <li>The template is sent in response to a user-initiated content refresh request. (see
 *       {@link Builder#setOnContentRefreshListener}.
 * </ul>
 */
@CarProtocol
@KeepFields
public final class PlaceListMapTemplate implements Template {
    private final boolean mIsLoading;
    private final boolean mShowCurrentLocation;
    private final @Nullable CarText mTitle;
    private final @Nullable ItemList mItemList;
    private final @Nullable Action mHeaderAction;
    private final @Nullable ActionStrip mActionStrip;
    private final @Nullable Place mAnchor;
    private final @Nullable OnContentRefreshDelegate mOnContentRefreshDelegate;

    public boolean isCurrentLocationEnabled() {
        return mShowCurrentLocation;
    }

    /**
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template, or
     * {@code null} if not set.
     *
     * @see Builder#setHeaderAction(Action)
     */
    public @Nullable Action getHeaderAction() {
        return mHeaderAction;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    public @Nullable ActionStrip getActionStrip() {
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
     * Returns the {@link ItemList} instance with the list of items to display in the template,
     * or {@code null} if not set.
     *
     * @see Builder#setItemList(ItemList)
     */
    public @Nullable ItemList getItemList() {
        return mItemList;
    }

    /**
     * Returns the {@link Place} instance to display as an anchor in the map.
     *
     * @see Builder#setAnchor(Place)
     */
    public @Nullable Place getAnchor() {
        return mAnchor;
    }

    /**
     * Returns the {@link OnContentRefreshDelegate} to be called when the user requests for content
     * refresh for this template.
     *
     * @see Builder#setOnContentRefreshListener
     */
    @RequiresCarApi(5)
    public @Nullable OnContentRefreshDelegate getOnContentRefreshDelegate() {
        return mOnContentRefreshDelegate;
    }

    @Override
    public @NonNull String toString() {
        return "PlaceListMapTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mShowCurrentLocation, mIsLoading, mTitle, mItemList, mHeaderAction, mActionStrip,
                mAnchor, mOnContentRefreshDelegate == null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlaceListMapTemplate)) {
            return false;
        }
        PlaceListMapTemplate otherTemplate = (PlaceListMapTemplate) other;

        return mShowCurrentLocation == otherTemplate.mShowCurrentLocation
                && mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mItemList, otherTemplate.mItemList)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mAnchor, otherTemplate.mAnchor)
                && Objects.equals(mOnContentRefreshDelegate == null,
                otherTemplate.mOnContentRefreshDelegate == null);
    }

    PlaceListMapTemplate(Builder builder) {
        mShowCurrentLocation = builder.mShowCurrentLocation;
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mItemList = builder.mItemList;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
        mAnchor = builder.mAnchor;
        mOnContentRefreshDelegate = builder.mOnContentRefreshDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private PlaceListMapTemplate() {
        mShowCurrentLocation = false;
        mIsLoading = false;
        mTitle = null;
        mItemList = null;
        mHeaderAction = null;
        mActionStrip = null;
        mAnchor = null;
        mOnContentRefreshDelegate = null;
    }

    /** A builder of {@link PlaceListMapTemplate}. */
    public static final class Builder {
        boolean mShowCurrentLocation;
        boolean mIsLoading;
        @Nullable CarText mTitle;
        @Nullable ItemList mItemList;
        @Nullable Action mHeaderAction;
        @Nullable ActionStrip mActionStrip;
        @Nullable Place mAnchor;
        @RequiresCarApi(5)
        @Nullable OnContentRefreshDelegate mOnContentRefreshDelegate;

        /**
         * Sets whether to show the current location in the map.
         *
         * <p>The map template will show the user's current location on the map.
         *
         * <p>This functionality requires the app to have either the {@code ACCESS_FINE_LOCATION} or
         * {@code ACCESS_COARSE_LOCATION} permission. When {@code isEnabled} is {@code true}, the
         * host may receive location updates from the app in order to show the user's current
         * location.
         */
        public @NonNull Builder setCurrentLocationEnabled(boolean isEnabled) {
            mShowCurrentLocation = isEnabled;
            return this;
        }

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator where the list content
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready. If set to {@code false}, the UI shows the {@link
         * ItemList} contents added via {@link #setItemList}.
         */
        public @NonNull Builder setLoading(boolean isLoading) {
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
        public @NonNull Builder setHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the title of the template.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is null
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }

        /**
         * Sets the title of the template, with support for multiple length variants.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is null
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CarText title) {
            mTitle = requireNonNull(title);
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }

        /**
         * Sets an {@link ItemList} to show in a list view along with the map.
         *
         * <p>Unless set with this method, the template will not show an item list.
         *
         * <p>To show a marker corresponding to a point of interest represented by a row, set the
         * {@link Place} instance via {@link Row.Builder#setMetadata}. The host will display the
         * {@link PlaceMarker} in both the map and the list view as the row becomes visible.
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
         * title or texts to indicate the distance of the point of interest from the current
         * location. A row is browsable when it's configured like so with
         * {@link Row.Builder#setBrowsable(boolean)}.
         *
         * @throws IllegalArgumentException if {@code itemList} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code itemList} is {@code null}
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        public @NonNull Builder setItemList(@NonNull ItemList itemList) {
            List<Item> items = requireNonNull(itemList).getItems();
            ROW_LIST_CONSTRAINTS_SIMPLE.validateOrThrow(itemList);
            ModelUtils.validateAllNonBrowsableRowsHaveDistance(items);
            ModelUtils.validateAllRowsHaveOnlySmallImages(items);
            ModelUtils.validateNoRowsHaveBothMarkersAndImages(items);
            mItemList = itemList;
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template.
         *
         * <p>Unless set with this method, the template will not have an action strip.
         *
         * <p>The {@link Action} buttons in Map Based Template are automatically adjusted based
         * on the screen size. On narrow width screen, icon {@link Action}s show by
         * default. If no icon specify, showing title {@link Action}s instead. On wider width
         * screen, title {@link Action}s show by default. If no title specify, showing icon
         * {@link Action}s instead.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 4 {@link Action}s in its {@link ActionStrip}. Of the 4
         * allowed {@link Action}s, it can either be a title {@link Action} as set via
         * {@link Action.Builder#setTitle}, or a icon {@link Action} as set via
         * {@link Action.Builder#setIcon}.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        public @NonNull Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_NAVIGATION
                    .validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Sets the anchor maker on the map.
         *
         * <p>An anchor marker will not be displayed unless set with this method.
         *
         * <p>The anchor marker is displayed differently from other markers by the host.
         *
         * <p>If not {@code null}, an anchor marker will be shown at the specified
         * {@link CarLocation} on the map. The camera will adapt to always have the anchor marker
         * visible within its viewport, along with other places' markers from {@link Row} that
         * are currently visible in the {@link Pane}. This can be used to provide a reference
         * point on the map (e.g. the center of a search region) as the user pages through the
         * {@link Pane}'s markers, for example.
         *
         * @throws NullPointerException if {@code anchor} is {@code null}
         */
        public @NonNull Builder setAnchor(@NonNull Place anchor) {
            mAnchor = requireNonNull(anchor);
            return this;
        }

        /**
         * Sets the {@link OnContentRefreshListener} to call when the user requests for the list
         * contents to be refreshed in this template.
         *
         * <p>When the listener is triggered, an app can send a new {@link PlaceListMapTemplate},
         * for example, to show a new set of point-of-interests based on the current user
         * location, without the car host counting it against the template quota described in
         * {@link Screen#onGetTemplate()}.
         *
         * @throws NullPointerException if {@code itemVisibilityChangedListener} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        @RequiresCarApi(5)
        public @NonNull Builder setOnContentRefreshListener(
                @NonNull OnContentRefreshListener onContentRefreshListener) {
            mOnContentRefreshDelegate =
                    OnContentRefreshDelegateImpl.create(onContentRefreshListener);
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * <p>If neither header {@link Action} nor title have been set on the template, the
         * header is hidden.
         *
         * @throws IllegalArgumentException if the template is in a loading state but the list is
         *                                  set, or vice versa
         */
        public @NonNull PlaceListMapTemplate build() {
            boolean hasList = mItemList != null;
            if (mIsLoading == hasList) {
                throw new IllegalArgumentException(
                        "Template is in a loading state but a list is set, or vice versa");
            }

            return new PlaceListMapTemplate(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
