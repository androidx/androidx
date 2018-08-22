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

package androidx.ui.painting.alignment

import androidx.ui.text.TextDirection
import androidx.ui.truncDiv

class _MixedAlignment(x: Double, start: Double, y: Double) : AlignmentGeometry() {

    override val _x = x

    override val _start = start

    override val _y = y

    override operator fun unaryMinus(): _MixedAlignment {
        return _MixedAlignment(
                -_x,
                -_start,
                -_y
        )
    }

    override operator fun times(other: Double): _MixedAlignment {
        return _MixedAlignment(
                _x * other,
                _start * other,
                _y * other
        )
    }

    override operator fun div(other: Double): _MixedAlignment {
        return _MixedAlignment(
                _x / other,
                _start / other,
                _y / other
        )
    }

    override fun truncDiv(other: Double): _MixedAlignment {
        return _MixedAlignment(
                (_x.truncDiv(other)).toDouble(),
                (_start.truncDiv(other)).toDouble(),
                (_y.truncDiv(other)).toDouble()
        )
    }

    override operator fun mod(other: Double): _MixedAlignment {
        return _MixedAlignment(
                _x % other,
                _start % other,
                _y % other
        )
    }

    override fun resolve(direction: TextDirection?): Alignment? {
        assert(direction != null)

        return when (direction) {
            TextDirection.RTL -> Alignment(_x - _start, _y)
            TextDirection.LTR -> Alignment(_x + _start, _y)
            else -> null
        }
    }
}