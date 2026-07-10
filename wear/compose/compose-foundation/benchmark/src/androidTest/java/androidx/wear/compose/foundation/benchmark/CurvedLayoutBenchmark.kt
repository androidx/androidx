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

package androidx.wear.compose.foundation.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Benchmark for Wear Compose CurvedLayout. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CurvedLayoutBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactoryFactory = { warped: CurvedTextStyle.WarpOffset ->
        { ClCaseFactory(warped) }
    }

    @Test
    fun first_pixel_base() =
        benchmarkRule.benchmarkToFirstPixel(caseFactoryFactory(CurvedTextStyle.WarpOffset.None))

    @Test
    fun first_pixel_warped() =
        benchmarkRule.benchmarkToFirstPixel(
            caseFactoryFactory(CurvedTextStyle.WarpOffset.HalfOpticalHeight)
        )
}

private class ClCaseFactory(val warpOffset: CurvedTextStyle.WarpOffset) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val style = CurvedTextStyle(color = Color.White, warpOffset = warpOffset)
        CurvedLayout(Modifier.fillMaxSize().background(Color.Black)) {
            basicCurvedText("مرحبا 👋 بالعالم 🌏!", style)
            basicCurvedText("Some more text to add around here", style)
        }
    }
}
