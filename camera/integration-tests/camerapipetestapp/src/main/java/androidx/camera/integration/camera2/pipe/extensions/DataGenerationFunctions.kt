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

package androidx.camera.integration.camera2.pipe.extensions

import androidx.camera.integration.camera2.pipe.DataGenerationParams1D
import androidx.camera.integration.camera2.pipe.DataGenerator
import androidx.camera.integration.camera2.pipe.generateNewValue

/** Represents a camera data point, unprocessed */
data class GeneratedDataPoint1D(
    val frameNumber: Long,
    val timestampNanos: Long,
    val value: Any
)

/**
 * Useful for the Runnable that needs to keep track what frame it is on (can't just increment
 * because of potential delays) and trigger listener for all the new points generated
 */
data class GeneratedDataPackage1D(
    val lastFrame: Long,
    val points: List<GeneratedDataPoint1D>
)

/** Returns a package of data generated randomly, using params given */
fun DataGenerator.generateData1D(
    params: DataGenerationParams1D,
    lastValue: Any? = null,
    lastFrame: Long
): GeneratedDataPackage1D {

    var frame = lastFrame
    var previousValue = lastValue

    /** Set sleep time to be time given in params */
    var sleep = params.unitSleepTimeMillis

    /** Sleep time is extended if by chance. This simulates delay in data arrival */
    if ((1..params.delayOnceInHowMany).random() == 1)
        sleep = (params.delayTimeMillisLowerBound..params.delayTimeMillisUpperBound).random()

    /** If there is a delay this will be greater than 1, otherwise it will equal 1 */
    val numFramesToPackage = sleep / params.unitSleepTimeMillis

    val points = mutableListOf<GeneratedDataPoint1D>()

    repeat(numFramesToPackage) {
        val timeStampNanos = System.nanoTime() - beginTimeNanos

        /**
         * Each value generated can be dependent on the previous value or not, and can be null. Null
         * data points aren't added to the generated points. This simulates missing data
         */
        val currentValue = params.generateNewValue(previousValue)
        currentValue?.let {
            points.add(
                GeneratedDataPoint1D(
                    frame,
                    timeStampNanos,
                    it
                )
            )
        }

        previousValue = currentValue
        frame++
        Thread.sleep(params.unitSleepTimeMillis.toLong())
    }

    return GeneratedDataPackage1D(frame, points)
}

/** Runnable for value graph data generation */
fun DataGenerator.get1DRunnable(
    params: DataGenerationParams1D,
    triggerDataListeners: (GeneratedDataPoint1D) -> Unit
): Runnable = Runnable {
    var lastValue: Any? = null
    var frameNumber = 1L
    while (true) {
        try {
            /** Get the package of data generated */
            val dataPackage = generateData1D(params, lastValue, frameNumber)

            val points = dataPackage.points
            if (points.isNotEmpty()) lastValue = points.last().value
            frameNumber = dataPackage.lastFrame

            /** Trigger the data listener for each new data point */
            dataPackage.points.forEach {
                triggerDataListeners(it)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
