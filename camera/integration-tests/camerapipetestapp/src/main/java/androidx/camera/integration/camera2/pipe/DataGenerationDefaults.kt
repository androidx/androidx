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

package androidx.camera.integration.camera2.pipe

import kotlin.math.max
import kotlin.math.min

/** This file contains the nuts and bolts of data generation */
fun DataGenerationParams1D.generateNewValue(prev: Any?): Any? {
    return when (this.typeString) {
        "Float" -> generateNewFloatOrNull(prev)
        "Int" -> generateNewIntOrNull(prev)
        "Bool" -> generateNewBoolOrNull(prev)
        else -> throw Exception("no generate new value function available for ${this.typeString}")
    }
}

private fun DataGenerationParams1D.generateNewFloatOrNull(prev: Any?): Float? {
    if (prev !is Float?) throw Exception("Can't generate new Float from $prev")
    val randomFloat = if ((1..this.nullValueOnceInHowMany).random() == 1) null
    else (this.valueLowerBound..this.valueUpperBound).random().toFloat()

    return when {
        prev == null -> randomFloat
        (1..this.changeModeOnceInHowMany).random() == 1 -> {
            if (randomFloat == null) randomFloat
            else {
                val change = (0..5).random()
                val direction = (0..1).random()
                val newValue = if (direction == 0) prev - change else prev.toFloat() + change
                max(
                    min(this.valueUpperBound.toFloat(), newValue),
                    this.valueLowerBound
                        .toFloat()
                )
            }
        }
        else -> prev
    }
}

private fun DataGenerationParams1D.generateNewIntOrNull(prev: Any?): Int? {
    if (prev !is Int?) throw Exception("Can't generate new Int from $prev")

    return if ((1..this.changeModeOnceInHowMany).random() == 1 || prev == null) {
        if ((1..this.nullValueOnceInHowMany).random() == 1) null
        else (this.valueLowerBound..this.valueUpperBound).random()
    } else prev
}

private fun DataGenerationParams1D.generateNewBoolOrNull(prev: Any?): Boolean? {
    if (prev !is Boolean?) throw Exception("Can't generate new Bool from $prev")
    return if ((1..this.changeModeOnceInHowMany).random() == 1 || prev == null) {
        val temp = if ((1..this.nullValueOnceInHowMany).random() == 1) null
        else (0..1).random()
        when (temp) {
            0 -> false
            1 -> true
            else -> null
        }
    } else prev
}

/** These parameters can be adjusted to simulate real camera data more */
val DATA_GENERATION_PARAMS: Map<CameraMetadataKey, DataGenerationParams1D> = mapOf(
    CameraMetadataKey.LENS_FOCUS_DISTANCE to DataGenerationParams1D(
        typeString = "Float",
        unitSleepTimeMillis = 60,
        delayTimeMillisLowerBound = 200,
        delayTimeMillisUpperBound = 300,
        valueLowerBound = 1,
        valueUpperBound = 30,
        delayOnceInHowMany = 30,
        nullValueOnceInHowMany = 35,
        changeModeOnceInHowMany = 5
    ),

    CameraMetadataKey.LENS_FOCAL_LENGTH to DataGenerationParams1D(
        typeString = "Float",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 50,
        delayTimeMillisUpperBound = 250,
        valueLowerBound = 7,
        valueUpperBound = 23,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 15,
        changeModeOnceInHowMany = 10
    ),

    CameraMetadataKey.CONTROL_AE_MODE to DataGenerationParams1D(
        typeString = "Int",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 100,
        delayTimeMillisUpperBound = 200,
        valueLowerBound = 0,
        valueUpperBound = 4,
        delayOnceInHowMany = 30,
        nullValueOnceInHowMany = 10,
        changeModeOnceInHowMany = 20
    ),

    CameraMetadataKey.CONTROL_AF_MODE to DataGenerationParams1D(
        typeString = "Int",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 100,
        delayTimeMillisUpperBound = 200,
        valueLowerBound = 0,
        valueUpperBound = 5,
        delayOnceInHowMany = 30,
        nullValueOnceInHowMany = 10,
        changeModeOnceInHowMany = 25
    ),

    CameraMetadataKey.CONTROL_AWB_MODE to DataGenerationParams1D(
        typeString = "Int",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 100,
        delayTimeMillisUpperBound = 200,
        valueLowerBound = 0,
        valueUpperBound = 8,
        delayOnceInHowMany = 30,
        nullValueOnceInHowMany = 10,
        changeModeOnceInHowMany = 25
    ),

    CameraMetadataKey.BLACK_LEVEL_LOCK to DataGenerationParams1D(
        typeString = "Bool",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 100,
        delayTimeMillisUpperBound = 200,
        valueLowerBound = 0,
        valueUpperBound = 1,
        delayOnceInHowMany = 30,
        nullValueOnceInHowMany = 10,
        changeModeOnceInHowMany = 25
    ),

    CameraMetadataKey.JPEG_QUALITY to DataGenerationParams1D(
        typeString = "Int",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 100,
        delayTimeMillisUpperBound = 200,
        valueLowerBound = 85,
        valueUpperBound = 95,
        delayOnceInHowMany = 30,
        nullValueOnceInHowMany = 10,
        changeModeOnceInHowMany = 30
    ),

    CameraMetadataKey.COLOR_CORRECTION_ABERRATION_MODE to DataGenerationParams1D(
        typeString = "Int",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 0,
        delayTimeMillisUpperBound = 100,
        valueLowerBound = 0,
        valueUpperBound = 2,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 20,
        changeModeOnceInHowMany = 40
    ),

    CameraMetadataKey.CONTROL_AE_EXPOSURE_COMPENSATION to DataGenerationParams1D(
        typeString = "Int",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 0,
        delayTimeMillisUpperBound = 100,
        valueLowerBound = -5,
        valueUpperBound = 5,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 20,
        changeModeOnceInHowMany = 40
    ),

    CameraMetadataKey.CONTROL_ZOOM_RATIO to DataGenerationParams1D(
        typeString = "Float",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 50,
        delayTimeMillisUpperBound = 250,
        valueLowerBound = 0,
        valueUpperBound = 5,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 15,
        changeModeOnceInHowMany = 10
    ),

    CameraMetadataKey.JPEG_ORIENTATION to DataGenerationParams1D(
        typeString = "Float",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 50,
        delayTimeMillisUpperBound = 250,
        valueLowerBound = 0,
        valueUpperBound = 270,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 15,
        changeModeOnceInHowMany = 10
    ),
    CameraMetadataKey.LENS_FILTER_DENSITY to DataGenerationParams1D(
        typeString = "Float",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 50,
        delayTimeMillisUpperBound = 250,
        valueLowerBound = 0,
        valueUpperBound = 100,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 15,
        changeModeOnceInHowMany = 10
    ),

    CameraMetadataKey.SENSOR_SENSITIVITY to DataGenerationParams1D(
        typeString = "Float",
        unitSleepTimeMillis = 30,
        delayTimeMillisLowerBound = 99,
        delayTimeMillisUpperBound = 801,
        valueLowerBound = 0,
        valueUpperBound = 270,
        delayOnceInHowMany = 50,
        nullValueOnceInHowMany = 15,
        changeModeOnceInHowMany = 10
    )
)

/** Defines how the data is generated for 1D visualizations */

data class DataGenerationParams1D(

    val typeString: String,

    /** Frequency of data coming in */
    val unitSleepTimeMillis: Int,

    /** If there is a delay, this represents the shortest delay */
    val delayTimeMillisLowerBound: Int,

    /** If there is a delay, this represents the largest delay */
    val delayTimeMillisUpperBound: Int,

    /** Lower bound of the generated Data */
    val valueLowerBound: Int,

    /** Upper bound of the generated Data */
    val valueUpperBound: Int,

    /** One in this many times, there is a delay */
    val delayOnceInHowMany: Int,

    /** Once in this many times, there is no data */
    val nullValueOnceInHowMany: Int,

    /** Once in this many times, the value changes from what it was before */
    val changeModeOnceInHowMany: Int
)
