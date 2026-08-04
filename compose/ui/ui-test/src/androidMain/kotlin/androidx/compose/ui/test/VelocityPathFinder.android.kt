/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.collection.emptyLongList
import androidx.collection.floatListOf
import androidx.collection.longListOf
import androidx.collection.mutableFloatListOf
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun VelocityPathFinder(
    startPosition: Offset,
    endPosition: Offset,
    endVelocity: Float,
    durationMillis: Long,
): VelocityPathFinder {
    return if (AndroidComposeUiFlags.isFrameworkVelocityTrackerEnabled) {
        PlatformVelocityPathFinder(startPosition, endPosition, endVelocity, durationMillis)
    } else {
        LegacyVelocityPathFinder(startPosition, endPosition, endVelocity, durationMillis)
    }
}

internal class PlatformVelocityPathFinder(
    private val startPosition: Offset,
    private val endPosition: Offset,
    private val endVelocity: Float,
    private val durationMillis: Long,
) : VelocityPathFinder() {

    private var progressFunction: SwipeFunction? = null

    override fun calculateOffsetForTime(time: Long): Offset {
        val progress = progressFunction ?: computeProgressFunction().also { progressFunction = it }
        if (time <= 0L) return startPosition
        if (time >= durationMillis) return endPosition
        val fraction = progress.calculateProgress(time).coerceIn(0f, 1f)
        return Offset(
            startPosition.x + (endPosition.x - startPosition.x) * fraction,
            startPosition.y + (endPosition.y - startPosition.y) * fraction,
        )
    }

    private fun computeProgressFunction(): SwipeFunction {
        val targetVelocity = calculateTargetVelocity(startPosition, endPosition, endVelocity)
        val velocityTracker = VelocityTracker()
        return findBestProgressFunction(
            startPosition,
            endPosition,
            targetVelocity,
            durationMillis,
            velocityTracker,
        )
            ?: run {
                val requestedDistance = (endPosition - startPosition).getDistance()
                val minHorizon =
                    min(durationMillis.toDouble(), HorizonMilliseconds.toDouble()).toFloat()

                val suggestedFixes =
                    if (endVelocity == 0f) {
                        val minAchievableDuration = HorizonMilliseconds + 1
                        val minAchievableVelocity = (2f / minHorizon) * requestedDistance * 1000
                        "Suggested fixes: " +
                            "1. increase duration to $minAchievableDuration or higher; " +
                            "2. increase velocity to $minAchievableVelocity px/s or higher; or " +
                            "3. decrease the distance between the start and end to 0.0 or lower"
                    } else {
                        val suggestedDuration =
                            (2 / endVelocity * requestedDistance * 1000).toDouble()
                        val suggestedVelocity = (2f / minHorizon) * requestedDistance * 1000
                        val suggestedDistance = (minHorizon / 2f) * endVelocity / 1000
                        "Suggested fixes: " +
                            "1. set duration to $suggestedDuration or lower; " +
                            "2. set velocity to $suggestedVelocity px/s or lower; or " +
                            "3. increase the distance between the start and end to $suggestedDistance or " +
                            "higher"
                    }

                throw IllegalArgumentException(
                    "Unable to generate a swipe gesture between $startPosition and $endPosition with " +
                        "duration $durationMillis that ends with velocity of $endVelocity px/s, without " +
                        "going outside of the range [start..end]. " +
                        suggestedFixes
                )
            }
    }

    /** Maps timestamp in milliseconds to normalized swipe progress in range `[0..1]`. */
    private fun interface SwipeFunction {
        fun calculateProgress(timeMillis: Long): Float
    }

    companion object {
        /** Calculates 2D vector magnitude of velocity. */
        private fun Velocity.getMagnitude(): Float = sqrt(x * x + y * y)

        /** Calculates 2D velocity vector given start/end offsets and speed magnitude. */
        private fun calculateTargetVelocity(start: Offset, end: Offset, speed: Float): Velocity {
            val delta = end - start
            val distance = delta.getDistance()
            if (distance == 0f) return Velocity.Zero
            val directionUnitVector = delta / distance
            return Velocity(directionUnitVector.x * speed, directionUnitVector.y * speed)
        }

        /**
         * Finds a progress function that reproduces the target velocity using a tiered strategy:
         * 1. [findLinearFinalSegmentProgress]: Fast, exact solution for final linear segments of
         *    duration `d >= H`. Movement inside the horizon window is a straight line with slope
         *    equal to target velocity.
         * 2. [findPolynomialProgress]: Evaluates smooth quadratic and cubic continuous motion
         *    curves.
         * 3. [findPiecewiseLinearProgress]: Fallback grid search over 2-segment piecewise linear
         *    progress curves to minimize velocity error when exact or smooth solutions are
         *    unavailable.
         */
        private fun findBestProgressFunction(
            startPosition: Offset,
            endPosition: Offset,
            targetVelocity: Velocity,
            durationMillis: Long,
            velocityTracker: VelocityTracker,
        ): SwipeFunction? {
            val distance = (endPosition - startPosition).getDistance()
            val speed = targetVelocity.getMagnitude()

            if (distance == 0f) {
                return if (speed <= MinSpeedThreshold) SwipeFunction { 0f } else null
            }

            val maxAllowedError = max(MinVelocityErrorTolerance, speed * VelocityErrorRatio)

            if (speed < MinSpeedThreshold) {
                if (durationMillis <= HorizonMilliseconds) return null
                val movementDurationMillis =
                    max(MinStaticMoveDurationMillis, durationMillis - HorizonMilliseconds)
                val progressFunction = SwipeFunction { timeMillis ->
                    if (timeMillis <= 0L) 0f
                    else if (timeMillis >= movementDurationMillis) 1f
                    else (timeMillis.toFloat() / movementDurationMillis)
                }
                val measuredVelocity =
                    measureVelocity(
                        startPosition,
                        endPosition,
                        durationMillis,
                        progressFunction,
                        velocityTracker,
                    )
                return if (measuredVelocity.getMagnitude() <= maxAllowedError) {
                    progressFunction
                } else {
                    null
                }
            }

            val horizonMillis = min(durationMillis, HorizonMilliseconds)

            findLinearFinalSegmentProgress(
                    startPosition,
                    endPosition,
                    targetVelocity,
                    durationMillis,
                    distance,
                    speed,
                    maxAllowedError,
                    horizonMillis,
                    velocityTracker,
                )
                ?.let {
                    return it
                }

            findPolynomialProgress(
                    startPosition,
                    endPosition,
                    targetVelocity,
                    durationMillis,
                    distance,
                    speed,
                    maxAllowedError,
                    velocityTracker,
                )
                ?.let {
                    return it
                }

            return findPiecewiseLinearProgress(
                startPosition,
                endPosition,
                targetVelocity,
                durationMillis,
                maxAllowedError,
                horizonMillis,
                velocityTracker,
            )
        }

        /**
         * Evaluates progress functions containing a final linear segment of duration `d >= H`.
         *
         * Movement inside the horizon window `[durationMillis - H, durationMillis]` forms a
         * straight line with constant slope equal to [targetVelocity], allowing velocity trackers
         * to measure exact slope.
         */
        private fun findLinearFinalSegmentProgress(
            startPosition: Offset,
            endPosition: Offset,
            targetVelocity: Velocity,
            durationMillis: Long,
            distance: Float,
            speed: Float,
            maxAllowedError: Float,
            horizonMillis: Long,
            velocityTracker: VelocityTracker,
        ): SwipeFunction? {
            val finalSegmentCandidates =
                if (durationMillis >= horizonMillis) {
                    val remaining = durationMillis - horizonMillis
                    if (remaining > 0) {
                        longListOf(
                            horizonMillis,
                            horizonMillis + remaining / 2,
                            horizonMillis + remaining * 3 / 4,
                            durationMillis - 1L,
                        )
                    } else {
                        longListOf(horizonMillis)
                    }
                } else {
                    emptyLongList()
                }

            var minError = Float.MAX_VALUE
            var bestProgressFunction: SwipeFunction? = null

            for (i in 0 until finalSegmentCandidates.size) {
                val finalSegmentDurationMillis = finalSegmentCandidates[i]
                if (
                    finalSegmentDurationMillis <= 0L || finalSegmentDurationMillis >= durationMillis
                ) {
                    continue
                }
                val finalSegmentDurationSeconds = finalSegmentDurationMillis / 1000f
                val finalSegmentDistance = speed * finalSegmentDurationSeconds
                if (finalSegmentDistance < distance) {
                    val kneeProgress = 1f - (finalSegmentDistance / distance)
                    val kneeTimeMillis = durationMillis - finalSegmentDurationMillis
                    val progressFunction = SwipeFunction { timeMillis ->
                        if (timeMillis <= 0L) 0f
                        else if (timeMillis >= durationMillis) 1f
                        else if (timeMillis < kneeTimeMillis) {
                            (timeMillis.toFloat() / kneeTimeMillis) * kneeProgress
                        } else {
                            kneeProgress +
                                ((timeMillis - kneeTimeMillis).toFloat() /
                                    finalSegmentDurationMillis) * (1f - kneeProgress)
                        }
                    }
                    val measuredVelocity =
                        measureVelocity(
                            startPosition,
                            endPosition,
                            durationMillis,
                            progressFunction,
                            velocityTracker,
                        )
                    val error = (measuredVelocity - targetVelocity).getMagnitude()
                    if (error < minError) {
                        minError = error
                        bestProgressFunction = progressFunction
                        if (error <= maxAllowedError) {
                            return bestProgressFunction
                        }
                    }
                }
            }
            return if (minError <= maxAllowedError) bestProgressFunction else null
        }

        /**
         * Evaluates quadratic and cubic polynomial curves for smooth motion matching
         * [targetVelocity].
         *
         * Generates continuous normalized progress curves `s(tau)` without sharp corners or knee
         * points and tests them against the velocity tracker.
         */
        private fun findPolynomialProgress(
            startPosition: Offset,
            endPosition: Offset,
            targetVelocity: Velocity,
            durationMillis: Long,
            distance: Float,
            speed: Float,
            maxAllowedError: Float,
            velocityTracker: VelocityTracker,
        ): SwipeFunction? {
            val normalizedVelocity = (speed * (durationMillis / 1000f)) / distance
            val smoothCandidates =
                buildList<SwipeFunction> {
                    if (
                        normalizedVelocity in MinNormalizedVelocity..MaxQuadraticNormalizedVelocity
                    ) {
                        val quadraticCoefficient = normalizedVelocity - 1f
                        val linearCoefficient = 2f - normalizedVelocity
                        add(
                            SwipeFunction { timeMillis ->
                                if (timeMillis <= 0L) 0f
                                else if (timeMillis >= durationMillis) 1f
                                else {
                                    val tau = timeMillis.toFloat() / durationMillis
                                    quadraticCoefficient * tau * tau + linearCoefficient * tau
                                }
                            }
                        )
                    }

                    if (normalizedVelocity > MinNormalizedVelocity) {
                        val cubicCoefficient = normalizedVelocity - 2f
                        val quadraticCoefficient = 3f - normalizedVelocity
                        add(
                            SwipeFunction { timeMillis ->
                                if (timeMillis <= 0L) 0f
                                else if (timeMillis >= durationMillis) 1f
                                else {
                                    val tau = timeMillis.toFloat() / durationMillis
                                    cubicCoefficient * tau * tau * tau +
                                        quadraticCoefficient * tau * tau
                                }
                            }
                        )
                    }
                }

            var minError = Float.MAX_VALUE
            var bestProgressFunction: SwipeFunction? = null

            for (progressFunction in smoothCandidates) {
                val measuredVelocity =
                    measureVelocity(
                        startPosition,
                        endPosition,
                        durationMillis,
                        progressFunction,
                        velocityTracker,
                    )
                val error = (measuredVelocity - targetVelocity).getMagnitude()
                if (error < minError) {
                    minError = error
                    bestProgressFunction = progressFunction
                    if (error <= maxAllowedError) {
                        return bestProgressFunction
                    }
                }
            }
            return if (minError <= maxAllowedError) bestProgressFunction else null
        }

        /**
         * Searches piecewise linear progress functions using coarse and fine parameter grid search.
         *
         * Evaluates 2-segment linear curves with variable knee points and slopes, first scanning
         * coarsely across duration and progress steps, then refining around the best candidate to
         * minimize velocity error.
         */
        private fun findPiecewiseLinearProgress(
            startPosition: Offset,
            endPosition: Offset,
            targetVelocity: Velocity,
            durationMillis: Long,
            maxAllowedError: Float,
            horizonMillis: Long,
            velocityTracker: VelocityTracker,
        ): SwipeFunction? {
            val preHorizonDurationMillis = durationMillis - horizonMillis
            val horizonStartProgress =
                if (preHorizonDurationMillis > 0) HorizonStartProgressFraction else 0f
            val durationStepMillis = max(1L, horizonMillis / DefaultDurationStepDivider)
            val boundaryOffsets =
                floatListOf(
                    0.001f,
                    0.005f,
                    0.01f,
                    0.02f,
                    0.05f,
                    0.1f,
                    0.2f,
                    0.5f,
                    0.8f,
                    0.9f,
                    0.95f,
                    0.98f,
                    0.99f,
                    0.995f,
                    0.999f,
                )

            val startProgressCandidates =
                mutableFloatListOf().apply {
                    var startProgressCandidate = 0f
                    while (startProgressCandidate <= 1f) {
                        add(startProgressCandidate)
                        startProgressCandidate += CoarseProgressStep
                    }
                    addAll(boundaryOffsets)
                }

            var minError = Float.MAX_VALUE
            var bestProgressFunction: SwipeFunction? = null
            var bestDurationMillis = 1L
            var bestStartProgress = 0.5f

            var durationMillisInHorizon = 1L
            while (durationMillisInHorizon < horizonMillis) {
                for (i in 0 until startProgressCandidates.size) {
                    val coarseStartProgress = startProgressCandidates[i]
                    val progressFunction = SwipeFunction { timeMillis ->
                        if (timeMillis <= 0L) 0f
                        else if (timeMillis >= durationMillis) 1f
                        else if (
                            preHorizonDurationMillis > 0 && timeMillis < preHorizonDurationMillis
                        ) {
                            (horizonStartProgress / preHorizonDurationMillis) * timeMillis
                        } else {
                            val timeInHorizonMillis = timeMillis - preHorizonDurationMillis
                            if (timeInHorizonMillis < durationMillisInHorizon) {
                                horizonStartProgress +
                                    (coarseStartProgress - horizonStartProgress) /
                                        durationMillisInHorizon * timeInHorizonMillis
                            } else {
                                coarseStartProgress +
                                    (1f - coarseStartProgress) /
                                        (horizonMillis - durationMillisInHorizon) *
                                        (timeInHorizonMillis - durationMillisInHorizon)
                            }
                        }
                    }

                    val measuredVelocity =
                        measureVelocity(
                            startPosition,
                            endPosition,
                            durationMillis,
                            progressFunction,
                            velocityTracker,
                        )
                    val error = (measuredVelocity - targetVelocity).getMagnitude()
                    if (error < minError) {
                        minError = error
                        bestDurationMillis = durationMillisInHorizon
                        bestStartProgress = coarseStartProgress
                        bestProgressFunction = progressFunction
                        if (error < ExactMatchErrorThreshold) {
                            return bestProgressFunction
                        }
                    }
                }
                durationMillisInHorizon += durationStepMillis
            }

            // Fine search around (bestDurationMillis, bestStartProgress)
            val fineMinDurationMillis = max(1L, bestDurationMillis - durationStepMillis)
            val fineMaxDurationMillis =
                min(horizonMillis - 1L, bestDurationMillis + durationStepMillis)
            val fineMinStartProgress = max(0f, bestStartProgress - FineSearchProgressWindow)
            val fineMaxStartProgress = min(1f, bestStartProgress + FineSearchProgressWindow)
            val fineProgressStep =
                max(
                    MinFineProgressStep,
                    (fineMaxStartProgress - fineMinStartProgress) / FineSearchProgressDivider,
                )

            var fineDurationMillis = fineMinDurationMillis
            while (fineDurationMillis <= fineMaxDurationMillis) {
                var fineStartProgress = fineMinStartProgress
                while (fineStartProgress <= fineMaxStartProgress) {
                    val progressFunction = SwipeFunction { timeMillis ->
                        if (timeMillis <= 0L) 0f
                        else if (timeMillis >= durationMillis) 1f
                        else if (
                            preHorizonDurationMillis > 0 && timeMillis < preHorizonDurationMillis
                        ) {
                            (horizonStartProgress / preHorizonDurationMillis) * timeMillis
                        } else {
                            val timeInHorizonMillis = timeMillis - preHorizonDurationMillis
                            if (timeInHorizonMillis < fineDurationMillis) {
                                horizonStartProgress +
                                    (fineStartProgress - horizonStartProgress) /
                                        fineDurationMillis * timeInHorizonMillis
                            } else {
                                fineStartProgress +
                                    (1f - fineStartProgress) /
                                        (horizonMillis - fineDurationMillis) *
                                        (timeInHorizonMillis - fineDurationMillis)
                            }
                        }
                    }

                    val measuredVelocity =
                        measureVelocity(
                            startPosition,
                            endPosition,
                            durationMillis,
                            progressFunction,
                            velocityTracker,
                        )
                    val error = (measuredVelocity - targetVelocity).getMagnitude()
                    if (error < minError) {
                        minError = error
                        bestProgressFunction = progressFunction
                        if (error < ExactMatchErrorThreshold) {
                            return bestProgressFunction
                        }
                    }
                    fineStartProgress += fineProgressStep
                }
                fineDurationMillis += 1L
            }

            if (minError > maxAllowedError) {
                return null
            }

            return bestProgressFunction
        }

        /**
         * Measures velocity produced by a progress function when positions are recorded into a
         * [VelocityTracker].
         */
        private fun measureVelocity(
            startPosition: Offset,
            endPosition: Offset,
            durationMillis: Long,
            progressFunc: SwipeFunction,
            velocityTracker: VelocityTracker,
        ): Velocity {
            velocityTracker.resetTracking()
            val eventPeriod = InputDispatcher.eventPeriodMillis
            val steps = max(1, (durationMillis / eventPeriod.toFloat()).roundToInt())
            for (step in 0..steps) {
                val stepProgressFraction = step / steps.toFloat()
                val timeMillis = (stepProgressFraction * durationMillis).roundToLong()
                val progress = progressFunc.calculateProgress(timeMillis).coerceIn(0f, 1f)
                val position =
                    Offset(
                        startPosition.x + (endPosition.x - startPosition.x) * progress,
                        startPosition.y + (endPosition.y - startPosition.y) * progress,
                    )
                velocityTracker.addPosition(timeMillis, position)
            }
            return velocityTracker.calculateVelocity()
        }
    }
}

// Empirically chosen constants for PlatformVelocityPathFinder path optimization

private const val MinSpeedThreshold = 0.001f
private const val MinVelocityErrorTolerance = 1f
private const val VelocityErrorRatio = 0.10f
private const val MinStaticMoveDurationMillis = 10L
private const val HorizonStartProgressFraction = 0.05f
private const val MinNormalizedVelocity = 0.01f
private const val MaxQuadraticNormalizedVelocity = 2.0f
private const val DefaultDurationStepDivider = 20L
private const val CoarseProgressStep = 0.05f
private const val ExactMatchErrorThreshold = 1f
private const val FineSearchProgressWindow = 0.05f
private const val MinFineProgressStep = 0.001f
private const val FineSearchProgressDivider = 40f
