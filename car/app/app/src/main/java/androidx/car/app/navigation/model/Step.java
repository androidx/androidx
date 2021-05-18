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
@CarProtocol
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
     * Returns the maneuver to be performed on this step or {@code null} if this step doesn't
     * involve a maneuver.
     *
     * @see Builder#setManeuver(Maneuver)
     */
    @Nullable
    public Maneuver getManeuver() {
        return mManeuver;
    }

    /**
     * Returns a list of {@link Lane} that contains information of the road lanes at the point
     * where the driver should execute this step.
     *
     * @see Builder#addLane(Lane)
     */
    @NonNull
    public List<Lane> getLanes() {
        return CollectionUtils.emptyIfNull(mLanes);
    }

    /**
     * Returns the image representing all the lanes or {@code null} if not set.
     *
     * @see Builder#setLanesImage(CarIcon)
     */
    @Nullable
    public CarIcon getLanesImage() {
        return mLanesImage;
    }

    /**
     * Returns the text description of this maneuver or {@code null} if not set.
     *
     * @see Builder#setCue(CharSequence)
     */
    @Nullable
    public CarText getCue() {
        return mCue;
    }

    /**
     * Returns the text description of the road for the step or {@code null} if unknown.
     *
     * @see Builder#setRoad(CharSequence)
     */
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

    Step(
            @Nullable Maneuver maneuver,
            List<Lane> lanes,
            @Nullable CarIcon lanesImage,
            @Nullable CarText cue,
            @Nullable CarText road) {
        mManeuver = maneuver;
        mLanes = CollectionUtils.unmodifiableCopy(lanes);
        mLanesImage = lanesImage;
        mCue = cue;
        mRoad = road;
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
        @Nullable
        private CarText mCue;
        @Nullable
        private CarText mRoad;

        /**
        * Constructs a new builder of {@link Step}.
        */
        public Builder() {
        }

        /**
         * Constructs a new builder of {@link Step} with a cue.
         *
         * <p>A cue can be used as a fallback when {@link Maneuver} is not set or is unavailable.
         *
         * <p>Some cluster displays do not support UTF-8 encoded characters, in which case
         * unsupported characters will not be displayed properly.
         *
         * <p>See {@link Builder#setCue} for details on span support in the input string.
         *
         * @throws NullPointerException if {@code cue} is {@code null}
         * @see Builder#setCue(CharSequence)
         */
        public Builder(@NonNull CharSequence cue) {
            mCue = CarText.create(requireNonNull(cue));
        }

        /**
         * Constructs a new builder of {@link Step} with a cue, with support for multiple length
         * variants.
         *
         * <p>See {@link Builder#setCue} for details on span support in the input string.
         *
         * @throws NullPointerException if {@code cue} is {@code null}
         * @see Builder#Builder(CharSequence)
         */
        public Builder(@NonNull CarText cue) {
            mCue = requireNonNull(cue);
        }

        /**
         * Sets the maneuver to be performed on this step.
         *
         * @throws NullPointerException if {@code maneuver} is {@code null}
         */
        @NonNull
        public Builder setManeuver(@NonNull Maneuver maneuver) {
            mManeuver = requireNonNull(maneuver);
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
         *
         * @throws NullPointerException if {@code lane} is {@code null}
         */
        @NonNull
        public Builder addLane(@NonNull Lane lane) {
            mLanes.add(requireNonNull(lane));
            return this;
        }

        /**
         * Sets an image representing all the lanes.
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
         *
         * @throws NullPointerException if {@code lanesImage} is {@code null}
         */
        @NonNull
        public Builder setLanesImage(@NonNull CarIcon lanesImage) {
            mLanesImage = requireNonNull(lanesImage);
            return this;
        }

        /**
         * Sets a text description of this maneuver.
         *
         * <p>A cue can be used as a fallback when {@link Maneuver} is not set or is unavailable.
         *
         * <p>For example "Turn left", "Make a U-Turn", "Sharp Right", or "Take the exit using
         * the left lane"
         *
         * <p>The {@code cue} string can contain images that replace spans of text by using {@link
         * androidx.car.app.model.CarIconSpan}. All other spans types are not supported and will be
         * ignored.
         *
         * <p>In the following example, the "520" text is replaced with an icon:
         *
         * <pre>{@code
         * SpannableString string = new SpannableString("Turn right on 520 East");
         * string.setSpan(textWithImage.setSpan(
         *     CarIconSpan.create(new CarIcon.Builder(
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
         * @see CarText
         */
        @NonNull
        public Builder setCue(@NonNull CharSequence cue) {
            mCue = CarText.create(requireNonNull(cue));
            return this;
        }

        /**
         * Sets a text description of the road for the step.
         *
         * <p>This value is primarily used for vehicle cluster and heads-up displays and may not
         * appear in the navigation template.
         *
         * <p>For example, a {@link Step} for a left turn might provide "State Street" for the road.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code destinations} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setRoad(@NonNull CharSequence road) {
            mRoad = CarText.create(requireNonNull(road));
            return this;
        }

        /**
         * Constructs the {@link Step} defined by this builder.
         *
         * @throws IllegalStateException if {@code lanesImage} was set but no lanes were added
         */
        @NonNull
        public Step build() {
            if (mLanesImage != null && mLanes.isEmpty()) {
                throw new IllegalStateException(
                        "A step must have lane data when the lanes image is set");
            }
            return new Step(mManeuver, mLanes, mLanesImage, mCue, mRoad);
        }
    }
}
