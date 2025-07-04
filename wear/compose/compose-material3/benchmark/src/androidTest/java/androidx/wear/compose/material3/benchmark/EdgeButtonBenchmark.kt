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

package androidx.wear.compose.material3.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// We run two benchmarks, one for a normal Button and one for an EdgeButton,
// so we can compare.
@MediumTest
@RunWith(Parameterized::class)
class EdgeButtonBenchmark(private val edgeButton: EdgeButtonType) {
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = EdgeButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val buttonTestCaseFactory = { EdgeButtonTestCase(edgeButton) }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(buttonTestCaseFactory)
    }
}

internal class EdgeButtonTestCase(private val edgeButton: EdgeButtonType) :
    LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        Box(Modifier.size(200.dp)) {
            if (edgeButton == EdgeButtonType.Normal) {
                Button(onClick = { /* do something */ }) { Text("Button") }
            } else {
                EdgeButton(onClick = { /* do something */ }) { Text("Edge Button") }
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class EdgeButtonType {
    Normal,
    EdgeButton,
}
