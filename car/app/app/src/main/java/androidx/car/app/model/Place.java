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

import java.util.Objects;

/** Represents a geographical location and additional information on how to display it. */
public final class Place {
    @Keep
    @Nullable
    private final CarLocation mLocation;
    @Keep
    @Nullable
    private final PlaceMarker mMarker;

    /**
     * Create a builder for a {@link Place} instance.
     *
     * @param latLng the geographical location associated with the place.
     * @throws NullPointerException if {@code latLng} is {@code null}.
     * @deprecated use {@link Place.Builder#Builder(CarLocation)} instead.
     */
    // TODO(b/175827428): remove once host is changed to use new public ctor.
    @Deprecated
    @NonNull
    public static Builder builder(@NonNull LatLng latLng) {
        return new Builder(requireNonNull(latLng));
    }

    /**
     * Returns a {@link Builder} instance with the same data as this {@link Place} instance.
     * @deprecated use constructor.
     */
    // TODO(b/177484889): remove once host is changed to use new public ctor.
    @NonNull
    @Deprecated
    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    public PlaceMarker getMarker() {
        return mMarker;
    }

    /**
     * @deprecated use {@link #getLocation()} instead.
     */
    // TODO(b/177591131): remove after all host references have been removed.
    @Deprecated
    @NonNull
    public LatLng getLatLng() {
        requireNonNull(mLocation);
        return LatLng.create(mLocation.getLatitude(), mLocation.getLongitude());
    }

    /**
     * @return the {@link CarLocation} set for this Place instance.
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
         * Returns a builder instance for a {@link LatLng}.
         *
         * @param latLng the geographical location associated with the place.
         * @throws NullPointerException if {@code latLng} is {@code null}.
         * @deprecated use {@link #Builder(CarLocation)} instead.
         */
        // TODO(b/177591131): remove after all host references have been removed.
        @Deprecated
        public Builder(@NonNull LatLng latLng) {
            this(CarLocation.create(latLng.getLatitude(), latLng.getLongitude()));
        }

        /**
         * Returns a builder instance for a {@link CarLocation}.
         *
         * @param location the geographical location associated with the place.
         * @throws NullPointerException if {@code location} is {@code null}.
         */
        public Builder(@NonNull CarLocation location) {
            mLocation = Objects.requireNonNull(location);
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

        /**
         * Sets the {@link PlaceMarker} that specifies how this place is to be displayed on a
         * map, or {@code null} to not display a marker for this place.
         *
         * <p>By default and unless otherwise set in this method, a marker will not be displayed.
         */
        @NonNull
        public Builder setMarker(@Nullable PlaceMarker marker) {
            this.mMarker = marker;
            return this;
        }

        /** Constructs the {@link Place} defined by this builder. */
        @NonNull
        public Place build() {
            return new Place(this);
        }
    }
}
