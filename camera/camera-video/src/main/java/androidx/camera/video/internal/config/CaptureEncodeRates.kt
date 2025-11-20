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

package androidx.camera.video.internal.config

import android.util.Rational
import androidx.camera.core.Logger
import kotlin.math.roundToInt

/**
 * Represents a pair of capture and encode rates.
 *
 * @property captureRate The capture rate.
 * @property encodeRate The encode rate.
 */
internal data class CaptureEncodeRates(val captureRate: Int, val encodeRate: Int)

private const val TAG = "CaptureEncodeRates"

/** Calculates the encoding rate based on the capture rate and a capture-to-encode ratio. */
internal fun toEncodeRate(captureRate: Int, captureToEncodeRatio: Rational?): Int {
    if (captureToEncodeRatio == null) {
        return captureRate
    }
    if (isInvalidCaptureToEncodeRatio(captureToEncodeRatio)) {
        Logger.w(TAG, "Invalid capture-to-encode ratio: $captureToEncodeRatio")
        return captureRate
    }
    return (captureRate / captureToEncodeRatio.toFloat()).roundToInt()
}

/** Calculates the capture rate based on the encoding rate and a capture-to-encode ratio. */
internal fun toCaptureRate(encodeRate: Int, captureToEncodeRatio: Rational?): Int {
    if (captureToEncodeRatio == null) {
        return encodeRate
    }
    if (isInvalidCaptureToEncodeRatio(captureToEncodeRatio)) {
        Logger.w(TAG, "Invalid capture-to-encode ratio: $captureToEncodeRatio")
        return encodeRate
    }
    return (encodeRate * captureToEncodeRatio.toFloat()).roundToInt()
}

private fun isInvalidCaptureToEncodeRatio(ratio: Rational): Boolean =
    ratio == Rational.NaN ||
        ratio == Rational.ZERO ||
        ratio == Rational.NEGATIVE_INFINITY ||
        ratio == Rational.POSITIVE_INFINITY
