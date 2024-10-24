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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_MAP;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_NAVIGATION;
import static androidx.car.app.model.constraints.CarColorConstraints.UNCONSTRAINED;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
 * <h4>Pan and Zoom</h4>
 *
 * This template allows an app to provide pan and zoom functionality. To support pan and zoom,
 * respond to the user input in {@link SurfaceCallback} methods, such as:
 *
 * <ul>
 *     <li>{@link SurfaceCallback#onScroll(float, float)}</li>
 *     <li>{@link SurfaceCallback#onFling(float, float)}</li>
 *     <li>{@link SurfaceCallback#onScale(float, float, float)}</li>
 * </ul>
 *
 * In order to receive the callbacks, add an {@link Action#PAN} button in a map
 * {@link ActionStrip} via the {@link Builder#setMapActionStrip(ActionStrip)} method:
 *
 * <pre>{@code
 * ...
 * Action panAction = new Action.Builder(Action.PAN).setIcon(myPanIcon).build();
 * ActionStrip mapActionStrip = new ActionStrip.Builder().addAction(panAction).build();
 * NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
 * builder.setMapActionStrip(mapActionStrip);
 * ...
 * }</pre>
 *
 * When the user presses the {@link Action#PAN} button, the host enters the pan mode. In this
 * mode, the host translates the user input from non-touch input devices, such as rotary controllers
 * and touchpads, and calls the appropriate {@link SurfaceCallback} methods. Respond to the user
 * action to enter or exit the pan mode via {@link Builder#setPanModeListener(PanModeListener)}.
 *
 * <p>If the app does not include this button in the map {@link ActionStrip}, the app will not
 * receive the user input for panning gestures from {@link SurfaceCallback} methods, and the host
 * will exit any previously activated pan mode.
 *
 * <p>The host may hide the pan button in some head units in which the user does not need it.
 * Also, the host may hide other UI components in the template while the user is in the pan mode.
 *
 * <p>Note that not all head units support touch gestures, and not all touch screens support
 * multi-touch gestures. Therefore, some {@link SurfaceCallback} methods may not be called in
 * some cars. In order to support different head units, use the buttons in the map action strip
 * to provide necessary functionality, such as the zoom-in and zoom-out buttons.
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
@CarProtocol
@KeepFields
public final class NavigationTemplate implements Template {

    /**
     * Represents navigation information such as routing instructions or navigation-related
     * messages.
     */
    public interface NavigationInfo {
    }

    private final @Nullable NavigationInfo mNavigationInfo;
    private final @Nullable CarColor mBackgroundColor;
    private final @Nullable TravelEstimate mDestinationTravelEstimate;
    private final @Nullable ActionStrip mActionStrip;
    private final @Nullable ActionStrip mMapActionStrip;
    private final @Nullable Toggle mPanModeToggle;
    private final @Nullable PanModeDelegate mPanModeDelegate;

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    public @Nullable ActionStrip getActionStrip() {
        return requireNonNull(mActionStrip);
    }

    /**
     * Returns the map {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setMapActionStrip(ActionStrip)
     */
    @RequiresCarApi(2)
    public @Nullable ActionStrip getMapActionStrip() {
        return mMapActionStrip;
    }

    /**
     * Returns whether this template is in the pan mode.
     *
     * @deprecated use {@link #getPanModeDelegate()}
     */
    // TODO(b/187989940): remove after hosts switch over to using getPanModeDelegate/
    @Deprecated
    @RequiresCarApi(2)
    public @Nullable Toggle getPanModeToggle() {
        return mPanModeToggle;
    }

    /**
     * Returns the {@link PanModeDelegate} that should be called when the user interacts with
     * pan mode on this template, or {@code null} if a {@link PanModeListener} was not set.
     */
    @RequiresCarApi(2)
    public @Nullable PanModeDelegate getPanModeDelegate() {
        return mPanModeDelegate;
    }

    /**
     * Returns the navigation information displayed on the template or {@code null} if there is no
     * navigation information on top of the map.
     */
    public @Nullable NavigationInfo getNavigationInfo() {
        return mNavigationInfo;
    }

    /**
     * Returns the background color used for the navigation information or {@code null} if set to
     * the default value.
     */
    public @Nullable CarColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Returns the {@link TravelEstimate} to the final destination or {@code null} if there is no
     * travel estimate information.
     */
    public @Nullable TravelEstimate getDestinationTravelEstimate() {
        return mDestinationTravelEstimate;
    }

    @Override
    public @NonNull String toString() {
        return "NavigationTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNavigationInfo, mBackgroundColor, mDestinationTravelEstimate,
                mActionStrip, mMapActionStrip, mPanModeToggle, mPanModeDelegate == null);
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
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mMapActionStrip, otherTemplate.mMapActionStrip)
                && Objects.equals(mPanModeToggle, otherTemplate.mPanModeToggle)
                && Objects.equals(mPanModeDelegate == null, otherTemplate.mPanModeDelegate == null);
    }

    NavigationTemplate(Builder builder) {
        mNavigationInfo = builder.mNavigationInfo;
        mBackgroundColor = builder.mBackgroundColor;
        mDestinationTravelEstimate = builder.mDestinationTravelEstimate;
        mActionStrip = builder.mActionStrip;
        mMapActionStrip = builder.mMapActionStrip;
        mPanModeToggle = builder.mPanModeToggle;
        mPanModeDelegate = builder.mPanModeDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private NavigationTemplate() {
        mNavigationInfo = null;
        mBackgroundColor = null;
        mDestinationTravelEstimate = null;
        mActionStrip = null;
        mMapActionStrip = null;
        mPanModeToggle = null;
        mPanModeDelegate = null;
    }

    /** A builder of {@link NavigationTemplate}. */
    public static final class Builder {
        @Nullable NavigationInfo mNavigationInfo;
        @Nullable CarColor mBackgroundColor;
        @Nullable TravelEstimate mDestinationTravelEstimate;
        @Nullable ActionStrip mActionStrip;
        @Nullable ActionStrip mMapActionStrip;
        @Nullable Toggle mPanModeToggle;
        @Nullable PanModeDelegate mPanModeDelegate;

        /**
         * Sets the navigation information to display on the template.
         *
         * <p>Unless set with this method, navigation info won't be displayed on the template.
         *
         * @throws NullPointerException if {@code navigationInfo} is {@code null}
         */
        public @NonNull Builder setNavigationInfo(@NonNull NavigationInfo navigationInfo) {
            mNavigationInfo = requireNonNull(navigationInfo);
            return this;
        }

        /**
         * Sets the background color to use for the navigation information.
         *
         * <p>Depending on contrast requirements, capabilities of the vehicle screens, or other
         * factors, the color may be ignored by the host or overridden by the vehicle system.
         */
        public @NonNull Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            UNCONSTRAINED.validateOrThrow(requireNonNull(backgroundColor));
            mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the {@link TravelEstimate} to the final destination.
         *
         * @throws IllegalArgumentException if the {@link TravelEstimate}'s remaining time is less
         *                                  than zero unless the value provided is
         *                                  {@link TravelEstimate#REMAINING_TIME_UNKNOWN}
         * @throws NullPointerException     if {@code destinationTravelEstimate} is {@code null}
         */
        public @NonNull Builder setDestinationTravelEstimate(
                @NonNull TravelEstimate destinationTravelEstimate) {
            if (requireNonNull(destinationTravelEstimate).getRemainingTimeSeconds() < 0
                    && requireNonNull(destinationTravelEstimate).getRemainingTimeSeconds()
                    != TravelEstimate.REMAINING_TIME_UNKNOWN) {
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
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        public @NonNull Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_NAVIGATION.validateOrThrow(
                    requireNonNull(actionStrip).getActions());
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
        @RequiresCarApi(2)
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
        @RequiresCarApi(2)
        public @NonNull Builder setPanModeListener(@NonNull PanModeListener panModeListener) {
            requireNonNull(panModeListener);
            mPanModeToggle =
                    new Toggle.Builder(
                            (isInPanMode) -> panModeListener.onPanModeChanged(isInPanMode)).build();
            mPanModeDelegate = PanModeDelegateImpl.create(panModeListener);
            return this;
        }

        /**
         * Constructs the {@link NavigationTemplate} defined by this builder.
         *
         * @throws IllegalStateException if an {@link ActionStrip} is not set on this template
         */
        public @NonNull NavigationTemplate build() {
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
