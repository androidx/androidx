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

package androidx.compose.ui.graphics.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ColorBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun colorLerp() {
        benchmarkRule.measureRepeated {
            for (i in 0..500) {
                lerp(Color.Red, Color.Green, i / 500.0f)
            }
        }
    }

    @Test
    fun wideColorLerp() {
        val start = Color(1.0f, 0.0f, 0.0f, 1.0f, ColorSpaces.DisplayP3)
        val end = Color(0.0f, 1.0f, 0.0f, 1.0f, ColorSpaces.DisplayP3)
        benchmarkRule.measureRepeated {
            for (i in 0..500) {
                lerp(start, end, i / 500.0f)
            }
        }
    }
}
