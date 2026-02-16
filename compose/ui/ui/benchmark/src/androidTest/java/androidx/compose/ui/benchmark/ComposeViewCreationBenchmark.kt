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

package androidx.compose.ui.benchmark

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import org.junit.Rule
import org.junit.Test

class ComposeViewCreationBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun createComposeView() {
        with(benchmarkRule) {
            runBenchmarkFor({ JustCreateComposeViewTestCase() }) {
                runOnUiThread { doFramesUntilNoChangesPending() }
                measureRepeatedOnUiThread {
                    val containingView = getTestCase().containingView
                    val composeView =
                        ComposeView(containingView.context).also {
                            it.setContent { Box(Modifier.fillMaxSize()) }
                        }
                    containingView.addView(composeView)
                    recompose()
                    runWithMeasurementDisabled { containingView.removeAllViews() }
                }
            }
        }
    }

    private class JustCreateComposeViewTestCase : ComposeTestCase {
        lateinit var containingView: FrameLayout

        @Composable
        override fun Content() {
            AndroidView(factory = { context -> FrameLayout(context).also { containingView = it } })
        }
    }
}
