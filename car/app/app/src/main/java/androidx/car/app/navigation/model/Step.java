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
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a step that the driver should take in order to remain on the current navigation route.
 *
 * <p>Example of steps are turning onto a street, taking a highway exit and merging onto a different
 * highway, or continuing straight through a roundabout.
 */
public final class Step {
    @Keep
    @Nullable
    private final Maneuver mManeuver;
    @Keep
    private final List<Lane> mLanes;
    @Keep
    @Nullable
    private final CarIcon mLanesImage;
    @Keep
    @Nullable
    private final CarText mCue;
    @Keep
    @Nullable
    private final CarText mRoad;

    /**
     * Constructs a new builder of {@link Step} with a cue.
     *
     * <p>A cue must always be set when the step is created and is used as a fallback when {@link
     * Maneuver} is not set or is unavailable.
     *
     * <p>Some cluster displays do not support UTF-8 encoded characters, in which case unsupported
     * characters will not be displayed properly.
     *
     * @throws NullPointerException if {@code cue} is {@code null}.
     * @see Builder#setCue(CharSequence)
     */
    @NonNull
    public static Builder builder(@NonNull CharSequence cue) {
        return new Builder(requireNonNull(cue));
    }

    /**
     * Returns a new {@link Builder} instance configured with the same data as this {@link Step}
     * instance.
     */
    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    public Maneuver getManeuver() {
        return mManeuver;
    }

    @NonNull
    public List<Lane> getLanes() {
        return CollectionUtils.emptyIfNull(mLanes);
    }

    @Nullable
    public CarIcon getLanesImage() {
        return mLanesImage;
    }

    @Nullable
    public CarText getCue() {
        return mCue;
    }

    @Nullable
    public CarText getRoad() {
        return mRoad;
    }

    @Override
    @NonNull
    public String toString() {
        return "[maneuver: "
                + mManeuver
                + ", lane count: "
                + (mLanes != null ? mLanes.size() : 0)
                + ", lanes image: "
                + mLanesImage
                + ", cue: "
                + CarText.toShortString(mCue)
                + ", road: "
                + CarText.toShortString(mRoad)
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mManeuver, mLanes, mLanesImage, mCue, mRoad);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Step)) {
            return false;
        }

        Step otherStep = (Step) other;
        return Objects.equals(mManeuver, otherStep.mManeuver)
                && Objects.equals(mLanes, otherStep.mLanes)
                && Objects.equals(mLanesImage, otherStep.mLanesImage)
                && Objects.equals(mCue, otherStep.mCue)
                && Objects.equals(mRoad, otherStep.mRoad);
    }

    private Step(
            @Nullable Maneuver maneuver,
            List<Lane> lanes,
            @Nullable CarIcon lanesImage,
            @Nullable CarText cue,
            @Nullable CarText road) {
        this.mManeuver = maneuver;
        this.mLanes = new ArrayList<>(lanes);
        this.mLanesImage = lanesImage;
        this.mCue = cue;
        this.mRoad = road;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Step() {
        mManeuver = null;
        mLanes = Collections.emptyList();
        mLanesImage = null;
        mCue = null;
        mRoad = null;
    }

    /** A builder of {@link Step}. */
    public static final class Builder {
        private final List<Lane> mLanes = new ArrayList<>();
        @Nullable
        private Maneuver mManeuver;
        @Nullable
        private CarIcon mLanesImage;
        private CarText mCue;
        @Nullable
        private CarText mRoad;

        private Builder(CharSequence cue) {
            this.mCue = CarText.create(cue);
        }

        private Builder(Step step) {
            this.mManeuver = step.mManeuver;
            this.mLanes.clear();
            this.mLanes.addAll(step.mLanes);
            this.mLanesImage = step.mLanesImage;
            this.mCue = requireNonNull(step.mCue);
            this.mRoad = step.mRoad;
        }

        /**
         * Sets the maneuver to be performed on this step or {@code null} if this step doesn't
         * involve a
         * maneuver.
         */
        @NonNull
        public Builder setManeuver(@Nullable Maneuver maneuver) {
            this.mManeuver = maneuver;
            return this;
        }

        /**
         * Adds the information of a single road lane at the point where the driver should
         * execute this step.
         *
         * <p>Lane information is primarily used when the step is passed to the vehicle cluster
         * or heads up displays. Some vehicles may not use the information at all. The navigation
         * template primarily uses the lanes image provided in {@link #setLanesImage}.
         *
         * <p>Lanes are displayed from left to right.
         */
        @NonNull
        public Builder addLane(@NonNull Lane lane) {
            requireNonNull(lane);
            mLanes.add(lane);
            return this;
        }

        /**
         * Clears any lanes that may have been added with {@link #addLane(Lane)} up to this
         * point.
         */
        @NonNull
        public Builder clearLanes() {
            mLanes.clear();
            return this;
        }

        /**
         * Sets an image representing all the lanes or {@code null} if no lanes image is available.
         *
         * <p>This image takes priority over {@link Lane}s that may have been added with {@link
         * #addLane}. If an image is added for the lanes with this method then corresponding lane
         * data using {@link #addLane} must also have been added in case it is shown on a display
         * with limited resources such as the car cluster or heads-up display (HUD).
         *
         * <p>This image should ideally have a transparent background.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * The provided image should have a maximum size of 294 x 44 dp. If the image exceeds this
         * maximum size in either one of the dimensions, it will be scaled down and centered
         * inside the bounding box while preserving the aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         */
        @NonNull
        public Builder setLanesImage(@Nullable CarIcon lanesImage) {
            this.mLanesImage = lanesImage;
            return this;
        }

        /**
         * Sets a text description of this maneuver.
         *
         * <p>Must always be set when the step is created and is used as a fallback when {@link
         * Maneuver} is not set or is unavailable.
         *
         * <p>For example "Turn left", "Make a U-Turn", "Sharp Right", or "Take the exit using
         * the left lane"
         *
         * <p>The {@code cue} string can contain images that replace spans of text by using {@link
         * androidx.car.app.model.CarIconSpan}.
         *
         * <p>In the following example, the "520" text is replaced with an icon:
         *
         * <pre>{@code
         * SpannableString string = new SpannableString("Turn right on 520 East");
         * string.setSpan(textWithImage.setSpan(
         *     CarIconSpan.create(CarIcon.of(
         *         IconCompat.createWithResource(getCarContext(), R.drawable.ic_520_highway))),
         *         14, 17, SPAN_INCLUSIVE_EXCLUSIVE));
         * }</pre>
         *
         * <p>The host may choose to display the string without the images, so it is important
         * that the string content is readable without the images. This may be the case, for
         * example, if the string is sent to a cluster display that does not support images, or
         * if the host limits the number of images that may be allowed for one line of text.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * The size these images will be displayed at varies depending on where the {@link Step}
         * object is used. Refer to the documentation of those APIs for details.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code cue} is {@code null}
         */
        @NonNull
        public Builder setCue(@NonNull CharSequence cue) {
            this.mCue = CarText.create(requireNonNull(cue));
            return this;
        }

        /**
         * Sets a text description of the road for the step or {@code null} if unknown.
         *
         * <p>This value is primarily used for vehicle cluster and heads-up displays and may not
         * appear
         * in the navigation template.
         *
         * <p>For example, a {@link Step} for a left turn might provide "State Street" for the road.
         *
         * @throws NullPointerException if {@code destinations} is {@code null}
         */
        @NonNull
        public Builder setRoad(@NonNull CharSequence road) {
            this.mRoad = CarText.create(requireNonNull(road));
            return this;
        }

        /**
         * Constructs the {@link Step} defined by this builder.
         *
         * @throws IllegalStateException if {@code lanesImage} was set but no lanes were added.
         */
        @NonNull
        public Step build() {
            if (mLanesImage != null && mLanes.isEmpty()) {
                throw new IllegalStateException(
                        "A step must have lane data when the lanes image is set.");
            }
            return new Step(mManeuver, mLanes, mLanesImage, mCue, mRoad);
        }
    }
}
