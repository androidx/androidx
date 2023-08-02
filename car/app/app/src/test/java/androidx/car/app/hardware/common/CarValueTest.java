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

import java.util.Collections;
import java.util.List;

/** Tests for {@link androidx.car.app.hardware.common.CarValue}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarValueTest {

    @Test
    public void createInstance_withoutCarZones() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_SUCCESS;
        List<CarZone> globalZoneList = Collections.singletonList(CarZone.CAR_ZONE_GLOBAL);
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);

        assertThat(value).isEqualTo(carValue.getValue());
        assertThat(timeStampMillis).isEqualTo(carValue.getTimestampMillis());
        assertThat(status).isEqualTo(carValue.getStatus());
        assertThat(globalZoneList).isEqualTo(carValue.getCarZones());
    }

    @Test
    public void createInstance_withCarZones() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_SUCCESS;
        CarZone driverZone = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        List<CarZone> carZones = Collections.singletonList(driverZone);
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status, carZones);

        assertThat(value).isEqualTo(carValue.getValue());
        assertThat(timeStampMillis).isEqualTo(carValue.getTimestampMillis());
        assertThat(status).isEqualTo(carValue.getStatus());
        assertThat(carZones).isEqualTo(carValue.getCarZones());
    }

    @Test
    public void equals() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_SUCCESS;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>(value, timeStampMillis, status)).isEqualTo(carValue);
    }

    @Test
    public void notEquals_differentValue() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_SUCCESS;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>("other", timeStampMillis, status))
                .isNotEqualTo(carValue);
    }

    @Test
    public void notEquals_differentTimestamp() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_SUCCESS;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>(value, 20, status)).isNotEqualTo(carValue);
    }

    @Test
    public void notEquals_differentStatus() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_SUCCESS;
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status);
        assertThat(new CarValue<>(value, timeStampMillis,
                CarValue.STATUS_UNAVAILABLE)).isNotEqualTo(carValue);
    }

    @Test
    public void notEquals_differentCarZones() {
        String value = "VALUE";
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNKNOWN;
        CarZone driverZone = new CarZone.Builder()
                .setRow(CarZone.CAR_ZONE_ROW_FIRST)
                .setColumn(CarZone.CAR_ZONE_COLUMN_DRIVER).build();
        List<CarZone> carZones = Collections.singletonList(driverZone);
        CarValue<String> carValue = new CarValue<>(value, timeStampMillis, status, carZones);
        assertThat(new CarValue<>(value, timeStampMillis,
                CarValue.STATUS_UNKNOWN)).isNotEqualTo(carValue);
    }

    @Test
    public void equals_handlesUnimplementedStatus() {
        long timeStampMillis = 10;
        int status = CarValue.STATUS_UNIMPLEMENTED;
        assertThat(new CarValue<>(null, timeStampMillis, status)).isEqualTo(new CarValue<>(null,
                timeStampMillis, status));
    }
}
