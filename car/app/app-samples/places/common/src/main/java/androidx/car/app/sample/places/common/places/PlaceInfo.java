/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.places.common.places;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Contains information about a place returned by the Places API. */
public class PlaceInfo {
    private final String mId;
    private final String mName;
    private final Location mLocation;

    @Nullable
    private Address mAddress; // lazily written

    PlaceInfo(@NonNull String id, @NonNull String name, @NonNull Location location) {
        mId = id;
        mName = name;
        mLocation = location;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public Location getLocation() {
        return mLocation;
    }

    /** Returns the address of the given {@link Geocoder}. */
    @NonNull
    public Address getAddress(@NonNull Geocoder geocoder) {
        if (mAddress == null) {
            mAddress = LocationUtil.getAddressForLocation(geocoder, mLocation);
        }
        return mAddress;
    }

    @Override
    @NonNull
    public String toString() {
        return "[" + mName + ", " + mLocation + "]";
    }
}
