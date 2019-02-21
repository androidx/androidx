/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An action that the driver should take in order to remain on the current navigation route. For
 * example: turning onto a street, taking a highway exit and merging onto a different highway,
 * continuing straight through a roundabout, etc.
 */
@VersionedParcelize
public final class Step implements VersionedParcelable {
    @ParcelField(1)
    Distance mDistance;
    @ParcelField(2)
    Maneuver mManeuver;
    @ParcelField(3)
    List<Lane> mLanes;
    @ParcelField(4)
    ImageReference mLanesImage;
    @ParcelField(5)
    RichText mCue;

    /**
     * Used by {@link VersionedParcelable}

     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Step() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Step(@Nullable Distance distance, @Nullable Maneuver maneuver, @NonNull List<Lane> lanes,
            @Nullable ImageReference lanesImage, @Nullable RichText cue) {
        mDistance = distance;
        mManeuver = maneuver;
        mLanes = new ArrayList<>(lanes);
        mLanesImage = lanesImage;
        mCue = cue;
    }

    /**
     * Builder for creating a {@link Step}
     */
    public static final class Builder {
        private Distance mDistance;
        private Maneuver mManeuver;
        private List<Lane> mLanes = new ArrayList<>();
        private ImageReference mLanesImage;
        private RichText mCue;

        /**
         * Sets the maneuver to be performed on this step, or null if this step doesn't involve a
         * maneuver.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setManeuver(@Nullable Maneuver maneuver) {
            mManeuver = maneuver;
            return this;
        }

        /**
         * Sets the distance from the current position to the point where this navigation step
         * should be executed, or null if distance to this step is not provided.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setDistance(@Nullable Distance distance) {
            mDistance = distance;
            return this;
        }

        /**
         * Adds a road lane configuration to this step. Lanes should be added from left to right.
         * <p>
         * If lanes configuration information is available, producers should provide both image (see
         * {@link #setLanesImage(ImageReference)}) and metadata (through this method) for maximum
         * interoperability, as some consumers might use images while others might use metadata, or
         * both.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder addLane(@NonNull Lane lane) {
            mLanes.add(Preconditions.checkNotNull(lane));
            return this;
        }

        /**
         * Sets a reference to an image that represents a complete lanes configuration at this point
         * in the navigation. The image, if provided, is expected to contain:
         *
         * <ul>
         * <li>A representation of all lanes, one next to the other in a single row.
         * <li>For each lane, a set of arrows, representing each possible driving directions
         * (e.g.: straight, left turn, right turn, etc.) within such lane.
         * <li>Each of such driving directions that would keep the driver within the navigation
         * route should be highlighted.
         * </ul>
         *
         * Lane configuration images are expected to be displayed in a canvas with fixed height and
         * variable width.
         * <p>
         * If lanes configuration information is available, producers should provide both metadata
         * (see {@link #addLane(Lane)}) and image (through this method) for maximum
         * interoperability, as some consumers might use images while others might use metadata, or
         * both.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setLanesImage(@Nullable ImageReference lanesImage) {
            mLanesImage = lanesImage;
            return this;
        }

        /**
         * Sets auxiliary instructions on how complete this navigation step, described as a
         * {@link RichText} object containing a sequence of texts (e.g.: "towards", "Wallaby way")
         * and images (e.g.: road badge of a highway).
         * <p>
         * If consumers don't have enough space to display the complete content of this
         * {@link RichText} instance, it is expected they will truncate these instructions by
         * cutting its end.
         * <p>
         * Because of this, it is expected the most important part of these instructions to be
         * located at the beginning of the sequence.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setCue(@Nullable RichText cue) {
            mCue = cue;
            return this;
        }

        /**
         * Returns a {@link Step} built with the provided information.
         */
        @NonNull
        public Step build() {
            return new Step(mDistance, mManeuver, mLanes, mLanesImage, mCue);
        }
    }

    /**
     * Returns the distance from the current position to the point where this navigation step should
     * be executed, or null if distance to this step was not provided.
     */
    @Nullable
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * Returns the maneuver to be performed on this step, or null if this step doesn't involve a
     * maneuver.
     */
    @Nullable
    public Maneuver getManeuver() {
        return mManeuver;
    }

    /**
     * Returns an unmodifiable list containing the configuration of road lanes at the point where
     * the driver should execute this step, or an empty list if lane configuration metadata is not
     * available. Lane configurations are listed from left to right.
     */
    @NonNull
    public List<Lane> getLanes() {
        return Common.immutableOrEmpty(mLanes);
    }

    /**
     * Returns an image representing the lanes configuration at this point in the navigation, or
     * null if the lanes configuration image was not provided. The image, if provided, is expected
     * to contain:
     *
     * <ul>
     * <li>A representation of all lanes, one next to the other in a single row.
     * <li>For each lane, a set of arrows, representing each possible driving directions
     * (e.g.: straight, left turn, right turn, etc.) within such lane.
     * <li>Each of such driving directions that would keep the driver within the navigation
     * route should be highlighted.
     * </ul>
     *
     * Lane configuration images are expected to be displayed in a canvas with fixed height and
     * variable width.
     */
    @Nullable
    public ImageReference getLanesImage() {
        return mLanesImage;
    }

    /**
     * Returns auxiliary instructions on how complete this navigation step, described as a
     * {@link RichText} object containing a sequence of texts (e.g.: "towards", "Wallaby way")
     * and images (e.g.: road badge of a highway).
     * <p>
     * If space is not enough to display the complete content of this {@link RichText} instance,
     * consumers must display the beginning of these instructions, cutting as much from the end
     * as needed.
     */
    @Nullable
    public RichText getCue() {
        return mCue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Step step = (Step) o;
        return Objects.equals(getManeuver(), step.getManeuver())
                && Objects.equals(getDistance(), step.getDistance())
                && Objects.equals(getLanes(), step.getLanes())
                && Objects.equals(getLanesImage(), step.getLanesImage())
                && Objects.equals(getCue(), step.getCue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getManeuver(), getDistance(), getLanes(), getLanesImage(), getCue());
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s, lanes: %s, lanesImage: %s, cue: %s}",
                mManeuver, mDistance, mLanes, mLanesImage, mCue);
    }
}
