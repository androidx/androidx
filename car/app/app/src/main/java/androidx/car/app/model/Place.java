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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;

import java.util.Objects;

/** Represents a geographical location and additional information on how to display it. */
@CarProtocol
public final class Place {
    @Keep
    @Nullable
    private final CarLocation mLocation;
    @Keep
    @Nullable
    private final PlaceMarker mMarker;

    /**
     * Returns the {@link PlaceMarker} object associated with this place or {@code null} if one
     * is not set.
     *
     * @see Builder#setMarker(PlaceMarker)
     */
    @Nullable
    public PlaceMarker getMarker() {
        return mMarker;
    }

    /**
     * Returns the {@link CarLocation} instance associated with this place.
     *
     * @see Builder#Builder(CarLocation)
     */
    @NonNull
    public CarLocation getLocation() {
        return requireNonNull(mLocation);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ location: " + mLocation + ", marker: " + mMarker + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLocation, mMarker);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Place)) {
            return false;
        }
        Place otherPlace = (Place) other;

        return Objects.equals(mLocation, otherPlace.mLocation) && Objects.equals(mMarker,
                otherPlace.mMarker);
    }

    Place(Builder builder) {
        mLocation = builder.mLocation;
        mMarker = builder.mMarker;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Place() {
        mLocation = null;
        mMarker = null;
    }

    /** A builder of {@link Place}. */
    public static final class Builder {
        CarLocation mLocation;
        @Nullable
        PlaceMarker mMarker;

        /**
         * Returns a builder instance for a {@link CarLocation}.
         *
         * @param location the geographical location associated with the place
         * @throws NullPointerException if {@code location} is {@code null}
         */
        public Builder(@NonNull CarLocation location) {
            mLocation = Objects.requireNonNull(location);
        }

        /**
         * Sets the {@link PlaceMarker} that specifies how this place is to be displayed on a
         * map.
         *
         * <p>Unless set with this method, the place will not have a marker.
         *
         * @throws NullPointerException if {@code marker} is {@code null}
         */
        @NonNull
        public Builder setMarker(@NonNull PlaceMarker marker) {
            mMarker = requireNonNull(marker);
            return this;
        }

        /**
         * Returns a {@link Builder} instance with the same data as the given {@link Place}
         * instance.
         */
        public Builder(@NonNull Place place) {
            requireNonNull(place);
            mLocation = place.getLocation();
            mMarker = place.getMarker();
        }

        /** Constructs the {@link Place} defined by this builder. */
        @NonNull
        public Place build() {
            return new Place(this);
        }
    }
}
