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

import androidx.car.app.annotations.ExperimentalCarApi;

import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;

/** CarZone to areaId converter for Seat */
@ExperimentalCarApi
public class SeatCarZoneAreaIdConverter implements CarZoneAreaIdConverter {

    public SeatCarZoneAreaIdConverter() {}

    /**
     * Converts seatAreaId, which is a bitmask of VehicleAreaSeat areas, into a list of
     * Car zones. Each object in the return list corresponds to a VehicleAreaSeat area in seatAreaId
     *
     * @param seatAreaId the area Id that needs to be converted to CarZone
     */
    @Override
    public @NonNull ImmutableSet<CarZone> convertAreaIdToCarZones(int seatAreaId) {
        ImmutableSet.Builder<CarZone> zones = new ImmutableSet.Builder<>();

        // first row

        // left column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_1_LEFT)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_1_LEFT) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_FIRST)
                    .setColumn(CAR_ZONE_COLUMN_LEFT).build());
        }
        // center column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_1_CENTER)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_1_CENTER) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_FIRST)
                    .setColumn(CAR_ZONE_COLUMN_CENTER).build());
        }
        // right column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_1_RIGHT)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_1_RIGHT) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_FIRST)
                    .setColumn(CAR_ZONE_COLUMN_RIGHT).build());
        }

        // second row

        // left column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_2_LEFT)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_2_LEFT) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_SECOND)
                    .setColumn(CAR_ZONE_COLUMN_LEFT).build());
        }
        // center column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_2_CENTER)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_2_CENTER) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_SECOND)
                    .setColumn(CAR_ZONE_COLUMN_CENTER).build());
        }
        // right column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_2_RIGHT)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_2_RIGHT) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_SECOND)
                    .setColumn(CAR_ZONE_COLUMN_RIGHT).build());
        }

        // third row

        // left column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_3_LEFT)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_3_LEFT) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_THIRD)
                    .setColumn(CAR_ZONE_COLUMN_LEFT).build());
        }
        // center column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_3_CENTER)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_3_CENTER) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_THIRD)
                    .setColumn(CAR_ZONE_COLUMN_CENTER).build());
        }
        // right column
        if ((seatAreaId & CarZoneAreaIdConstants.VehicleAreaSeat.ROW_3_RIGHT)
                == CarZoneAreaIdConstants.VehicleAreaSeat.ROW_3_RIGHT) {
            zones.add(new CarZone.Builder().setRow(CAR_ZONE_ROW_THIRD)
                    .setColumn(CAR_ZONE_COLUMN_RIGHT).build());
        }
        return zones.build();
    }
}
