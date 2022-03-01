/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.records

import androidx.annotation.StringDef

/** The arm and part of the arm where a blood pressure measurement was taken. */
public object BloodPressureMeasurementLocations {
    const val LEFT_WRIST = "left_wrist"
    const val RIGHT_WRIST = "right_wrist"
    const val LEFT_UPPER_ARM = "left_upper_arm"
    const val RIGHT_UPPER_ARM = "right_upper_arm"
}

/**
 * The arm and part of the arm where a blood pressure measurement was taken.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            BloodPressureMeasurementLocations.LEFT_WRIST,
            BloodPressureMeasurementLocations.RIGHT_WRIST,
            BloodPressureMeasurementLocations.LEFT_UPPER_ARM,
            BloodPressureMeasurementLocations.RIGHT_UPPER_ARM,
        ]
)
annotation class BloodPressureMeasurementLocation
