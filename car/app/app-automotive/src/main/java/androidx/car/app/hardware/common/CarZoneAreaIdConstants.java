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

/** Car Zone area mapping constants */
public final class CarZoneAreaIdConstants {

    /** Area Id for global (non-zoned) properties */
    public static final int AREA_ID_GLOBAL = 0;

    /** Seat car zone area mapping constants */
    // Platform Hvac areas
    public static final class VehicleAreaSeat {
        public static final int ROW_1_LEFT = android.car.VehicleAreaSeat.SEAT_ROW_1_LEFT;
        public static final int ROW_1_CENTER = android.car.VehicleAreaSeat.SEAT_ROW_1_CENTER;
        public static final int ROW_1_RIGHT = android.car.VehicleAreaSeat.SEAT_ROW_1_RIGHT;

        public static final int ROW_2_LEFT = android.car.VehicleAreaSeat.SEAT_ROW_2_LEFT;
        public static final int ROW_2_CENTER = android.car.VehicleAreaSeat.SEAT_ROW_2_CENTER;
        public static final int ROW_2_RIGHT = android.car.VehicleAreaSeat.SEAT_ROW_2_RIGHT;

        public static final int ROW_3_LEFT = android.car.VehicleAreaSeat.SEAT_ROW_3_LEFT;
        public static final int ROW_3_CENTER = android.car.VehicleAreaSeat.SEAT_ROW_3_CENTER;
        public static final int ROW_3_RIGHT = android.car.VehicleAreaSeat.SEAT_ROW_3_RIGHT;

        public static final int ROW_FIRST = ROW_1_LEFT | ROW_1_CENTER | ROW_1_RIGHT;
        public static final int ROW_SECOND = ROW_2_LEFT | ROW_2_CENTER | ROW_2_RIGHT;
        public static final int ROW_THIRD = ROW_3_LEFT | ROW_3_CENTER | ROW_3_RIGHT;
        public static final int ROW_ALL = ROW_FIRST | ROW_SECOND | ROW_THIRD;

        public static final int COL_LEFT = ROW_1_LEFT | ROW_2_LEFT | ROW_3_LEFT;
        public static final int COL_CENTER = ROW_1_CENTER | ROW_2_CENTER | ROW_3_CENTER;
        public static final int COL_RIGHT = ROW_1_RIGHT | ROW_2_RIGHT | ROW_3_RIGHT;
        public static final int COL_ALL = COL_LEFT | COL_CENTER | COL_RIGHT;

        private VehicleAreaSeat() {}
    }

    private CarZoneAreaIdConstants() {}
}
