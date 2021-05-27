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

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.navigation.model.NavigationTemplate.NavigationInfo;

import java.util.Objects;

/**
 * Represents routing information that can be shown in the {@link NavigationTemplate} during
 * navigation
 */
@CarProtocol
public final class RoutingInfo implements NavigationInfo {
    @Keep
    @Nullable
    private final Step mCurrentStep;
    @Keep
    @Nullable
    private final Distance mCurrentDistance;
    @Keep
    @Nullable
    private final Step mNextStep;
    @Keep
    @Nullable
    private final CarIcon mJunctionImage;
    @Keep
    private final boolean mIsLoading;

    /**
     * Returns whether the routing info is in a loading state.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the current step to display to the user or {@code null} if not set.
     *
     * @see Builder#setCurrentStep(Step, Distance)
     */
    @Nullable
    public Step getCurrentStep() {
        return mCurrentStep;
    }

    /**
     * Returns the current distance to display to the user or {@code null} if not set.
     *
     * @see Builder#setCurrentStep(Step, Distance)
     */
    @Nullable
    public Distance getCurrentDistance() {
        return mCurrentDistance;
    }

    @Nullable
    public Step getNextStep() {
        return mNextStep;
    }


    /**
     * Returns an image for a junction of the maneuver or {@code null} if not set.
     *
     * @see Builder#setJunctionImage(CarIcon)
     */
    @Nullable
    public CarIcon getJunctionImage() {
        return mJunctionImage;
    }

    @NonNull
    @Override
    public String toString() {
        return "RoutingInfo";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCurrentStep, mCurrentDistance, mNextStep, mJunctionImage, mIsLoading);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RoutingInfo)) {
            return false;
        }
        RoutingInfo otherInfo = (RoutingInfo) other;

        return mIsLoading == otherInfo.mIsLoading
                && Objects.equals(mCurrentStep, otherInfo.mCurrentStep)
                && Objects.equals(mCurrentDistance, otherInfo.mCurrentDistance)
                && Objects.equals(mNextStep, otherInfo.mNextStep)
                && Objects.equals(mJunctionImage, otherInfo.mJunctionImage);
    }

    RoutingInfo(Builder builder) {
        mCurrentStep = builder.mCurrentStep;
        mCurrentDistance = builder.mCurrentDistance;
        mNextStep = builder.mNextStep;
        mJunctionImage = builder.mJunctionImage;
        mIsLoading = builder.mIsLoading;
    }

    /** Constructs an empty instance, used by serialization code. */
    private RoutingInfo() {
        mCurrentStep = null;
        mCurrentDistance = null;
        mNextStep = null;
        mJunctionImage = null;
        mIsLoading = false;
    }

    /** A builder of {@link RoutingInfo}. */
    public static final class Builder {
        @Nullable
        Step mCurrentStep;
        @Nullable
        Distance mCurrentDistance;
        @Nullable
        Step mNextStep;
        @Nullable
        CarIcon mJunctionImage;
        boolean mIsLoading;

        /**
         * Sets the current {@link Step} and {@link Distance} to display in the template.
         *
         * <p>A {@link Step} with a {@link Maneuver} of type {@link Maneuver#TYPE_UNKNOWN} will
         * shown here with the given icon.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * Images in the cue of the {@link Step} object, set with {@link Step.Builder#setCue}, can
         * contain image spans. To minimize scaling artifacts across a wide range of car screens,
         * apps should provide images targeting a 216 x 72 dp bounding box. If necessary, those
         * images in the spans will be scaled down to fit the bounding box while preserving their
         * aspect ratios.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code currentStep} is {@code null}
         * @throws NullPointerException if {@code currentDistance} is {@code null}
         */
        @NonNull
        public Builder setCurrentStep(@NonNull Step currentStep,
                @NonNull Distance currentDistance) {
            mCurrentStep = requireNonNull(currentStep);
            mCurrentDistance = requireNonNull(currentDistance);
            return this;
        }

        /**
         * Sets the next {@link Step}.
         *
         * <p>Unless set with this method, the next step won't be displayed.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * Images in the cue of the {@link Step} object, set with {@link Step.Builder#setCue}, can
         * contain image spans. To minimize scaling artifacts across a wide range of car screens,
         * apps should provide images targeting a 216 x 72 dp bounding box. If necessary, those
         * images in the spans will be scaled down to fit the bounding box while preserving their
         * aspect ratios.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code nextStep} is {@code null}
         */
        @NonNull
        public Builder setNextStep(@NonNull Step nextStep) {
            mNextStep = requireNonNull(nextStep);
            return this;
        }

        /**
         * Sets an image of a junction for the maneuver.
         *
         * <p>For example, a photo-realistic view of the upcoming junction that the driver can
         * see when executing the maneuver.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * images targeting a 500 x 312 dp bounding box. If the image exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio. The aspect ratio should be greater than
         * or equal to 1.6 in order to fit the horizontal space fully.
         *
         * <p>On smaller screens the junction image may result in the hiding of the {@link Lane}
         * s, {@link TravelEstimate} or next {@link Step}.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code junctionImage} is {@code null}
         */
        @NonNull
        public Builder setJunctionImage(@NonNull CarIcon junctionImage) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(junctionImage));
            mJunctionImage = junctionImage;
            return this;
        }

        /**
         * Sets whether the {@link RoutingInfo} is in a loading state.
         *
         * <p>If set to {@code true}, the UI will show a loading indicator, and adding any other
         * routing info will throw an {@link IllegalArgumentException}. The caller is expected to
         * call {@link androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready. If set to {@code false}, the UI shows the actual
         * routing info.
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Constructs the {@link RoutingInfo} defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * The {@link RoutingInfo} can be in a loading state by passing {@code true} to {@link
         * #setLoading(boolean)}, in which case no other fields may be set. Otherwise, the current
         * step and distance must be set. If the lane information is set with {@link
         * Step.Builder#addLane(Lane)}, then the lane image must also be set with {@link
         * Step.Builder#setLanesImage(CarIcon)}.
         *
         * @throws IllegalStateException if the {@link RoutingInfo} does not meet the template's
         *                               requirements
         */
        @NonNull
        public RoutingInfo build() {
            Step current = mCurrentStep;
            Distance distance = mCurrentDistance;

            if (mIsLoading) {
                if (current != null || distance != null || mNextStep != null
                        || mJunctionImage != null) {
                    throw new IllegalStateException(
                            "The routing info is set to loading but is not empty");
                }
            } else {
                if (current == null || distance == null) {
                    throw new IllegalStateException(
                            "Current step and distance must be set during the navigating state");
                }
                if (!current.getLanes().isEmpty() && current.getLanesImage() == null) {
                    // TODO(b/154660041): Remove restriction when lane image can be draw from
                    // lane info.
                    throw new IllegalStateException(
                            "Current step must have a lanes image if the lane information is set");
                }
            }
            return new RoutingInfo(this);
        }

        /** Constructs an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
