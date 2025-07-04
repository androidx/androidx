/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.hardware.info;

import static com.google.common.truth.Truth.assertThat;

import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.CarZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class MileageTest {

    @Test
    public void builder_allCarValuesAreUnknownIfNotSet() {
        assertThat(new Mileage.Builder().build()).isEqualTo(
                new Mileage.Builder().setDistanceDisplayUnit(
                        CarValue.UNKNOWN_INTEGER).setOdometerMeters(
                        CarValue.UNKNOWN_FLOAT).build());
    }

    @Test
    public void getOdometerInKilometers() {
        Mileage mileage = new Mileage.Builder().setOdometerMeters(
                new CarValue<>(100f, 0, CarValue.STATUS_SUCCESS,
                        Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))).build();

        assertThat(mileage.getOdometerInKilometers().getValue()).isEqualTo(100f);
    }

    @Test
    public void equals_whenSameOdometer_returnsTrue() {
        Mileage mileage1 = new Mileage.Builder().setOdometerMeters(
                new CarValue<>(105f, 0, CarValue.STATUS_SUCCESS,
                        Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))).build();
        Mileage mileage2 = new Mileage.Builder().setOdometerMeters(
                new CarValue<>(105f, 0, CarValue.STATUS_SUCCESS,
                        Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))).build();

        assertThat(mileage1).isEqualTo(mileage2);
    }

    @Test
    public void equals_whenDifferentOdometer_returnsFalse() {
        Mileage mileage1 = new Mileage.Builder().setOdometerMeters(
                new CarValue<>(200f, 0, CarValue.STATUS_SUCCESS,
                        Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))).build();
        Mileage mileage2 = new Mileage.Builder().setOdometerMeters(
                new CarValue<>(105f, 0, CarValue.STATUS_SUCCESS,
                        Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))).build();

        assertThat(mileage1).isNotEqualTo(mileage2);
    }

    @Test
    @SuppressWarnings("deprecation") // Intentionally testing deprecated getOdometerMeters
    public void getOdometerMeters_returnsTheSameAsGetOdometerInKilometers() {
        Mileage mileage = new Mileage.Builder().setOdometerMeters(
                new CarValue<>(563f, 0, CarValue.STATUS_SUCCESS,
                        Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))).build();

        assertThat(mileage.getOdometerMeters()).isEqualTo(mileage.getOdometerInKilometers());
    }
}
