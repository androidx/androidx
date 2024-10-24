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

package androidx.car.app.navigation.model;

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION;
import static androidx.car.app.model.constraints.RowListConstraints.MAP_ROW_LIST_CONSTRAINTS_ALLOW_SELECTABLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_PANE;

import static java.util.Objects.requireNonNull;

import androidx.car.app.Screen;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ModelUtils;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A template that displays a map with data such as {@link Pane} or {@link ItemList} on top of it.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The template title has not changed, and the number of rows and the title (not counting
 *       spans) of each row between the previous and new {@link Pane}s or {@link ItemList}s have
 *       not changed.
 * </ul>
 *
 * For instance, using the deprecated {@link MapTemplate}, if the template was:
 *  <pre>
 *  <code>MapTemplate.Builder builder = new MapTemplate.Builder()
 *          .setPane(paneBuilder.build())
 *          .setActionStrip(actionStrip)
 *          .setHeader(header)
 *          .setMapController(mapController)
 *          .build();
 * </code>
 * </pre>
 * Using the new {@link MapWithContentTemplate}, the template would be:
 * <pre>
 * <code>MapWithContentTemplate template = new MapWithContentTemplate.Builder()
 *            .setContentTemplate(new PaneTemplate.Builder(paneBuilder.build())
 *                   .setHeader(header)
 *                   .build())
 *            .setActionStrip(actionStrip)
 *            .setMapController(mapController)
 *            .build();
 * </code>
 * </pre>
 * @deprecated with API 7. Use the {@link MapWithContentTemplate} API instead.
 */
@RequiresCarApi(5)
@CarProtocol
@KeepFields
@Deprecated
public final class MapTemplate implements Template {
    private final @Nullable MapController mMapController;
    private final @Nullable Pane mPane;
    private final @Nullable ItemList mItemList;
    private final @Nullable Header mHeader;
    private final @Nullable ActionStrip mActionStrip;

    MapTemplate(Builder builder) {
        mMapController = builder.mMapController;
        mPane = builder.mPane;
        mItemList = builder.mItemList;
        mHeader = builder.mHeader;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MapTemplate() {
        mMapController = null;
        mPane = null;
        mItemList = null;
        mHeader = null;
        mActionStrip = null;
    }

    /**
     * Returns the controls associated with an app-provided map.
     *
     * @see Builder#setMapController
     */
    public @Nullable MapController getMapController() {
        return mMapController;
    }

    /**
     * Returns the {@link Pane} to display in this template.
     *
     * @see Builder#setPane(Pane)
     */
    public @Nullable Pane getPane() {
        return mPane;
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
     * Returns the {@link Header} to display in this template.
     *
     * @see Builder#setHeader(Header)
     */
    public @Nullable Header getHeader() {
        return mHeader;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    public @Nullable ActionStrip getActionStrip() {
        return mActionStrip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMapController, mPane, mItemList, mHeader, mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MapTemplate)) {
            return false;
        }
        MapTemplate otherTemplate = (MapTemplate) other;

        return Objects.equals(mPane, otherTemplate.mPane)
                && Objects.equals(mItemList, otherTemplate.mItemList)
                && Objects.equals(mHeader, otherTemplate.mHeader)
                && Objects.equals(mMapController, otherTemplate.mMapController)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    /** A builder of {@link MapTemplate}. */
    public static final class Builder {
        @Nullable MapController mMapController;
        @Nullable Pane mPane;
        @Nullable ItemList mItemList;
        @Nullable Header mHeader;
        @Nullable ActionStrip mActionStrip;

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
         * Sets the {@link Pane} for this template.
         *
         * {@link Pane#getImage()} for pane will not be shown in {@link MapTemplate}.
         *
         * <p>Unless set with this method, the template will not show a pane.
         *
         * <h4>Requirements</h4>
         *
         * The number of items in the {@link Pane} should be smaller or equal than the limit
         * provided by
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_PANE}.
         * The host via {@link Row.Builder#addText} and cannot contain either a {@link Toggle} or a
         * {@link OnClickListener}.
         *
         * <p>Up to 2 {@link Action}s are allowed in the {@link Pane}. Each action's title color
         * can be customized with {@link ForegroundCarColorSpan} instances. Any other span is
         * not supported.
         *
         * <p>If none of the header {@link Action}, the header title or the action strip have
         * been set on the template, the header is hidden.
         *
         * @throws IllegalArgumentException if the {@link Pane} does not meet the requirements
         * @throws NullPointerException     if {@code pane} is null
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        public @NonNull Builder setPane(@NonNull Pane pane) {
            List<Action> actions = requireNonNull(pane).getActions();
            ROW_LIST_CONSTRAINTS_PANE.validateOrThrow(pane);
            ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION.validateOrThrow(actions);
            mPane = pane;
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
         * <p>Rows are not allowed to have both an image and a place marker.
         *
         * @throws IllegalArgumentException if {@code itemList} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code itemList} is {@code null}
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        public @NonNull Builder setItemList(@NonNull ItemList itemList) {
            List<Item> items = requireNonNull(itemList).getItems();
            MAP_ROW_LIST_CONSTRAINTS_ALLOW_SELECTABLE.validateOrThrow(itemList);
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
        public @NonNull Builder setHeader(@NonNull Header header) {
            mHeader = requireNonNull(header);
            return this;
        }

        /**
         * Sets the {@link MapController} for this template.
         */
        public @NonNull Builder setMapController(@NonNull MapController mapController) {
            mMapController = requireNonNull(mapController);
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * <p>A {@link Pane} and a {@link Header} must be set on the component.
         *
         * <p> The number of items in the {@link ItemList} should be smaller or equal than the limit
         * provided by
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_PANE}. The host
         * will ignore any rows over that limit. Each {@link Row}s can add up to 2 lines of texts
         * via {@link Row.Builder#addText} and cannot contain either a {@link Toggle} or a {@link
         * OnClickListener}.
         *
         * <p>Up to 2 {@link Action}s are allowed in the {@link Pane}. Each action's title color
         * can be customized with {@link ForegroundCarColorSpan} instances. Any other span is not
         * supported.
         *
         * <p>If neither header {@link Action} nor title have been set on the template, the
         * header is hidden.
         *
         * @throws IllegalArgumentException if the {@link Pane} does not meet the requirements
         * @throws IllegalStateException    if both {@link Pane} and {@link ItemList} are set or
         *                                  are null.
         */
        public @NonNull MapTemplate build() {
            if ((mPane == null) == (mItemList == null)) {
                throw new IllegalStateException("Either Pane or Item List must be set but not "
                        + "both");
            }

            return new MapTemplate(this);
        }
    }
}
