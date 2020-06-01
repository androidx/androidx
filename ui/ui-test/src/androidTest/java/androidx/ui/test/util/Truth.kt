/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.util

import androidx.ui.unit.PxPosition
import com.google.common.truth.FloatSubject
import com.google.common.truth.Truth.assertThat
import kotlin.math.sign

/**
 * Checks if the subject is within [tolerance] of [f]. Shorthand for
 * `isWithin([tolerance]).of([f])`.
 */
fun FloatSubject.isAlmostEqualTo(f: Float, tolerance: Float) {
    isWithin(tolerance).of(f)
}

/**
 * Verifies that the [PxPosition] is equal to the given position with some tolerance. The default
 * tolerance is 0.001.
 */
fun PxPosition.isAlmostEqualTo(position: PxPosition, tolerance: Float = 1e-3f) {
    assertThat(x).isAlmostEqualTo(position.x, tolerance)
    assertThat(y).isAlmostEqualTo(position.y, tolerance)
}

/**
 * Checks that the values are progressing in a monotonic direction between [a] and [b].
 * If [a] and [b] are equal, all values in the list should be that value too. The edges [a] and
 * [b] allow a [tolerance] for floating point imprecision, which is by default `0.001`.
 */
fun List<Float>.isMonotonicBetween(a: Float, b: Float, tolerance: Float = 1e-3f) {
    val expectedSign = sign(b - a)
    if (expectedSign == 0f) {
        forEach { assertThat(it).isAlmostEqualTo(a, tolerance) }
    } else {
        forEach { assertThat(it).isAlmostBetween(a, b, tolerance) }
        zipWithNext { curr, next -> sign(next - curr) }.forEach {
            if (it != 0f) assertThat(it).isEqualTo(expectedSign)
        }
    }
}

/**
 * Checks that the float value is between [a] and [b], allowing a [tolerance] on either side.
 * The order of [a] and [b] doesn't matter, the float value must be _between_ them. The default
 * tolerance is `0.001`.
 */
fun FloatSubject.isAlmostBetween(a: Float, b: Float, tolerance: Float = 1e-3f) {
    if (a < b) {
        isAtLeast(a - tolerance)
        isAtMost(b + tolerance)
    } else {
        isAtLeast(b - tolerance)
        isAtMost(a + tolerance)
    }
}
