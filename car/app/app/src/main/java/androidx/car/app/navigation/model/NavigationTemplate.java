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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION;
import static androidx.car.app.model.constraints.CarColorConstraints.UNCONSTRAINED;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarAppPermission;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceListener;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Template;

import java.util.Objects;

// TODO(rampara): Update code reference to CarAppExtender is javadoc to link

/**
 * A template for showing navigation information.
 *
 * <p>This template has two independent sections which can be updated:
 *
 * <ul>
 *   <li>Navigation information such as routing instructions or navigation-related messages.
 *   <li>Travel estimates to the destination.
 * </ul>
 *
 * <p>To update the template as the user navigates, call {@link Screen#invalidate} to provide the
 * host with a new template with the updated information.
 *
 * <p>The template itself does not expose a drawing surface. In order to draw on the canvas, use
 * {@link androidx.car.app.AppManager#setSurfaceListener(SurfaceListener)}.
 *
 * <p>See {@code androidx.car.app.notification.CarAppExtender} for how to show
 * alerts with notifications. Frequent alert notifications distract the driver and are discouraged.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regard to template refreshes, as described in {@link Screen#onGetTemplate()}, this template
 * supports any content changes as refreshes. This allows apps to interactively update the
 * turn-by-turn instructions without the templates being counted against the template quota.
 *
 * <p>Further, this template is considered a view that the user will stay and consume contents from,
 * and the host will reset the template quota once an app reaches this template.
 *
 * <p>In order to use this template your car app <b>MUST</b> declare that it uses the {@code
 * androidx.car.app.NAVIGATION_TEMPLATES} permission in the manifest.
 */
public class NavigationTemplate implements Template {

    /**
     * Represents navigation information such as routing instructions or navigation-related
     * messages.
     */
    public interface NavigationInfo {
    }

    @Keep
    @Nullable
    private final NavigationInfo mNavigationInfo;
    @Keep
    @Nullable
    private final CarColor mBackgroundColor;
    @Keep
    @Nullable
    private final TravelEstimate mDestinationTravelEstimate;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;

    /** Constructs a new builder of {@link NavigationTemplate}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public NavigationInfo getNavigationInfo() {
        return mNavigationInfo;
    }

    @Nullable
    public CarColor getBackgroundColor() {
        return mBackgroundColor;
    }

    @Nullable
    public TravelEstimate getDestinationTravelEstimate() {
        return mDestinationTravelEstimate;
    }

    @NonNull
    public ActionStrip getActionStrip() {
        return requireNonNull(mActionStrip);
    }

    @NonNull
    @Override
    public String toString() {
        return "NavigationTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNavigationInfo, mBackgroundColor, mDestinationTravelEstimate,
                mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NavigationTemplate)) {
            return false;
        }
        NavigationTemplate otherTemplate = (NavigationTemplate) other;

        return Objects.equals(mNavigationInfo, otherTemplate.mNavigationInfo)
                && Objects.equals(mBackgroundColor, otherTemplate.mBackgroundColor)
                && Objects.equals(mDestinationTravelEstimate,
                otherTemplate.mDestinationTravelEstimate)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    @Override
    public void checkPermissions(@NonNull Context context) {
        CarAppPermission.checkHasLibraryPermission(context, CarAppPermission.NAVIGATION_TEMPLATES);
    }

    private NavigationTemplate(Builder builder) {
        mNavigationInfo = builder.mNavigationInfo;
        mBackgroundColor = builder.mBackgroundColor;
        mDestinationTravelEstimate = builder.mDestinationTravelEstimate;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private NavigationTemplate() {
        mNavigationInfo = null;
        mBackgroundColor = null;
        mDestinationTravelEstimate = null;
        mActionStrip = null;
    }

    /** A builder of {@link NavigationTemplate}. */
    public static final class Builder {
        @Nullable
        private NavigationInfo mNavigationInfo;
        @Nullable
        private CarColor mBackgroundColor;
        @Nullable
        private TravelEstimate mDestinationTravelEstimate;
        private ActionStrip mActionStrip;

        private Builder() {
        }

        /**
         * Sets the navigation information to display on the template, or {@code null} to not
         * display navigation information on top of the map.
         */
        @NonNull
        public Builder setNavigationInfo(@Nullable NavigationInfo navigationInfo) {
            this.mNavigationInfo = navigationInfo;
            return this;
        }

        /**
         * Sets the background color to use for the navigation information, or {@code null} to
         * use the default.
         *
         * <p>The host may ignore this color and use a default color instead if the color does
         * not pass the contrast requirements.
         */
        @NonNull
        public Builder setBackgroundColor(@Nullable CarColor backgroundColor) {
            if (backgroundColor != null) {
                UNCONSTRAINED.validateOrThrow(backgroundColor);
            }
            this.mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the {@link TravelEstimate} to the final destination, or {@code null} to not show any
         * travel estimate information.
         *
         * @throws IllegalArgumentException if the {@link TravelEstimate}'s remaining time is
         *                                  less than zero.
         */
        @NonNull
        public Builder setDestinationTravelEstimate(
                @Nullable TravelEstimate destinationTravelEstimate) {
            if (destinationTravelEstimate != null
                    && destinationTravelEstimate.getRemainingTimeSeconds() < 0) {
                throw new IllegalArgumentException(
                        "The destination travel estimate's remaining time must be greater or "
                                + "equal to zero");
            }
            this.mDestinationTravelEstimate = destinationTravelEstimate;
            return this;
        }

        /**
         * Sets an {@link ActionStrip} with a list of template-scoped actions for this template.
         *
         * <h4>Requirements</h4>
         *
         * Besides {@link Action#APP_ICON} and {@link Action#BACK}, this template requires at
         * least 1 and up to 4 {@link Action}s in its {@link ActionStrip}. Of the 4 allowed
         * {@link Action}s, only one can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the template's
         *                                  requirements.
         * @throws NullPointerException     if {@code actionStrip} is {@code null}.
         */
        @NonNull
        public Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_NAVIGATION.validateOrThrow(
                    requireNonNull(actionStrip).getActions());
            this.mActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the {@link NavigationTemplate} defined by this builder.
         *
         * @throws IllegalStateException if an {@link ActionStrip} is not set on this template.
         */
        @NonNull
        public NavigationTemplate build() {
            if (mActionStrip == null) {
                throw new IllegalStateException("Action strip for this template must be set.");
            }
            return new NavigationTemplate(this);
        }
    }
}
