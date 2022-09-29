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

import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_COLUMN_CENTER;
import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_COLUMN_LEFT;
import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_COLUMN_RIGHT;
import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_ROW_FIRST;
import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_ROW_SECOND;
import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_ROW_THIRD;

import static com.google.common.truth.Truth.assertThat;

import android.car.VehicleAreaSeat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SeatCarZoneAreaIdConverterTest {

    private SeatCarZoneAreaIdConverter mSeatCarZoneAreaIdConverter;

    private static final int sRow1SeatAreaId = VehicleAreaSeat.SEAT_ROW_1_LEFT
            | VehicleAreaSeat.SEAT_ROW_1_CENTER
            | VehicleAreaSeat.SEAT_ROW_1_RIGHT;
    private static final int sRow2SeatAreaId = VehicleAreaSeat.SEAT_ROW_2_LEFT
            | VehicleAreaSeat.SEAT_ROW_2_CENTER
            | VehicleAreaSeat.SEAT_ROW_2_RIGHT;
    private static final int sRow3SeatAreaId =
            VehicleAreaSeat.SEAT_ROW_3_LEFT
                    | VehicleAreaSeat.SEAT_ROW_3_CENTER
                    | VehicleAreaSeat.SEAT_ROW_3_RIGHT;


    @Before
    public void setUp() {
        mSeatCarZoneAreaIdConverter = new SeatCarZoneAreaIdConverter();
    }

    @Test
    public void convertAreaIdToCarZones_singleZone_returnsCorrectZone() {
        List<CarZone> zonesRow2Left =
                new ArrayList<>(
                        mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(
                                VehicleAreaSeat.SEAT_ROW_2_LEFT));
        List<CarZone> zonesRow2Center =
                new ArrayList<>(
                        mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(
                                VehicleAreaSeat.SEAT_ROW_2_CENTER));
        List<CarZone> zonesRow2Right =
                new ArrayList<>(
                        mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(
                                VehicleAreaSeat.SEAT_ROW_2_RIGHT));

        assertThat(zonesRow2Left).hasSize(1);
        assertThat(zonesRow2Left.get(0).getRow()).isEqualTo(CAR_ZONE_ROW_SECOND);
        assertThat(zonesRow2Left.get(0).getColumn()).isEqualTo(CAR_ZONE_COLUMN_LEFT);

        assertThat(zonesRow2Center).hasSize(1);
        assertThat(zonesRow2Center.get(0).getRow()).isEqualTo(CAR_ZONE_ROW_SECOND);
        assertThat(zonesRow2Center.get(0).getColumn()).isEqualTo(CAR_ZONE_COLUMN_CENTER);

        assertThat(zonesRow2Right).hasSize(1);
        assertThat(zonesRow2Right.get(0).getRow()).isEqualTo(CAR_ZONE_ROW_SECOND);
        assertThat(zonesRow2Right.get(0).getColumn()).isEqualTo(CAR_ZONE_COLUMN_RIGHT);
    }

    @Test
    public void convertAreaIdToCarZones_row1_returnsCorrectZones() {
        Set<CarZone> actualZones = mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(
                sRow1SeatAreaId);
        Set<CarZone> expectedZones = new HashSet<>();
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_FIRST)
                        .setColumn(CAR_ZONE_COLUMN_LEFT)
                        .build());
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_FIRST)
                        .setColumn(CAR_ZONE_COLUMN_CENTER)
                        .build());
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_FIRST)
                        .setColumn(CAR_ZONE_COLUMN_RIGHT)
                        .build());

        assertThat(areSame(expectedZones, actualZones)).isTrue();
    }

    @Test
    public void convertAreaIdToCarZones_row2_returnsCorrectZones() {
        Set<CarZone> actualZones =
                mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(sRow2SeatAreaId);
        Set<CarZone> expectedZones = new HashSet<>();
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_SECOND)
                        .setColumn(CAR_ZONE_COLUMN_LEFT)
                        .build());
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_SECOND)
                        .setColumn(CAR_ZONE_COLUMN_CENTER)
                        .build());
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_SECOND)
                        .setColumn(CAR_ZONE_COLUMN_RIGHT)
                        .build());

        assertThat(areSame(expectedZones, actualZones)).isTrue();
    }

    @Test
    public void convertAreaIdToCarZones_row3_returnsCorrectZones() {
        Set<CarZone> actualZones = mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(
                sRow3SeatAreaId);
        Set<CarZone> expectedZones = new HashSet<>();
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_THIRD)
                        .setColumn(CAR_ZONE_COLUMN_LEFT)
                        .build());
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_THIRD)
                        .setColumn(CAR_ZONE_COLUMN_CENTER)
                        .build());
        expectedZones.add(
                new CarZone.Builder()
                        .setRow(CAR_ZONE_ROW_THIRD)
                        .setColumn(CAR_ZONE_COLUMN_RIGHT)
                        .build());

        assertThat(areSame(expectedZones, actualZones)).isTrue();
    }

    @Test
    public void convertAreaIdToCarZones_invalidAreaId_returnsEmptyList() {
        int seatAreaId = 0x0000; // invalid
        Set<CarZone> zones = mSeatCarZoneAreaIdConverter.convertAreaIdToCarZones(seatAreaId);
        assertThat(zones).isEmpty();
    }

    private static boolean areSame(Set<CarZone> expectedZones, Set<CarZone> actualZones) {
        if (expectedZones.size() != actualZones.size()) {
            return false;
        }
        for (CarZone expectedZone : expectedZones) {
            boolean foundExpectedZone = false;
            for (CarZone actualZone : actualZones) {
                if (expectedZone.getRow() == actualZone.getRow()
                        && expectedZone.getColumn() == actualZone.getColumn()) {
                    foundExpectedZone = true;
                    break;
                }
            }
            if (!foundExpectedZone) {
                return false;
            }
        }
        return true;
    }
}
