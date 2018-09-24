/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.velocity_tracker

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.lsq_solver.LeastSquaresSolver
import androidx.ui.gestures.lsq_solver.PolynomialFit
import kotlin.math.absoluteValue

/**
 * Computes a pointer's velocity based on data from [PointerMoveEvent]s.
 *
 * The input data is provided by calling [addPosition]. Adding data is cheap.
 *
 * To obtain a velocity, call [getVelocity] or [getVelocityEstimate]. This will
 * compute the velocity based on the data added so far. Only call these when
 * you need to use the velocity, as they are comparatively expensive.
 *
 * The quality of the velocity estimation will be better if more data points
 * have been received.
 */
open class VelocityTracker {

    // Circular buffer; current sample at _index.
    private val _samples: Array<_PointAtTime?> = Array(_kHistorySize) { null }
    private var _index: Int = 0

    /** Adds a position as the given time to the tracker. */
    fun addPosition(time: Duration, position: Offset) {
        _index += 1
        if (_index == _kHistorySize) {
            _index = 0
        }
        _samples[_index] = _PointAtTime(position, time)
    }

    /**
     * Returns an estimate of the velocity of the object being tracked by the
     * tracker given the current information available to the tracker.
     *
     * Information is added using [addPosition].
     *
     * Returns null if there is no data on which to base an estimate.
     */
    open fun getVelocityEstimate(): VelocityEstimate? {
        val x: MutableList<Double> = mutableListOf()
        val y: MutableList<Double> = mutableListOf()
        val w: MutableList<Double> = mutableListOf()
        val time: MutableList<Double> = mutableListOf()
        var sampleCount = 0
        var index: Int = _index

        val newestSample: _PointAtTime = _samples[index] ?: return null

        var previousSample: _PointAtTime = newestSample
        var oldestSample: _PointAtTime = newestSample

        // Starting with the most recent PointAtTime sample, iterate backwards while
        // the samples represent continuous motion.
        do {
            val sample: _PointAtTime = _samples[index] ?: break

            val age: Double = (newestSample.time - sample.time).inMilliseconds.toDouble()
            val delta: Double =
                (sample.time - previousSample.time).inMilliseconds.absoluteValue.toDouble()
            previousSample = sample
            if (age > _kHorizonMilliseconds || delta > _kAssumePointerMoveStoppedMilliseconds) {
                break
            }

            oldestSample = sample
            val position: Offset = sample.point
            x.add(position.dx)
            y.add(position.dy)
            w.add(1.0)
            time.add(-age)
            index = (if (index == 0) _kHistorySize else index) - 1

            sampleCount += 1
        } while (sampleCount < _kHistorySize)

        if (sampleCount >= _kMinSampleSize) {
            val xSolver =
                LeastSquaresSolver(time.toDoubleArray(), x.toDoubleArray(), w.toDoubleArray())
            val xFit: PolynomialFit? = xSolver.solve(2)
            if (xFit != null) {
                val ySolver =
                    LeastSquaresSolver(time.toDoubleArray(), y.toDoubleArray(), w.toDoubleArray())
                val yFit: PolynomialFit? = ySolver.solve(2)
                if (yFit != null) {

                    return VelocityEstimate(
                        pixelsPerSecond = Offset(
                            // convert from pixels/ms to pixels/s
                            xFit.coefficients.get(1) * 1000, yFit.coefficients.get(1) * 1000
                        ),
                        confidence = xFit.confidence * yFit.confidence,
                        duration = newestSample.time - oldestSample.time,
                        offset = newestSample.point - oldestSample.point
                    )
                }
            }
        }

        // We're unable to make a velocity estimate but we did have at least one
        // valid pointer position.
        return VelocityEstimate(
            pixelsPerSecond = Offset.zero,
            confidence = 1.0,
            duration = newestSample.time - oldestSample.time,
            offset = newestSample.point - oldestSample.point
        )
    }

    /**
     * Computes the velocity of the pointer at the time of the last
     * provided data point.
     *
     * This can be expensive. Only call this when you need the velocity.
     *
     * Returns [Velocity.zero] if there is no data from which to compute an
     * estimate or if the estimated velocity is zero.
     */
    open fun getVelocity(): Velocity {
        val estimate: VelocityEstimate? = this.getVelocityEstimate()
        if (estimate == null || estimate.pixelsPerSecond == Offset.zero) {
            return Velocity.zero
        }
        return Velocity(pixelsPerSecond = estimate.pixelsPerSecond)
    }

    companion object {
        private val _kAssumePointerMoveStoppedMilliseconds: Int = 40
        private val _kHistorySize: Int = 20
        private val _kHorizonMilliseconds: Int = 100
        private val _kMinSampleSize: Int = 3
    }
}