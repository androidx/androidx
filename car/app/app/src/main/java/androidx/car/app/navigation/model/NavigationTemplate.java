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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Template;

import java.util.Objects;

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
 * {@link androidx.car.app.AppManager#setSurfaceCallback(SurfaceCallback)}.
 *
 * <p>See {@link androidx.car.app.notification.CarAppExtender} for how to show
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
public final class NavigationTemplate implements Template {

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

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    @Nullable
    public ActionStrip getActionStrip() {
        return requireNonNull(mActionStrip);
    }

    /**
     * Returns the navigation information displayed on the template or {@code null} if there is no
     * navigation information on top of the map.
     */
    @Nullable
    public NavigationInfo getNavigationInfo() {
        return mNavigationInfo;
    }

    /**
     * Returns the background color used for the navigation information or {@code null} if set to
     * the default value.
     */
    @Nullable
    public CarColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Returns the {@link TravelEstimate} to the final destination or {@code null} if there is no
     * travel estimate information.
     */
    @Nullable
    public TravelEstimate getDestinationTravelEstimate() {
        return mDestinationTravelEstimate;
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

    NavigationTemplate(Builder builder) {
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
        NavigationInfo mNavigationInfo;
        @Nullable
        CarColor mBackgroundColor;
        @Nullable
        TravelEstimate mDestinationTravelEstimate;
        ActionStrip mActionStrip;

        /**
         * Sets the navigation information to display on the template.
         *
         * <p>Unless set with this method, navigation info won't be displayed on the template.
         *
         * @throws NullPointerException if {@code navigationInfo} is {@code null}
         */
        @NonNull
        public Builder setNavigationInfo(@NonNull NavigationInfo navigationInfo) {
            mNavigationInfo = requireNonNull(navigationInfo);
            return this;
        }

        /**
         * Sets the background color to use for the navigation information.
         *
         * <p>The host may ignore this color and use a default color instead if the color does
         * not pass the contrast requirements.
         */
        @NonNull
        public Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            UNCONSTRAINED.validateOrThrow(requireNonNull(backgroundColor));
            mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the {@link TravelEstimate} to the final destination.
         *
         * @throws IllegalArgumentException if the {@link TravelEstimate}'s remaining time is
         *                                  less than zero
         * @throws NullPointerException     if {@code destinationTravelEstimate} is {@code null}
         */
        @NonNull
        public Builder setDestinationTravelEstimate(
                @NonNull TravelEstimate destinationTravelEstimate) {
            if (requireNonNull(destinationTravelEstimate).getRemainingTimeSeconds() < 0) {
                throw new IllegalArgumentException(
                        "The destination travel estimate's remaining time must be greater or "
                                + "equal to zero");
            }
            mDestinationTravelEstimate = destinationTravelEstimate;
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
         *                                  requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        @NonNull
        public Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_NAVIGATION.validateOrThrow(
                    requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the {@link NavigationTemplate} defined by this builder.
         *
         * @throws IllegalStateException if an {@link ActionStrip} is not set on this template
         */
        @NonNull
        public NavigationTemplate build() {
            if (mActionStrip == null) {
                throw new IllegalStateException("Action strip for this template must be set");
            }
            return new NavigationTemplate(this);
        }

        /** Constructs an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
