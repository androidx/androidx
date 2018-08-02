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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

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

    /**
     * Used by {@link VersionedParcelable}

     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Step() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Step(@Nullable Distance distance, @Nullable Maneuver maneuver) {
        mDistance = distance;
        mManeuver = maneuver;
    }

    /**
     * Builder for creating a {@link Step}
     */
    public static final class Builder {
        Distance mDistance;
        Maneuver mManeuver;

        /**
         * Sets the distance from the current position to the point where this navigation step
         * should be executed, or null if this step doesn't involve a maneuver.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setManeuver(@Nullable Maneuver maneuver) {
            mManeuver = maneuver;
            return this;
        }

        /**
         * Sets the maneuver to be performed on this step, or null if distance to this step is not
         * provided.
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setDistance(@Nullable Distance distance) {
            mDistance = distance;
            return this;
        }

        /**
         * Returns a {@link Step} built with the provided information.
         */
        @NonNull
        public Step build() {
            return new Step(mDistance, mManeuver);
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
                && Objects.equals(getDistance(), step.getDistance());
    }

    @Override
    public String toString() {
        return String.format("{maneuver: %s, distance: %s}", getManeuver(), getDistance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mManeuver, mDistance);
    }
}
