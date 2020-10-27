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

/** Tests for {@link LatLng}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LatLngTest {
    @Test
    public void createInstance() {
        LatLng location = LatLng.create(123.f, 456.f);
        assertThat(location.getLatitude()).isWithin(0.001).of(123.f);
        assertThat(location.getLongitude()).isWithin(0.001).of(456.f);
    }

    @Test
    public void equals() {
        LatLng latLng = LatLng.create(123.45, 987.65);

        assertThat(LatLng.create(123.45, 987.65)).isEqualTo(latLng);
    }

    @Test
    public void notEquals_differentLat() {
        LatLng latLng = LatLng.create(123.45, 987.65);

        assertThat(LatLng.create(123.449999999, 987.65)).isNotEqualTo(latLng);
        assertThat(LatLng.create(123.450000001, 987.65)).isNotEqualTo(latLng);
    }

    @Test
    public void notEquals_differentLng() {
        LatLng latLng = LatLng.create(123.45, 987.65);

        assertThat(LatLng.create(123.45, 987.64999999999)).isNotEqualTo(latLng);
        assertThat(LatLng.create(123.45, 987.65000000001)).isNotEqualTo(latLng);
    }
}
