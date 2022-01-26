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

package androidx.car.app.hardware;

/**
 * @hide
 */
oneway interface ICarHardwareResultTypes {
    const int TYPE_UNKNOWN = 0;
    const int TYPE_INFO_MODEL = 1;
    const int TYPE_INFO_ENERGY_PROFILE = 2;
    const int TYPE_INFO_TOLL = 3;
    const int TYPE_INFO_ENERGY_LEVEL = 4;
    const int TYPE_INFO_SPEED = 5;
    const int TYPE_INFO_MILEAGE = 6;
    const int TYPE_INFO_EV_STATUS = 7;

    const int TYPE_SENSOR_ACCELEROMETER = 20;
    const int TYPE_SENSOR_COMPASS = 21;
    const int TYPE_SENSOR_GYROSCOPE = 22;
    const int TYPE_SENSOR_CAR_LOCATION = 23;
}
