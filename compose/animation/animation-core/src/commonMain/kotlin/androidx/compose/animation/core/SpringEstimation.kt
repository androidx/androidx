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

package androidx.compose.animation.core

import androidx.annotation.RestrictTo
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Maximum duration in Milliseconds.
 *
 * This takes into account that the duration will be converted to nanos.
 */
private const val MAX_LONG_MILLIS: Long = Long.MAX_VALUE / 1_000_000

/** Returns the estimated time that the spring will last be at [delta] */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun estimateAnimationDurationMillis(
    stiffness: Float,
    dampingRatio: Float,
    initialVelocity: Float,
    initialDisplacement: Float,
    delta: Float
): Long {
    if (dampingRatio == 0f) {
        // No damping, duration is infinite (max value).
        return MAX_LONG_MILLIS
    }

    return estimateAnimationDurationMillis(
        stiffness = stiffness.toDouble(),
        dampingRatio = dampingRatio.toDouble(),
        initialVelocity = initialVelocity.toDouble(),
        initialDisplacement = initialDisplacement.toDouble(),
        delta = delta.toDouble()
    )
}

/** Returns the estimated time that the spring will last be at [delta] */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun estimateAnimationDurationMillis(
    stiffness: Double,
    dampingRatio: Double,
    initialVelocity: Double,
    initialDisplacement: Double,
    delta: Double
): Long {
    val dampingCoefficient = 2.0 * dampingRatio * sqrt(stiffness)

    // Compute the roots of the polynomial [a]x^2+[b]x+[c]=0 which may be complex.
    // Here a is set to the constant 1.0, and folded into the other computations
    val partialRoot = dampingCoefficient * dampingCoefficient - 4.0 * stiffness
    val partialRootReal = if (partialRoot < 0.0) 0.0 else sqrt(partialRoot)
    val partialRootImaginary = if (partialRoot < 0.0) sqrt(abs(partialRoot)) else 0.0

    val firstRootReal = (-dampingCoefficient + partialRootReal) * 0.5
    val firstRootImaginary = partialRootImaginary * 0.5
    val secondRootReal = (-dampingCoefficient - partialRootReal) * 0.5

    return estimateDurationInternal(
        firstRootReal,
        firstRootImaginary,
        secondRootReal,
        dampingRatio,
        initialVelocity,
        initialDisplacement,
        delta
    )
}

/** Returns the estimated time that the spring will last be at [delta] */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun estimateAnimationDurationMillis(
    springConstant: Double,
    dampingCoefficient: Double,
    mass: Double,
    initialVelocity: Double,
    initialDisplacement: Double,
    delta: Double
): Long {
    val criticalDamping = 2.0 * sqrt(springConstant * mass)
    val dampingRatio = dampingCoefficient / criticalDamping

    // Compute the roots of the polynomial [a]x^2+[b]x+[c]=0 which may be complex.
    val partialRoot = dampingCoefficient * dampingCoefficient - 4.0 * mass * springConstant
    val divisor = 1.0 / (2.0 * mass)
    val partialRootReal = if (partialRoot < 0.0) 0.0 else sqrt(partialRoot)
    val partialRootImaginary = if (partialRoot < 0.0) sqrt(abs(partialRoot)) else 0.0

    val firstRootReal = (-dampingCoefficient + partialRootReal) * divisor
    val firstRootImaginary = partialRootImaginary * divisor
    val secondRootReal = (-dampingCoefficient - partialRootReal) * divisor

    return estimateDurationInternal(
        firstRootReal,
        firstRootImaginary,
        secondRootReal,
        dampingRatio,
        initialVelocity,
        initialDisplacement,
        delta
    )
}

/**
 * In the under-damped case we simply calculate the envelope of the function. The general solution
 * is of the form x(t) = c_1*e^(r*t)*cos(...) + c_2*e^(r*t)sin(...) which simplifies to x(t) =
 * c*e^(r*t)*cos(...) where c*e^(r*t) is the envelope of x(t)
 */
private fun estimateUnderDamped(
    firstRootReal: Double,
    firstRootImaginary: Double,
    p0: Double,
    v0: Double,
    delta: Double
): Double {
    val r = firstRootReal
    val c1 = p0
    val c2 = (v0 - r * c1) / firstRootImaginary
    val c = sqrt(c1 * c1 + c2 * c2)

    return ln(delta / c) / r
}

/**
 * In the critically-damped case we apply Newton-Raphson's iterative numerical method of solving the
 * equation x(t) = c_1*e^(r*t) + c_2*t*e^(r*t)
 */
private fun estimateCriticallyDamped(
    firstRootReal: Double,
    p0: Double,
    v0: Double,
    delta: Double
): Double {
    val r = firstRootReal
    val c1 = p0
    val c2 = v0 - r * c1

    // For our initial guess, determine the max t of c_1*e^(r*t) = delta and
    // c_2*t*e^(r*t) = delta
    val t1 = ln(abs(delta / c1)) / r
    val t2 =
        run {
            // Application of Lambert's W function to solve te^t
            val guess = ln(abs(delta / c2))
            var t = guess
            for (i in 0..5) {
                t = (guess - ln(abs(t / r)))
            }
            t
        } / r
    var tCurr =
        when {
            t1.isNotFinite() -> t2
            t2.isNotFinite() -> t1
            else -> max(t1, t2)
        }

    // Calculate the inflection time. This is important if the inflection is in t > 0
    val tInflection = -(r * c1 + c2) / (r * c2)
    val xInflection = c1 * exp(r * tInflection) + c2 * tInflection * exp(r * tInflection)

    // For inflection that does not exist in real time, we always solve for x(t)=delta. Note
    // the system is manipulated such that p0 is always positive.
    val signedDelta =
        if (tInflection.isNaN() || tInflection <= 0.0) {
            -delta
        } else if (tInflection > 0.0 && -xInflection < delta) {
            // In this scenario the first crossing with the threshold is to be found. Note that
            // the inflection does not exceed delta. As such, we search from the left.
            if (c2 < 0 && c1 > 0) {
                tCurr = 0.0
            }
            -delta
        } else {
            // In this scenario there are three total crossings of the threshold, once from
            // above, and then once when the inflection exceeds the threshold and then one last
            // one when x(t) finally decays to zero. The point of determining concavity is to
            // find the final crossing.
            //
            // By finding a point between when concavity changes, and when the inflection point is,
            // Newton's method will always converge onto the rightmost point (in this case),
            // the one that we are interested in.
            val tConcavChange = -(2.0 / r) - (c1 / c2)
            tCurr = tConcavChange
            delta
        }

    var tDelta = Double.MAX_VALUE
    var iterations = 0
    while (tDelta > 0.001 && iterations < 100) {
        iterations++
        val tLast = tCurr
        tCurr =
            iterateNewtonsMethod(
                tCurr,
                { t -> (c1 + c2 * t) * exp(r * t) + signedDelta },
                { t -> (c2 * (r * t + 1) + c1 * r) * exp(r * t) }
            )
        tDelta = abs(tLast - tCurr)
    }

    return tCurr
}

/**
 * In the over-damped case we apply Newton-Raphson's iterative numerical method of solving the
 * equation x(t) = c_1*e^(r_1*t) + c_2*e^(r_2*t)
 */
private fun estimateOverDamped(
    firstRootReal: Double,
    secondRootReal: Double,
    p0: Double,
    v0: Double,
    delta: Double
): Double {
    val r1 = firstRootReal
    val r2 = secondRootReal
    val c2 = (r1 * p0 - v0) / (r1 - r2)
    val c1 = p0 - c2

    // For our initial guess, determine the max t of c_1*e^(r_1*t) = delta and
    // c_2*e^(r_2*t) = delta
    val t1 = ln(abs(delta / c1)) / r1
    val t2 = ln(abs(delta / c2)) / r2

    var tCurr =
        when {
            t1.isNotFinite() -> t2
            t2.isNotFinite() -> t1
            else -> max(t1, t2)
        }

    // Calculate the inflection time. This is important if the inflection is in t > 0
    val tInflection = ln((c1 * r1) / (-c2 * r2)) / (r2 - r1)
    fun xInflection() = c1 * exp(r1 * tInflection) + c2 * exp(r2 * tInflection)

    // For inflection that does not exist in real time, we always solve for x(t)=delta. Note
    // the system is manipulated such that p0 is always positive.
    val signedDelta =
        if (tInflection.isNaN() || tInflection <= 0.0) {
            -delta
        } else if (tInflection > 0.0 && -xInflection() < delta) {
            // In this scenario the first crossing with the threshold is to be found. Note that
            // the inflection does not exceed delta. As such, we search from the left.
            if (c2 > 0.0 && c1 < 0.0) {
                tCurr = 0.0
            }
            -delta
        } else {
            // In this scenario there are three total crossings of the threshold, once from
            // above, and then once when the inflection exceeds the threshold and then one last
            // one when x(t) finally decays to zero. The point of determining concavity is to
            // find the final crossing.
            //
            // By finding a point between when concavity changes, and when the inflection point is,
            // Newton's method will always converge onto the rightmost point (in this case),
            // the one that we are interested in.
            val tConcavChange = ln(-(c2 * r2 * r2) / (c1 * r1 * r1)) / (r1 - r2)
            tCurr = tConcavChange
            delta
        }

    // For a good initial guess, simply return
    if (abs(c1 * r1 * exp(r1 * tCurr) + c2 * r2 * exp(r2 * tCurr)) < 0.0001) {
        return tCurr
    }
    var tDelta = Double.MAX_VALUE

    // Cap iterations for safety - Experimentally this method takes <= 5 iterations
    var iterations = 0
    while (tDelta > 0.001 && iterations < 100) {
        iterations++
        val tLast = tCurr
        tCurr =
            iterateNewtonsMethod(
                tCurr,
                { t -> c1 * exp(r1 * t) + c2 * exp(r2 * t) + signedDelta },
                { t -> c1 * r1 * exp(r1 * t) + c2 * r2 * exp(r2 * t) }
            )
        tDelta = abs(tLast - tCurr)
    }

    return tCurr
}

// Applies Newton-Raphson's method to solve for the estimated time the spring mass system will
// last be at [delta].
private fun estimateDurationInternal(
    firstRootReal: Double,
    firstRootImaginary: Double,
    secondRootReal: Double,
    dampingRatio: Double,
    initialVelocity: Double,
    initialPosition: Double,
    delta: Double
): Long {
    if (initialPosition == 0.0 && initialVelocity == 0.0) {
        return 0L
    }

    val v0 = if (initialPosition < 0) -initialVelocity else initialVelocity
    val p0 = abs(initialPosition)

    return (when {
            dampingRatio > 1.0 ->
                estimateOverDamped(firstRootReal, secondRootReal, p0 = p0, v0 = v0, delta = delta)
            dampingRatio < 1.0 ->
                estimateUnderDamped(
                    firstRootReal,
                    firstRootImaginary,
                    v0 = v0,
                    p0 = p0,
                    delta = delta
                )
            else -> estimateCriticallyDamped(firstRootReal, p0 = p0, v0 = v0, delta = delta)
        } * 1000.0)
        .toLong()
}

private inline fun iterateNewtonsMethod(
    x: Double,
    fn: (Double) -> Double,
    fnPrime: (Double) -> Double
): Double {
    return x - fn(x) / fnPrime(x)
}

@Suppress("NOTHING_TO_INLINE") private inline fun Double.isNotFinite() = !fastIsFinite()
