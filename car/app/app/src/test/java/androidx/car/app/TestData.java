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

package androidx.car.app;

import androidx.car.app.model.LatLng;
import androidx.car.app.model.Place;

/** A grab bag of fake data shared by tests. */
public final class TestData {
    public static final LatLng GOOGLE_KIR = LatLng.create(47.6696482, -122.19950278);
    public static final LatLng GOOGLE_BVE = LatLng.create(47.6204588, -122.1918818);

    public static final Place PLACE_KIR = Place.builder(GOOGLE_KIR).build();
    public static final Place PLACE_BVE = Place.builder(GOOGLE_BVE).build();

    private TestData() {
    }
}
