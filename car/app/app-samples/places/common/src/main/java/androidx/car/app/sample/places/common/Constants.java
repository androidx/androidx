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

package androidx.car.app.sample.places.common;

import android.location.Location;

import androidx.car.app.sample.places.common.places.PlaceCategory;

/** App-wide constants */
class Constants {
    /** The initial location to use as an anchor for searches. */
    static final Location INITIAL_SEARCH_LOCATION;

    /** The radius around the current anchor location to to search for POIs. */
    static final int POI_SEARCH_RADIUS_METERS = 2000; // 2 km ~ 1.2 miles.

    /** The maximum number of location search results when searching for POIs. */
    static final int POI_SEARCH_MAX_RESULTS = 12;

    /** The radius around the current anchor location to search for other anchor locations. */
    static final int LOCATION_SEARCH_RADIUS_METERS = 100000; // 100 km ~ 62 miles.

    /** The maximum number of location search results when searching for anchors. */
    static final int LOCATION_SEARCH_MAX_RESULTS = 5;

    static final PlaceCategory[] CATEGORIES = {
            PlaceCategory.create("Banks", "bank"),
            PlaceCategory.create("Bars", "bar"),
            PlaceCategory.create("Parking", "parking"),
            PlaceCategory.create("Restaurants", "restaurant"),
            PlaceCategory.create("Gas stations", "gas_station"),
            PlaceCategory.create("Transit stations", "transit_station")
    };

    static {
        INITIAL_SEARCH_LOCATION = new Location("PlacesDemo");

        // Googleplex
        INITIAL_SEARCH_LOCATION.setLatitude(37.422255);
        INITIAL_SEARCH_LOCATION.setLongitude(-122.084047);
    }

    private Constants() {
    }
}
