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

package androidx.compose.ui.benchmark.autofill

import android.view.View
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.platform.LocalView
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@SdkSuppress(minSdkVersion = 26)
@RunWith(JUnit4::class)
class PopulateAutofillStructBenchmark {
    private lateinit var ownerView: View

    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(MicrobenchmarkConfig(traceAppTagEnabled = true))

    @Test
    fun populateViewStructureBenchmark_textScreen() {
        measurePopulateViewStructureRepeatedOnUiThread { AutofillTextScreen() }
    }

    @Test
    fun populateViewStructureBenchmark_autofillScreen() {
        measurePopulateViewStructureRepeatedOnUiThread { AutofillScreen() }
    }

    private fun measurePopulateViewStructureRepeatedOnUiThread(content: @Composable () -> Unit) {
        benchmarkRule.runBenchmarkFor(
            givenTestCase = {
                object : ComposeTestCase {
                    @Composable
                    override fun Content() {
                        ownerView = LocalView.current
                        content()
                    }
                }
            }
        ) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }
            benchmarkRule.measureRepeatedOnUiThread {
                ownerView.onProvideAutofillVirtualStructure(FakeViewStructure(), 0)
            }
        }
    }
}
