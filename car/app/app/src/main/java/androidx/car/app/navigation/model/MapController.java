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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_MAP;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.SurfaceCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A component that holds onto controls associated with an app-provided provided map tile.
 */
@RequiresCarApi(5)
@CarProtocol
@KeepFields
public final class MapController {
    private final @Nullable PanModeDelegate mPanModeDelegate;
    private final @Nullable ActionStrip mMapActionStrip;

    MapController(Builder builder) {
        mPanModeDelegate = builder.mPanModeDelegate;
        mMapActionStrip = builder.mMapActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MapController() {
        mPanModeDelegate = null;
        mMapActionStrip = null;
    }

    /**
     * Returns the map {@link ActionStrip} for this component (which will be shown on the active
     * template) or {@code null} if not set.
     *
     * @see Builder#setMapActionStrip(ActionStrip)
     */
    public @Nullable ActionStrip getMapActionStrip() {
        return mMapActionStrip;
    }

    /**
     * Returns {@link PanModeDelegate} that should be called when the user interacts with pan mode
     * on the active template.
     *
     * @see Builder#setPanModeListener(PanModeListener)
     */
    public @Nullable PanModeDelegate getPanModeDelegate() {
        return mPanModeDelegate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPanModeDelegate, mMapActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MapController)) {
            return false;
        }
        MapController otherComponent = (MapController) other;

        return Objects.equals(mPanModeDelegate == null, otherComponent.mPanModeDelegate == null)
                && Objects.equals(mMapActionStrip, otherComponent.mMapActionStrip);
    }

    /** A builder of {@link MapController}. */
    public static final class Builder {
        @Nullable PanModeDelegate mPanModeDelegate;
        @Nullable ActionStrip mMapActionStrip;

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
        public @NonNull Builder setPanModeListener(@NonNull PanModeListener panModeListener) {
            requireNonNull(panModeListener);
            mPanModeDelegate = PanModeDelegateImpl.create(panModeListener);
            return this;
        }

        /**
         * Sets an {@link ActionStrip} with a list of map-control related actions for the active
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
         * This component allows up to 4 {@link Action}s in its map {@link ActionStrip}. Only
         * {@link Action}s with icons set via {@link Action.Builder#setIcon} are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the component's
         *                                  requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        public @NonNull Builder setMapActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_MAP.validateOrThrow(requireNonNull(actionStrip).getActions());
            mMapActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the {@link MapController} defined by this builder.
         */
        public @NonNull MapController build() {
            return new MapController(this);
        }
    }
}
