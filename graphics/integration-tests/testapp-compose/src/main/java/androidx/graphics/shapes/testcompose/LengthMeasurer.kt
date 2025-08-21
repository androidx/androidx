/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.ui.geometry.Offset
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Feature

/**
 * Copied code from internal Measurer to measure cubics. Reason for copy: We split cubics depending
 * on length threshold, therefore we need a way to know their lengths.
 */
internal class LengthMeasurer() {
    fun measureCubic(cubic: Cubic): Float {
        var total = 0f
        var prev = Offset(cubic.anchor0X, cubic.anchor0Y)

        for (i in 1..3) {
            val progress = i.toFloat() / 3
            val point = cubic.pointOnCurve(progress)
            val segment = (point - prev).getDistance()

            total += segment
            prev = point
        }

        return total
    }

    fun measureFeature(feature: Feature): Float = feature.cubics.map { measureCubic(it) }.sum()
}
