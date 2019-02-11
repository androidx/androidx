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

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * A representation of a latitude, longitude that can be serialized as a
 * {@link VersionedParcelable}.
 * <p>
 * Unlike {@link Location}, this class only contains latitude and longitude and not other fields
 * like bearing, speed and so on.
  */
@VersionedParcelize
public final class LatLng implements VersionedParcelable {
    @ParcelField(1)
    double mLatitude;
    @ParcelField(2)
    double mLongitude;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    LatLng() {
    }

    /**
     * Construct a new {@link LatLng} with the given latitude and longitude.
     */
    public LatLng(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    /**
     * Returns the latitude.
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Returns the longitude.
     */
    public double getLongitude() {
        return mLongitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LatLng latlng = (LatLng) o;
        return getLatitude() == latlng.getLatitude() && getLongitude() == latlng.getLongitude();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLatitude(), getLongitude());
    }

    // DefaultLocale suppressed as this method is only offered for debugging purposes.
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{%f, %f}", mLatitude, mLongitude);
    }
}
