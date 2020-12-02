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

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for the {@link Place} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PlaceTest {
    /** Tests basic setter and getter operations. */
    @Test
    public void setAndGet() {
        Place place =
                Place.builder(LatLng.create(123, 456))
                        .setMarker(PlaceMarker.builder().setLabel("A").build())
                        .build();
        assertThat(place.getLatLng()).isEqualTo(LatLng.create(123, 456));
        assertThat(place.getMarker()).isEqualTo(PlaceMarker.builder().setLabel("A").build());
    }

    @Test
    public void equals() {
        Place place =
                Place.builder(LatLng.create(123, 456))
                        .setMarker(PlaceMarker.builder().setLabel("A").build())
                        .build();

        assertThat(place)
                .isEqualTo(
                        Place.builder(LatLng.create(123, 456))
                                .setMarker(PlaceMarker.builder().setLabel("A").build())
                                .build());
    }

    @Test
    public void notEquals_differentLatLng() {
        Place place = Place.builder(LatLng.create(123, 456)).build();

        assertThat(place).isNotEqualTo(Place.builder(LatLng.create(1, 2)).build());
    }

    @Test
    public void notEquals_differentMarker() {
        Place place =
                Place.builder(LatLng.create(123, 456))
                        .setMarker(PlaceMarker.builder().setLabel("A").build())
                        .build();

        assertThat(place)
                .isNotEqualTo(
                        Place.builder(LatLng.create(123, 456))
                                .setMarker(PlaceMarker.builder().setLabel("B").build())
                                .build());
    }
}
