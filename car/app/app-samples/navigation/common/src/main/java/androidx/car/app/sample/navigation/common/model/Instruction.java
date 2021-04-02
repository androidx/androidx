/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.navigation.common.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;

/**
 * Container for scripting navigation instructions.
 *
 * <p>Each {@link Instruction} represents a change (delta) in the state of navigation corresponding
 * to a change in state that could occur during a real navigation session. In general a script
 * follows the following rough outline similar to a driving sessions:
 *
 * <ul>
 *   <li>Start Navigation
 *   <li>Add one or more destinations (as would be selected by a driver)
 *   <li>Add one or more steps (as would be provided by a routing algorithm)
 *   <li>Add updated positions until the next step is reached.
 *   <li>pop a step and send positions until the next step is reached.
 *   <li>Repeat until all steps are popped.
 *   <li>End Navigation.
 * </ul>
 *
 * <p>In addition to a {@link Type}, each instruction specifies the duration until the next
 * instruction should be executed. A duration of zero allows multiple instructions to be stacked up
 * as if they were a single instruction. All other parameters are optional and related directly to
 * which instruction is specified.
 */
public class Instruction {

    public enum Type {
        START_NAVIGATION,
        END_NAVIGATION,
        ADD_DESTINATION_NAVIGATION,
        POP_DESTINATION_NAVIGATION,
        ADD_STEP_NAVIGATION,
        POP_STEP_NAVIGATION,
        SET_REROUTING,
        SET_ARRIVED,
        SET_TRIP_POSITION_NAVIGATION,
    }

    ;

    private final Type mType;
    private final long mDurationMillis;

    // Only support a single destination at the moment.
    @Nullable
    private final Destination mDestination;

    // Only support setting a single step.
    @Nullable
    private final Step mStep;
    @Nullable
    private final Distance mStepRemainingDistance;
    @Nullable
    private final TravelEstimate mStepTravelEstimate;
    @Nullable
    private final TravelEstimate mDestinationTravelEstimate;
    @Nullable
    private final String mRoad;
    private final boolean mShouldShowNextStep;
    private final boolean mShouldShowLanes;
    @Nullable
    private final CarIcon mJunctionImage;

    @Nullable
    private final String mNotificationTitle;
    @Nullable
    private final String mNotificationContent;
    private final int mNotificationIcon;
    private final boolean mShouldNotify;

    /** Constructs a new builder of {@link Instruction}. */
    @NonNull
    public static Builder builder(@NonNull Type type, long lengthMs) {
        return new Builder(type, lengthMs);
    }

    public long getDurationMillis() {
        return mDurationMillis;
    }

    @NonNull
    public Type getType() {
        return mType;
    }

    @Nullable
    public Destination getDestination() {
        return mDestination;
    }

    @Nullable
    public Step getStep() {
        return mStep;
    }

    @Nullable
    public Distance getStepRemainingDistance() {
        return mStepRemainingDistance;
    }

    @Nullable
    public TravelEstimate getStepTravelEstimate() {
        return mStepTravelEstimate;
    }

    @Nullable
    public TravelEstimate getDestinationTravelEstimate() {
        return mDestinationTravelEstimate;
    }

    @Nullable
    public String getRoad() {
        return mRoad;
    }

    public boolean getShouldShowNextStep() {
        return mShouldShowNextStep;
    }

    public boolean getShouldShowLanes() {
        return mShouldShowLanes;
    }

    @Nullable
    public CarIcon getJunctionImage() {
        return mJunctionImage;
    }

    @Nullable
    public String getNotificationTitle() {
        return mNotificationTitle;
    }

    @Nullable
    public String getNotificationContent() {
        return mNotificationContent;
    }

    public int getNotificationIcon() {
        return mNotificationIcon;
    }

    public boolean getShouldNotify() {
        return mShouldNotify;
    }

    Instruction(Builder builder) {
        mType = builder.mType;
        mDurationMillis = builder.mDurationMillis;
        mDestination = builder.mDestination;
        mStep = builder.mStep;
        mStepRemainingDistance = builder.mStepRemainingDistance;
        mStepTravelEstimate = builder.mStepTravelEstimate;
        mDestinationTravelEstimate = builder.mDestinationTravelEstimate;
        mRoad = builder.mRoad;
        mShouldShowNextStep = builder.mShouldShowNextStep;
        mShouldShowLanes = builder.mShouldShowLanes;
        mJunctionImage = builder.mJunctionImage;
        mNotificationTitle = builder.mNotificationTitle;
        mNotificationContent = builder.mNotificationContent;
        mNotificationIcon = builder.mNotificationIcon;
        mShouldNotify = builder.mShouldNotify;
    }

    /** Builder for creating an {@link Instruction}. */
    public static final class Builder {
        Type mType;
        long mDurationMillis;
        @Nullable
        Destination mDestination;
        @Nullable
        Step mStep;
        @Nullable
        Distance mStepRemainingDistance;
        @Nullable
        TravelEstimate mStepTravelEstimate;
        @Nullable
        TravelEstimate mDestinationTravelEstimate;
        @Nullable
        String mRoad;
        boolean mShouldShowNextStep;
        boolean mShouldShowLanes;
        @Nullable
        CarIcon mJunctionImage;

        @Nullable
        String mNotificationTitle;
        @Nullable
        String mNotificationContent;
        int mNotificationIcon;
        boolean mShouldNotify;

        public Builder(@NonNull Type type, long durationMillis) {
            mType = type;
            mDurationMillis = durationMillis;
        }

        Builder setDestination(@Nullable Destination destination) {
            mDestination = destination;
            return this;
        }

        Builder setStep(@Nullable Step step) {
            mStep = step;
            return this;
        }

        Builder setStepRemainingDistance(@Nullable Distance stepRemainingDistance) {
            mStepRemainingDistance = stepRemainingDistance;
            return this;
        }

        Builder setStepTravelEstimate(@Nullable TravelEstimate stepTravelEstimate) {
            mStepTravelEstimate = stepTravelEstimate;
            return this;
        }

        Builder setDestinationTravelEstimate(@Nullable TravelEstimate destinationTravelEstimate) {
            mDestinationTravelEstimate = destinationTravelEstimate;
            return this;
        }

        Builder setRoad(@Nullable String road) {
            mRoad = road;
            return this;
        }

        Builder setShouldShowNextStep(boolean shouldShowNextStep) {
            mShouldShowNextStep = shouldShowNextStep;
            return this;
        }

        Builder setShouldShowLanes(boolean shouldShowLanes) {
            mShouldShowLanes = shouldShowLanes;
            return this;
        }

        Builder setJunctionImage(@Nullable CarIcon junctionImage) {
            mJunctionImage = junctionImage;
            return this;
        }

        Builder setNotification(
                boolean shouldNotify,
                @Nullable String notificationTitle,
                @Nullable String notificationContent,
                int notificationIcon) {
            mNotificationTitle = notificationTitle;
            mNotificationContent = notificationContent;
            mShouldNotify = shouldNotify;
            mNotificationIcon = notificationIcon;
            return this;
        }

        /** Constructs the {@link Instruction} defined by this builder. */
        @NonNull
        public Instruction build() {
            return new Instruction(this);
        }
    }
}
