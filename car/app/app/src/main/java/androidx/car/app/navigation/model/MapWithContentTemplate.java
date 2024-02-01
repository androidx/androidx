/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.constraints.ContentTemplateConstraints;

import java.util.Objects;

/**
 * A template that allows an app to render map tiles with some sort of content (for example, a
 * list). The content is usually rendered as an overlay on top of the map tiles, with the map
 * visible and stable areas adjusting to the content.
 *
 * See {@link ContentTemplateConstraints#MAP_WITH_CONTENT_TEMPLATE_CONSTRAINTS}
 * for the list of supported content templates.
 */
@CarProtocol
@KeepFields
@ExperimentalCarApi
@RequiresCarApi(7)
public final class MapWithContentTemplate implements Template {
    @Nullable
    private final MapController mMapController;
    @NonNull
    private final Template mContentTemplate;
    @Nullable
    private final ActionStrip mActionStrip;

    /**
     * Creates a new {@code MapWithContentTemplate}. Please use the {@link Builder} to construct
     * instances of this template.
     */
    MapWithContentTemplate(Builder builder) {
        mMapController = builder.mMapController;
        mContentTemplate = builder.mContentTemplate;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MapWithContentTemplate() {
        mMapController = null;
        mContentTemplate = new Template() {};
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
     * Returns the {@link Template} content to display in this template.
     *
     * @see Builder#setContentTemplate(Template)
     */
    @NonNull
    public Template getContentTemplate() {
        return mContentTemplate;
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
        return Objects.hash(mMapController, mContentTemplate, mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MapWithContentTemplate)) {
            return false;
        }
        MapWithContentTemplate otherTemplate = (MapWithContentTemplate) other;

        return  Objects.equals(mContentTemplate, otherTemplate.mContentTemplate)
                && Objects.equals(mMapController, otherTemplate.mMapController)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    /** A builder of {@link MapWithContentTemplate}. */
    public static final class Builder {

        @Nullable
        MapController mMapController;
        @NonNull
        Template mContentTemplate;
        @Nullable
        ActionStrip mActionStrip;

        public Builder() {
            mContentTemplate = new Template() {};
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
        @NonNull
        public Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_NAVIGATION
                    .validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Sets the content to be displayed on top of the map tiles.
         */
        @NonNull
        public Builder setContentTemplate(@NonNull Template template) {
            mContentTemplate = requireNonNull(template);
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
         * <p>
         * <h4>Requirements</h4>
         * <p>
         * @throws IllegalArgumentException if the template is not one of the allowed Content types
         * see {@link ContentTemplateConstraints#MAP_WITH_CONTENT_TEMPLATE_CONSTRAINTS}
         * for the list of supported content templates.
         */
        @NonNull
        public MapWithContentTemplate build() {
            ContentTemplateConstraints.MAP_WITH_CONTENT_TEMPLATE_CONSTRAINTS
                    .validateOrThrow(mContentTemplate);
            return new MapWithContentTemplate(this);
        }
    }
}
