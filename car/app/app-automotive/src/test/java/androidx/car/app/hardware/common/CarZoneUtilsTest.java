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

package androidx.car.app.hardware.common;

import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_COLUMN_LEFT;
import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_ROW_SECOND;

import static com.google.common.truth.Truth.assertThat;

import android.car.VehicleAreaSeat;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Modifier;

@RunWith(RobolectricTestRunner.class)
public final class CarZoneUtilsTest {

    @Test
    public void finalModifier() {
        assertThat(Modifier.isFinal(CarZoneUtils.class.getModifiers())).isTrue();
    }

    @Test
    public void convertAreaIdToCarZones_areaNone_success() {
        ImmutableSet<CarZone> result =
                CarZoneUtils.convertAreaIdToCarZones(
                        CarZoneUtils.AreaType.NONE, CarZoneAreaIdConstants.AREA_ID_GLOBAL);
        assertThat(result).containsExactly(CarZone.CAR_ZONE_GLOBAL);
    }

    @Test
    public void convertAreaIdToCarZones_areaSeat_success() {
        ImmutableSet<CarZone> result =
                CarZoneUtils.convertAreaIdToCarZones(
                        CarZoneUtils.AreaType.SEAT, VehicleAreaSeat.SEAT_ROW_2_LEFT);
        assertThat(result)
                .containsExactly(
                        new CarZone.Builder().setRow(CAR_ZONE_ROW_SECOND)
                                .setColumn(CAR_ZONE_COLUMN_LEFT).build());
    }
}
