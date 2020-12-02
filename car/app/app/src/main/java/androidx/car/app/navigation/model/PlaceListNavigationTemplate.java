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

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarAppPermission;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceListener;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DistanceSpan;
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
 * {@link androidx.car.app.AppManager#setSurfaceListener(SurfaceListener)}.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in {@link Screen#onGetTemplate()}, this template
 * is considered a refresh of a previous one if:
 *
 * <ul>
 *   <li>The template title has not changed, and
 *   <li>The previous template is in a loading state (see {@link Builder#setIsLoading}, or the
 *       number of rows and the string contents (title, texts, not counting spans) of each row
 *       between the previous and new {@link ItemList}s have not changed.
 * </ul>
 *
 * <p>In order to use this template your car app <b>MUST</b> declare that it uses the {@code
 * androidx.car.app.NAVIGATION_TEMPLATES} permission in the manifest.
 */
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

    /** Constructs a new builder of {@link PlaceListNavigationTemplate}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
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

    @Override
    public void checkPermissions(@NonNull Context context) {
        CarAppPermission.checkHasLibraryPermission(context, CarAppPermission.NAVIGATION_TEMPLATES);
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

    private PlaceListNavigationTemplate(Builder builder) {
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
        private CarText mTitle;
        private boolean mIsLoading;
        @Nullable
        private ItemList mItemList;
        @Nullable
        private Action mHeaderAction;
        @Nullable
        private ActionStrip mActionStrip;

        /** Sets the {@link CharSequence} to show as title, or {@code null} to not show a title. */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will show a loading indicator where the list content
         * would be otherwise. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} and send the new template content to the
         * host
         * once the data is ready. If set to {@code false}, the UI shows the {@link ItemList}
         * contents added via {@link #setItemList}.
         */
        // TODO(rampara): Consider renaming to setLoading()
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setIsLoading(boolean isLoading) {
            this.mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template, or
         * {@code null} to not display an action.
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
         * Sets an {@link ItemList} to show in the list view along with the map, or {@code null}
         * to not display a list.
         *
         * <p>To show a marker corresponding to a point of interest represented by a row, set the
         * {@link Place} instance via {@link Row.Builder#setMetadata}. The host will render the
         * {@link PlaceMarker} in the list view as the row become visible. The app should
         * synchronize with the list's behavior by rendering the same marker on the map surface.
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
         * title or texts, to indicate the distance of the point of interest from the current
         * location. Where in the title or text the span is attached to is up to the app.
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

        /**
         * Sets an {@link ItemList} for the template. This method does not enforce the
         * template's requirements and is only intended for testing purposes.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @VisibleForTesting
        @NonNull
        public Builder setItemListForTesting(@Nullable ItemList itemList) {
            this.mItemList = itemList;
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template, or {@code null} to not show an {@link
         * ActionStrip}.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
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
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalArgumentException if the template is in a loading state but the list is
         *                                  set, or vice-versa.
         * @throws IllegalStateException    if the template does not have either a title or header
         *                                  {@link Action} set.
         */
        @NonNull
        public PlaceListNavigationTemplate build() {
            boolean hasList = mItemList != null;
            if (mIsLoading == hasList) {
                throw new IllegalArgumentException(
                        "Template is in a loading state but a list is set, or vice versa.");
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new PlaceListNavigationTemplate(this);
        }
    }
}
