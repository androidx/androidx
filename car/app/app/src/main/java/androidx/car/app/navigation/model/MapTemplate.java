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
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_PANE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.Screen;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;

import java.util.Objects;

/**
 * A template that displays a map with data such as {@link Pane} on top of it.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The template title has not changed, and the number of rows and the title (not counting
 *       spans) of each row between the previous and new {@link Pane}s have not changed.
 * </ul>
 */
@ExperimentalCarApi
@RequiresCarApi(5)
@CarProtocol
public final class MapTemplate implements Template {
    @Keep
    @Nullable
    private final MapController mMapController;
    @Keep
    @Nullable
    private final Pane mPane;
    @Keep
    @Nullable
    private final Header mHeader;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;

    MapTemplate(Builder builder) {
        mMapController = builder.mMapController;
        mPane = builder.mPane;
        mHeader = builder.mHeader;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MapTemplate() {
        mMapController = null;
        mPane = null;
        mHeader = null;
        mActionStrip = null;
    }

    /**
     * Returns the controls associated with an app-provided map.
     *
     * @see Builder#setMapController
     */
    @Nullable
    public MapController getMapController() {
        return mMapController;
    }

    /**
     * Returns the {@link Pane} to display in this template.
     *
     * @see Builder#setPane(Pane)
     */
    @Nullable
    public Pane getPane() {
        return mPane;
    }

    /**
     * Returns the {@link Header} to display in this template.
     *
     * @see Builder#setHeader(Header)
     */
    @Nullable
    public Header getHeader() {
        return mHeader;
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

    @Override
    public int hashCode() {
        return Objects.hash(mMapController, mPane, mHeader, mActionStrip);
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
                && Objects.equals(mHeader, otherTemplate.mHeader)
                && Objects.equals(mMapController, otherTemplate.mMapController)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    /** A builder of {@link MapTemplate}. */
    public static final class Builder {
        @Nullable
        MapController mMapController;
        @Nullable
        Pane mPane;
        @Nullable
        Header mHeader;
        @Nullable
        ActionStrip mActionStrip;

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
         * Sets the {@link Pane} for this template.
         *
         * @throws NullPointerException if {@code pane} is null
         */
        @NonNull
        public Builder setPane(@NonNull Pane pane) {
            mPane = requireNonNull(pane);
            return this;
        }

        /**
         * Sets the {@link Header} for this template.
         *
         * @throws NullPointerException if {@code header} is null
         */
        @NonNull
        public Builder setHeader(@NonNull Header header) {
            mHeader = requireNonNull(header);
            return this;
        }

        /**
         * Sets the {@link MapController} for this template.
         */
        @NonNull
        public Builder setMapController(@NonNull MapController mapController) {
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
         * @throws IllegalArgumentException if the {@link Pane} does not meet the requirements
         * @throws NullPointerException     if {@link Header} or {@link Pane} is null
         */
        @NonNull
        public MapTemplate build() {
            if (mPane == null) {
                throw new IllegalStateException("Pane must be set");
            }
            if (mHeader == null) {
                throw new IllegalStateException("Header must be set");
            }

            ROW_LIST_CONSTRAINTS_PANE.validateOrThrow(mPane);
            ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION.validateOrThrow(mPane.getActions());

            return new MapTemplate(this);
        }
    }
}
