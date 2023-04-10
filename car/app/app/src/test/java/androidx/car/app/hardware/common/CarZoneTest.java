/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.hardware.common;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarZone}*/
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarZoneTest {

    @Test
    public void createInstance_defaultValue() {
        CarZone carZone = new CarZone.Builder().build();
        assertThat(CarZone.CAR_ZONE_COLUMN_ALL).isEqualTo(carZone.getColumn());
        assertThat(CarZone.CAR_ZONE_ROW_ALL).isEqualTo(carZone.getRow());
    }

    @Test
    public void createInstance_driveSide() {
        CarZone carZone = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        assertThat(CarZone.CAR_ZONE_COLUMN_DRIVER).isEqualTo(carZone.getColumn());
        assertThat(CarZone.CAR_ZONE_ROW_FIRST).isEqualTo(carZone.getRow());
    }

    @Test
    public void equals() {
        CarZone carZoneDriver = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        CarZone carZoneDriverDuplicate = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        assertThat(carZoneDriver).isEqualTo(carZoneDriverDuplicate);
    }

    @Test
    public void notEquals_differentRow() {
        CarZone carZoneDriver = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        CarZone allDriverSideSeat = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_ALL)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        assertThat(carZoneDriver).isNotEqualTo(allDriverSideSeat);
    }

    @Test
    public void notEquals_differentColumn() {
        CarZone carZoneDriver = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        CarZone carZonePassenger = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_PASSENGER).build();
        assertThat(carZoneDriver).isNotEqualTo(carZonePassenger);
    }
}
