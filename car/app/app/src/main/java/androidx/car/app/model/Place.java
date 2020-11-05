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
public class Place {
    @Keep
    @Nullable
    private final LatLng mLatLng;
    @Keep
    @Nullable
    private final PlaceMarker mMarker;

    /**
     * Create a builder for a {@link Place} instance.
     *
     * @param latLng the geographical location associated with the place.
     * @throws NullPointerException if {@code latLng} is {@code null}.
     */
    @NonNull
    public static Builder builder(@NonNull LatLng latLng) {
        return new Builder(requireNonNull(latLng));
    }

    /** Returns a {@link Builder} instance with the same data as this {@link Place} instance. */
    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    public PlaceMarker getMarker() {
        return mMarker;
    }

    @NonNull
    public LatLng getLatLng() {
        return requireNonNull(mLatLng);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ latlng: " + mLatLng + ", marker: " + mMarker + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLatLng, mMarker);
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

        return Objects.equals(mLatLng, otherPlace.mLatLng) && Objects.equals(mMarker,
                otherPlace.mMarker);
    }

    private Place(Builder builder) {
        mLatLng = builder.mLatLng;
        mMarker = builder.mMarker;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Place() {
        mLatLng = null;
        mMarker = null;
    }

    /** A builder of {@link Place}. */
    public static final class Builder {
        private LatLng mLatLng;
        @Nullable
        private PlaceMarker mMarker;

        private Builder(LatLng latLng) {
            this.mLatLng = latLng;
        }

        private Builder(Place place) {
            mLatLng = requireNonNull(place.mLatLng);
            mMarker = place.mMarker;
        }

        /**
         * Sets the geographical location associated with this place.
         *
         * @throws NullPointerException if {@code latLng} is {@code null}.
         */
        @NonNull
        public Builder setLatLng(@NonNull LatLng latLng) {
            this.mLatLng = requireNonNull(latLng);
            return this;
        }

        /**
         * Sets the {@link PlaceMarker} that specifies how this place is to be displayed on a
         * map, or
         * {@code null} to not display a marker for this place.
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
