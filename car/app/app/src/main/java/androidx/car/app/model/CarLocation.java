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

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.location.Location;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;

/** Represents a geographical location with a latitude and a longitude. */
@CarProtocol
public final class CarLocation {
    @Keep
    private final double mLat;
    @Keep
    private final double mLng;

    /** Returns a new instance of a {@link CarLocation}. */
    @NonNull
    public static CarLocation create(double latitude, double longitude) {
        return new CarLocation(latitude, longitude);
    }

    /**
     * Returns a new instance of a {@link CarLocation} with the same latitude and longitude
     * contained in the given {@link Location}.
     *
     * @throws NullPointerException if {@code location} is {@code null}
     */
    @NonNull
    public static CarLocation create(@NonNull Location location) {
        requireNonNull(location);
        return create(location.getLatitude(), location.getLongitude());
    }

    /** Returns the latitude of the location, in degrees. */
    public double getLatitude() {
        return mLat;
    }

    /** Returns the longitude of the location, in degrees. */
    public double getLongitude() {
        return mLng;
    }

    @Override
    public String toString() {
        return "[" + getLatitude() + ", " + getLongitude() + "]";
    }

    @Override
    public int hashCode() {
        return hash(mLat, mLng);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarLocation)) {
            return false;
        }
        CarLocation otherLatLng = (CarLocation) other;

        return Double.doubleToLongBits(mLat) == Double.doubleToLongBits(otherLatLng.mLat)
                && Double.doubleToLongBits(mLng) == Double.doubleToLongBits(otherLatLng.mLng);
    }

    private CarLocation(double lat, double lng) {
        mLat = lat;
        mLng = lng;
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarLocation() {
        this(0, 0);
    }
}
