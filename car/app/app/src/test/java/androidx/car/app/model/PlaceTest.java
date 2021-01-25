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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for the {@link Place} class. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PlaceTest {
    /** Tests basic setter and getter operations. */
    @Test
    public void setAndGet() {
        Place place =
                new Place.Builder(CarLocation.create(123, 456))
                        .setMarker(new PlaceMarker.Builder().setLabel("A").build())
                        .build();
        assertThat(place.getLocation()).isEqualTo(CarLocation.create(123, 456));
        assertThat(place.getMarker()).isEqualTo(new PlaceMarker.Builder().setLabel("A").build());
    }

    @Test
    public void equals() {
        Place place =
                new Place.Builder(CarLocation.create(123, 456))
                        .setMarker(new PlaceMarker.Builder().setLabel("A").build())
                        .build();

        assertThat(place)
                .isEqualTo(
                        new Place.Builder(CarLocation.create(123, 456))
                                .setMarker(new PlaceMarker.Builder().setLabel("A").build())
                                .build());
    }

    @Test
    public void notEquals_differentLatLng() {
        Place place = new Place.Builder(CarLocation.create(123, 456)).build();

        assertThat(place).isNotEqualTo(new Place.Builder(CarLocation.create(1, 2)).build());
    }

    @Test
    public void notEquals_differentMarker() {
        Place place =
                new Place.Builder(CarLocation.create(123, 456))
                        .setMarker(new PlaceMarker.Builder().setLabel("A").build())
                        .build();

        assertThat(place)
                .isNotEqualTo(
                        new Place.Builder(CarLocation.create(123, 456))
                                .setMarker(new PlaceMarker.Builder().setLabel("B").build())
                                .build());
    }
}
