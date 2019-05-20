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
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration of a single lane of a road at a particular point in the navigation. It describes
 * all possible directions the driver could go from this lane, and indicates which directions the
 * driver could take to stay on the navigation route.
 */
@VersionedParcelize
public final class Lane implements VersionedParcelable {
    @ParcelField(1)
    List<LaneDirection> mDirections;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Lane() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Lane(@NonNull List<LaneDirection> directions) {
        mDirections = new ArrayList<>(directions);
    }

    /**
     * Builder for creating a {@link Lane}
     */
    public static final class Builder {
        private List<LaneDirection> mDirections = new ArrayList<>();

        /**
         * Add a possible direction a driver can take from this lane.
         */
        @NonNull
        public Builder addDirection(@NonNull LaneDirection direction) {
            mDirections.add(Preconditions.checkNotNull(direction));
            return this;
        }

        /**
         * Returns a {@link Lane} built with the provided information.
         */
        @NonNull
        public Lane build() {
            return new Lane(mDirections);
        }
    }

    /**
     * Returns an unmodifiable list of possible directions a driver can take from this lane.
     */
    @NonNull
    public List<LaneDirection> getDirections() {
        return Common.immutableOrEmpty(mDirections);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Lane lane = (Lane) o;
        return Objects.equals(getDirections(), lane.getDirections());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDirections());
    }

    @Override
    public String toString() {
        return String.format("{direction: %s}", mDirections);
    }
}
