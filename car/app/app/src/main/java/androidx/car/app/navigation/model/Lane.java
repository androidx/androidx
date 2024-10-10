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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration of a single lane of a road at a particular point in the navigation.
 *
 * <p>A {@link Lane} object describes all possible directions the driver could go from this lane,
 * and indicates which directions the driver could take to stay on the navigation route.
 */
@CarProtocol
@KeepFields
public final class Lane {
    private final List<LaneDirection> mDirections;

    /**
     * Returns the list of directions a driver can take from this {@link Lane}.
     *
     * @see Builder#addDirection(LaneDirection)
     */
    public @NonNull List<LaneDirection> getDirections() {
        return CollectionUtils.emptyIfNull(mDirections);
    }

    @Override
    public @NonNull String toString() {
        return "[direction count: " + (mDirections != null ? mDirections.size() : 0) + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mDirections);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Lane)) {
            return false;
        }

        Lane otherLane = (Lane) other;
        return Objects.equals(mDirections, otherLane.mDirections);
    }

    Lane(List<LaneDirection> directions) {
        mDirections = CollectionUtils.unmodifiableCopy(directions);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Lane() {
        mDirections = Collections.emptyList();
    }

    /** A builder of {@link Lane}. */
    public static final class Builder {
        private final List<LaneDirection> mDirections = new ArrayList<>();

        /**
         * Adds a direction a driver can take from this lane.
         *
         * @throws NullPointerException if {@code direction} is {@code null}
         */
        public @NonNull Builder addDirection(@NonNull LaneDirection direction) {
            mDirections.add(requireNonNull(direction));
            return this;
        }

        /** Constructs the {@link Lane} defined by this builder. */
        public @NonNull Lane build() {
            return new Lane(mDirections);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
