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
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_SIMPLE;

import android.Manifest.permission;
import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarAppPermission;

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
 *   <li>The template title has not changed, and
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}, or the
 *       number of rows and the string contents (title, texts, not counting spans) of each row
 *       between the previous and new {@link ItemList}s have not changed.
 * </ul>
 */
public final class PlaceListMapTemplate implements Template {
    @Keep
    private final boolean mIsLoading;
    @Keep
    private final boolean mShowCurrentLocation;
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
    @Keep
    @Nullable
    private final Place mAnchor;

    /** Constructs a new builder of {@link PlaceListMapTemplate}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public boolean isCurrentLocationEnabled() {
        return mShowCurrentLocation;
    }

    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    @Nullable
    public ItemList getItemList() {
        return mItemList;
    }

    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    @Nullable
    public Place getAnchor() {
        return mAnchor;
    }

    @Override
    public void checkPermissions(@NonNull Context context) {
        if (isCurrentLocationEnabled()) {
            CarAppPermission.checkHasPermission(context, permission.ACCESS_FINE_LOCATION);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "PlaceListMapTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mShowCurrentLocation, mIsLoading, mTitle, mItemList, mHeaderAction, mActionStrip,
                mAnchor);
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
                && Objects.equals(mAnchor, otherTemplate.mAnchor);
    }

    private PlaceListMapTemplate(Builder builder) {
        mShowCurrentLocation = builder.mShowCurrentLocation;
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mItemList = builder.mItemList;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
        mAnchor = builder.mAnchor;
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
    }

    /** A builder of {@link PlaceListMapTemplate}. */
    public static final class Builder {
        private boolean mShowCurrentLocation;
        private boolean mIsLoading;
        @Nullable
        private CarText mTitle;
        @Nullable
        private ItemList mItemList;
        @Nullable
        private Action mHeaderAction;
        @Nullable
        private ActionStrip mActionStrip;
        @Nullable
        private Place mAnchor;

        /**
         * Sets whether to show the current location in the map.
         *
         * <p>The map template will show the user's current location on the map, which is normally
         * indicated by a blue dot.
         *
         * <p>This functionality requires the app to have the {@code ACCESS_FINE_LOCATION}
         * permission.
         */
        @NonNull
        public Builder setCurrentLocationEnabled(boolean isEnabled) {
            this.mShowCurrentLocation = isEnabled;
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

        /**
         * Sets the {@link CharSequence} to show as the template's title, or {@code null} to not
         * display
         * a title.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /**
         * Sets an {@link ItemList} to show in a list view along with the map, or {@code null} to
         * not display a list.
         *
         * <p>To show a marker corresponding to a point of interest represented by a row, set the
         * {@link Place} instance via {@link Row.Builder#setMetadata}. The host will display the
         * {@link PlaceMarker} in both the map and the list view as the row becomes visible.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 6 {@link Row}s in the {@link ItemList}. The host will
         * ignore any items over that limit. The list itself cannot be selectable as set via {@link
         * ItemList.Builder#setOnSelectedListener}. Each {@link Row} can add up to 2 lines of texts
         * via {@link Row.Builder#addText} and cannot contain a {@link Toggle}.
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
         *                                  requirements.
         */
        @NonNull
        public Builder setItemList(@Nullable ItemList itemList) {
            if (itemList != null) {
                List<Object> items = itemList.getItems();
                ROW_LIST_CONSTRAINTS_SIMPLE.validateOrThrow(itemList);
                ModelUtils.validateAllNonBrowsableRowsHaveDistance(items);
                ModelUtils.validateAllRowsHaveOnlySmallImages(items);
                ModelUtils.validateNoRowsHaveBothMarkersAndImages(items);
            }
            this.mItemList = itemList;

            return this;
        }

        /** @hide */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder setItemListForTesting(@Nullable ItemList itemList) {
            this.mItemList = itemList;
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
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the template's
         *                                  requirements.
         */
        @NonNull
        public Builder setActionStrip(@Nullable ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(
                    actionStrip == null ? Collections.emptyList() : actionStrip.getActions());
            this.mActionStrip = actionStrip;
            return this;
        }

        /**
         * Sets the anchor maker on the map, or {@code null} to not display an anchor marker.
         *
         * <p>The anchor marker is displayed differently from other markers by the host.
         *
         * <p>If not {@code null}, an anchor marker will be shown at the specified {@link LatLng}
         * on the
         * map. The camera will adapt to always have the anchor marker visible within its viewport,
         * along with other places' markers from {@link Row} that are currently visible in the
         * {@link
         * Pane}. This can be used to provide a reference point on the map (e.g. the center of a
         * search
         * region) as the user pages through the {@link Pane}'s markers, for example.
         */
        @NonNull
        public Builder setAnchor(@Nullable Place anchor) {
            this.mAnchor = anchor;
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
         *                                  set,
         *                                  or vice versa.
         * @throws IllegalStateException    if the template does not have either a title or header
         *                                  {@link Action} set.
         */
        @NonNull
        public PlaceListMapTemplate build() {
            boolean hasList = mItemList != null;
            if (mIsLoading == hasList) {
                throw new IllegalArgumentException(
                        "Template is in a loading state but a list is set, or vice versa.");
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new PlaceListMapTemplate(this);
        }
    }
}
