/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.internal.checkPrecondition
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

private const val AssumePointerMoveStoppedMilliseconds: Int = 40
private const val HistorySize: Int = 20
private const val HorizonMilliseconds: Int = 100

internal class PointerVelocityTracker1D(
    // whether the data points added to the tracker represent differential values
    // (i.e. change in the  tracked object's displacement since the previous data point).
    // If false, it means that the data points added to the tracker will be considered as absolute
    // values (e.g. positional values).
    val isDataDifferential: Boolean = false,
    // The velocity tracking strategy that this instance uses for all velocity calculations.
    private val strategy: Strategy = Strategy.Lsq2,
    // Prevents getting the velocity value opposite to the general scroll direction of the pointer
    // movement.
    private val preventOppositeVelocity: Boolean = false,
) {

    init {
        if (isDataDifferential && strategy.equals(Strategy.Lsq2)) {
            throw IllegalStateException("Lsq2 not (yet) supported for differential axes")
        }
    }

    private val minSampleSize: Int =
        when (strategy) {
            Strategy.Impulse -> 2
            Strategy.Lsq2 -> 3
        }

    /**
     * A strategy used for velocity calculation. Each strategy has a different philosophy that could
     * result in notably different velocities than the others, so make careful choice or change of
     * strategy whenever you want to make one.
     */
    internal enum class Strategy {
        /**
         * Least squares strategy. Polynomial fit at degree 2. Note that the implementation of this
         * strategy currently supports only non-differential data points.
         */
        Lsq2,

        /**
         * Impulse velocity tracking strategy, that calculates velocity using the mathematical
         * relationship between kinetic energy and velocity.
         */
        Impulse,
    }

    // Circular buffer; current sample at index.
    private val samples: Array<DataPointAtTime?> = arrayOfNulls(HistorySize)
    private var index: Int = 0

    // Reusable arrays to avoid allocation inside calculateVelocity.
    private val reusableDataPointsArray = FloatArray(HistorySize)
    private val reusableTimeArray = FloatArray(HistorySize)

    // Reusable array to minimize allocations inside calculateLeastSquaresVelocity.
    private val reusableVelocityCoefficients = FloatArray(3)

    /**
     * Adds a data point for velocity calculation at a given time, [timeMillis]. The data ponit
     * represents an amount of a change in position (for differential data points), or an absolute
     * position (for non-differential data points). Whether or not the tracker handles differential
     * data points is decided by [isDataDifferential], which is set once and finally during the
     * construction of the tracker.
     *
     * Use the same units for the data points provided. For example, having some data points in `cm`
     * and some in `m` will result in incorrect velocity calculations, as this method (and the
     * tracker) has no knowledge of the units used.
     */
    fun addDataPoint(timeMillis: Long, dataPoint: Float) {
        index = (index + 1) % HistorySize
        samples.set(index, timeMillis, dataPoint)
    }

    /**
     * Computes the estimated velocity at the time of the last provided data point.
     *
     * The units of velocity will be `units/second`, where `units` is the units of the data points
     * provided via [addDataPoint].
     *
     * This can be expensive. Only call this when you need the velocity.
     */
    fun calculateVelocity(): Float {
        val dataPoints = reusableDataPointsArray
        val time = reusableTimeArray
        var sampleCount = 0
        var index: Int = index

        // The sample at index is our newest sample.  If it is null, we have no samples so return.
        val newestSample: DataPointAtTime = samples[index] ?: return 0f

        var previousSample: DataPointAtTime = newestSample

        // Starting with the most recent PointAtTime sample, iterate backwards while
        // the samples represent continuous motion.
        do {
            val sample: DataPointAtTime = samples[index] ?: break

            val age: Float = (newestSample.time - sample.time).toFloat()
            val delta: Float = abs(sample.time - previousSample.time).toFloat()
            previousSample =
                if (strategy == Strategy.Lsq2 || isDataDifferential) {
                    sample
                } else {
                    newestSample
                }
            if (age > HorizonMilliseconds || delta > AssumePointerMoveStoppedMilliseconds) {
                break
            }

            dataPoints[sampleCount] = sample.dataPoint
            time[sampleCount] = -age
            index = (if (index == 0) HistorySize else index) - 1

            sampleCount += 1
        } while (sampleCount < HistorySize)

        sampleCount = adjustDataPointsIfNeeded(sampleCount)

        if (sampleCount >= minSampleSize) {
            // Choose computation logic based on strategy.
            val velocity = when (strategy) {
                Strategy.Impulse -> {
                    calculateImpulseVelocity(dataPoints, time, sampleCount, isDataDifferential)
                }
                Strategy.Lsq2 -> {
                    calculateLeastSquaresVelocity(dataPoints, time, sampleCount)
                }
            } * 1000 // Multiply by "1000" to convert from units/ms to units/s

            if (preventOppositeVelocity) {
                if (dataPoints[sampleCount - 1] < dataPoints[0] && velocity < 0) {
                    return 0f
                }
                if (dataPoints[sampleCount - 1] > dataPoints[0] && velocity > 0) {
                    return 0f
                }
            }
            return velocity
        }

        // We're unable to make a velocity estimate but we did have at least one
        // valid pointer position.
        return 0f
    }

    /**
     * Adjusts the data points in the reusable arrays if needed, based on a particular strategy and
     * the provided sample count. This is primarily used to fix cases when the Lsq2 returns an opposite
     * direction of velocity for a small sample count.
     *
     * If the selected strategy is not `Strategy.Lsq2`, the method returns the original sample
     * count without any modification. For `Strategy.Lsq2`, it ensures that there are at least
     * three data points by modifying the reusable arrays as necessary.
     *
     * @param sampleCount The number of data points currently available for velocity calculation.
     * @return The updated sample count after adjustments, if performed. If no adjustments are
     *         needed, the original sample count is returned.
     */
    private fun adjustDataPointsIfNeeded(sampleCount: Int): Int {
        if (strategy != Strategy.Lsq2) return sampleCount
        if (sampleCount > 3) return sampleCount
        if (sampleCount < 2) return sampleCount

        val firstPoint = reusableDataPointsArray[0]
        val firstTime = reusableTimeArray[0]

        val lastPoint = reusableDataPointsArray[sampleCount - 1]
        val lastTime = reusableTimeArray[sampleCount - 1]

        reusableDataPointsArray[1] = (firstPoint + 2 * lastPoint) / 3f
        reusableTimeArray[1] = (2 * firstTime + lastTime) / 3f

        reusableDataPointsArray[2] = lastPoint
        reusableTimeArray[2] = lastTime

        return 3
    }

    /**
     * Computes the estimated velocity at the time of the last provided data point.
     *
     * The method allows specifying the maximum absolute value for the calculated velocity. If the
     * absolute value of the calculated velocity exceeds the specified maximum, the return value
     * will be clamped down to the maximum. For example, if the absolute maximum velocity is
     * specified as "20", a calculated velocity of "25" will be returned as "20", and a velocity of
     * "-30" will be returned as "-20".
     *
     * @param maximumVelocity the absolute value of the maximum velocity to be returned in
     *   units/second, where `units` is the units of the positions provided to this VelocityTracker.
     */
    fun calculateVelocity(maximumVelocity: Float): Float {
        checkPrecondition(maximumVelocity > 0f) {
            "maximumVelocity should be a positive value. You specified=$maximumVelocity"
        }
        val velocity = calculateVelocity()

        return if (velocity == 0.0f || velocity.isNaN()) {
            0.0f
        } else if (velocity > 0) {
            velocity.coerceAtMost(maximumVelocity)
        } else {
            velocity.coerceAtLeast(-maximumVelocity)
        }
    }

    /** Clears data points added by [addDataPoint]. */
    fun resetTracking() {
        samples.fill(element = null)
        index = 0
    }

    /**
     * Calculates velocity based on [Strategy.Lsq2]. The provided [time] entries are in "ms", and
     * should be provided in reverse chronological order. The returned velocity is in "units/ms",
     * where "units" is unit of the [dataPoints].
     */
    private fun calculateLeastSquaresVelocity(
        dataPoints: FloatArray,
        time: FloatArray,
        sampleCount: Int,
    ): Float {
        // The 2nd coefficient is the derivative of the quadratic polynomial at
        // x = 0, and that happens to be the last timestamp that we end up
        // passing to polyFitLeastSquares.
        return try {
            polyFitLeastSquares(time, dataPoints, sampleCount, 2, reusableVelocityCoefficients)[1]
        } catch (_: IllegalArgumentException) {
            0f
        }
    }
}

private fun Array<DataPointAtTime?>.set(index: Int, time: Long, dataPoint: Float) {
    val currentEntry = this[index]
    if (currentEntry == null) {
        this[index] = DataPointAtTime(time, dataPoint)
    } else {
        currentEntry.time = time
        currentEntry.dataPoint = dataPoint
    }
}

private fun calculateImpulseVelocity(
    dataPoints: FloatArray,
    time: FloatArray,
    sampleCount: Int,
    isDataDifferential: Boolean,
): Float {
    var work = 0f
    val start = sampleCount - 1
    var nextTime = time[start]
    for (i in start downTo 1) {
        val currentTime = nextTime
        nextTime = time[i - 1]
        if (currentTime == nextTime) {
            continue
        }
        val dataPointsDelta =
            if (isDataDifferential) -dataPoints[i - 1] else dataPoints[i] - dataPoints[i - 1]
        val vCurr = dataPointsDelta / (currentTime - nextTime)
        val vPrev = kineticEnergyToVelocity(work)
        work += (vCurr - vPrev) * abs(vCurr)
        if (i == start) {
            work = (work * 0.5f)
        }
    }
    return kineticEnergyToVelocity(work)
}
@Suppress("NOTHING_TO_INLINE")
private inline fun kineticEnergyToVelocity(kineticEnergy: Float): Float {
    return sign(kineticEnergy) * sqrt(2 * abs(kineticEnergy))
}

private typealias TempMatrix = Array<FloatArray>

@Suppress("NOTHING_TO_INLINE")
private inline operator fun TempMatrix.get(row: Int, col: Int): Float = this[row][col]

@Suppress("NOTHING_TO_INLINE")
private inline operator fun TempMatrix.set(row: Int, col: Int, value: Float) {
    this[row][col] = value
}
