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
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_MAP;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ModelUtils;
import androidx.car.app.model.OnContentRefreshDelegate;
import androidx.car.app.model.OnContentRefreshDelegateImpl;
import androidx.car.app.model.OnContentRefreshListener;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.car.app.model.constraints.CarTextConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
 *   <li>The template is sent in response to a user-initiated content refresh request. (see
 *       {@link PlaceListMapTemplate.Builder#setOnContentRefreshListener}.
 * </ul>
 *
 * <p>In order to use this template your car app <b>MUST</b> declare that it uses the {@code
 * androidx.car.app.NAVIGATION_TEMPLATES} permission in the manifest.</p>
 *
 *  <br>For instance, using the deprecated {@link PlaceListNavigationTemplate}, if the template was:
 *
 *  <pre>
 *  <code>PlaceListNavigationTemplate template = new PlaceListNavigationTemplate.Builder()
 *          .setItemList(itemListBuilder.build())
 *          .setHeader(header)
 *          .setActionStrip(actionStrip)
 *          .setMapActionStrip(mapActionStrip)
 *          .build();
 * </code>
 * </pre>
 * Using the new {@link MapWithContentTemplate}, the template would be:
 *  <pre>
 *  <code>MapWithContentTemplate template = new MapWithContentTemplate.Builder()
 *            .setContentTemplate(new ListTemplate.Builder()
 *                   .setSingleList(itemListBuilder.build())
 *                   .setHeader(header)
 *                   .build())
 *            .setActionStrip(actionStrip)
 *            .setMapController(new MapController.Builder()
 *                  .setMapActionStrip(mapActionStrip)
 *                  .build())
 *            .build();
 * </code>
 * </pre>
 * @deprecated with API 7. Use the {@link MapWithContentTemplate} API instead.
 */
@CarProtocol
@KeepFields
@Deprecated
public final class PlaceListNavigationTemplate implements Template {
    private final boolean mIsLoading;
    /**
     * @deprecated Use the Header to set up the Title.
     */
    // TODO(b/225914724): remove after hosts switch over to setHeader().
    @Deprecated
    private final @Nullable CarText mTitle;
    private final @Nullable ItemList mItemList;
    private final @Nullable Header mHeader;
    /**
     * @deprecated Use the Header to set up the HeaderAction.
     */
    // TODO(b/225914724): remove after hosts switch over to setHeader().
    @Deprecated
    private final @Nullable Action mHeaderAction;
    private final @Nullable ActionStrip mActionStrip;
    private final @Nullable ActionStrip mMapActionStrip;
    private final @Nullable PanModeDelegate mPanModeDelegate;
    private final @Nullable OnContentRefreshDelegate mOnContentRefreshDelegate;

    /**
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     * @deprecated use {@link #getHeader()}
     */
    // TODO(b/225914724): remove after hosts switch over to getHeader()
    @Deprecated
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Header} to display in this template.
     *
     * @see Builder#setHeader(Header)
     */
    @RequiresCarApi(5)
    public @Nullable Header getHeader() {
        return mHeader;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template or
     * {@code null} if not set.
     *
     * @see Builder#setHeaderAction(Action)
     * @deprecated use {@link #getHeader()}
     */
    // TODO(b/225914724): remove after hosts switch over to getHeader().
    @Deprecated
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
     * Returns the map {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setMapActionStrip(ActionStrip)
     */
    @RequiresCarApi(4)
    public @Nullable ActionStrip getMapActionStrip() {
        return mMapActionStrip;
    }

    /**
     * Returns the {@link PanModeDelegate} that should be called when the user interacts with
     * pan mode on this template, or {@code null} if a {@link PanModeListener} was not set.
     */
    @RequiresCarApi(4)
    public @Nullable PanModeDelegate getPanModeDelegate() {
        return mPanModeDelegate;
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
    public @Nullable ItemList getItemList() {
        return mItemList;
    }

    /**
     * Returns the {@link OnContentRefreshDelegate} to be called when the user requests for content
     * refresh for this template.
     *
     * @see PlaceListMapTemplate.Builder#setOnContentRefreshListener
     */
    public @Nullable OnContentRefreshDelegate getOnContentRefreshDelegate() {
        return mOnContentRefreshDelegate;
    }

    @Override
    public @NonNull String toString() {
        return "PlaceListNavigationTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mIsLoading, mItemList, mHeaderAction, mActionStrip,
                mMapActionStrip, mPanModeDelegate == null, mOnContentRefreshDelegate == null,
                mHeader);
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
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mMapActionStrip, otherTemplate.mMapActionStrip)
                && Objects.equals(mPanModeDelegate == null, otherTemplate.mPanModeDelegate == null)
                && Objects.equals(mOnContentRefreshDelegate == null,
                otherTemplate.mOnContentRefreshDelegate == null)
                && Objects.equals(mHeader, otherTemplate.mHeader);
    }

    PlaceListNavigationTemplate(Builder builder) {
        mTitle = builder.mTitle;
        mIsLoading = builder.mIsLoading;
        mItemList = builder.mItemList;
        mHeader = builder.mHeader;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
        mMapActionStrip = builder.mMapActionStrip;
        mPanModeDelegate = builder.mPanModeDelegate;
        mOnContentRefreshDelegate = builder.mOnContentRefreshDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private PlaceListNavigationTemplate() {
        mTitle = null;
        mIsLoading = false;
        mItemList = null;
        mHeader = null;
        mHeaderAction = null;
        mActionStrip = null;
        mMapActionStrip = null;
        mPanModeDelegate = null;
        mOnContentRefreshDelegate = null;
    }

    /** A builder of {@link PlaceListNavigationTemplate}. */
    public static final class Builder {
        @Nullable CarText mTitle;
        boolean mIsLoading;
        @Nullable ItemList mItemList;
        @Nullable Header mHeader;
        @Nullable Action mHeaderAction;
        @Nullable ActionStrip mActionStrip;
        @Nullable ActionStrip mMapActionStrip;
        @Nullable PanModeDelegate mPanModeDelegate;
        @Nullable OnContentRefreshDelegate mOnContentRefreshDelegate;

        /**
         * Sets the title of the template.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         * @deprecated use {@link #setHeader(Header)}
         */
        // TODO(b/225914724): remove after hosts switch over to setHeader().
        @Deprecated
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
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         * @deprecated use {@link #setHeader(Header)}
         */
        // TODO(b/225914724): remove after hosts switch over to setHeader().
        @Deprecated
        public @NonNull Builder setTitle(@NonNull CarText title) {
            mTitle = requireNonNull(title);
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
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
         * @deprecated use {@link #setHeader(Header)}
         */
        // TODO(b/225914724): remove after hosts switch over to setHeader().
        @Deprecated
        public @NonNull Builder setHeaderAction(@NonNull Action headerAction) {
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
         * Sets the {@link Header} for this template.
         *
         * @throws NullPointerException if {@code header} is null
         */
        @RequiresCarApi(5)
        public @NonNull Builder setHeader(@NonNull Header header) {
            mHeader = requireNonNull(header);
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
         * Sets an {@link ActionStrip} with a list of map-control related actions for this
         * template, such as pan or zoom.
         *
         * <p>The host will draw the buttons in an area that is associated with map controls.
         *
         * <p>If the app does not include the {@link Action#PAN} button in this
         * {@link ActionStrip}, the app will not receive the user input for panning gestures from
         * {@link SurfaceCallback} methods, and the host will exit any previously activated pan
         * mode.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 4 {@link Action}s in its map {@link ActionStrip}. Only
         * {@link Action}s with icons set via {@link Action.Builder#setIcon} are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        @RequiresCarApi(4)
        public @NonNull Builder setMapActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_MAP.validateOrThrow(
                    requireNonNull(actionStrip).getActions());
            mMapActionStrip = actionStrip;
            return this;
        }

        /**
         * Sets a {@link PanModeListener} that notifies when the user enters and exits
         * the pan mode.
         *
         * <p>If the app does not include the {@link Action#PAN} button in the map
         * {@link ActionStrip}, the app will not receive the user input for panning gestures from
         * {@link SurfaceCallback} methods, and the host will exit any previously activated pan
         * mode.
         *
         * @throws NullPointerException if {@code panModeListener} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        @RequiresCarApi(4)
        public @NonNull Builder setPanModeListener(@NonNull PanModeListener panModeListener) {
            requireNonNull(panModeListener);
            mPanModeDelegate = PanModeDelegateImpl.create(panModeListener);
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
        public @NonNull PlaceListNavigationTemplate build() {
            boolean hasList = mItemList != null;
            if (mIsLoading == hasList) {
                throw new IllegalArgumentException(
                        "Template is in a loading state but a list is set, or vice versa");
            }

            return new PlaceListNavigationTemplate(this);
        }

        /** Constructs an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
