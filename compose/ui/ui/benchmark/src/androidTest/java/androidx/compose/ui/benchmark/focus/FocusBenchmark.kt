/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.benchmark.focus

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_TAB
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FocusBenchmark {

    @get:Rule val composeBenchmarkRule = ComposeBenchmarkRule()

    @Test
    fun focusTarget() {
        composeBenchmarkRule.benchmarkToFirstPixel {
            object : LayeredComposeTestCase() {
                @Composable
                override fun MeasuredContent() {
                    Box(Modifier.focusTarget())
                }
            }
        }
    }

    @Test
    fun focusTraversal() {
        composeBenchmarkRule.runBenchmarkFor({
            object : ComposeTestCase {
                @Composable
                override fun Content() {
                    Column(Modifier.fillMaxSize()) {
                        repeat(10) {
                            Row(Modifier.focusTarget()) {
                                repeat(10) { Box(Modifier.focusTarget()) }
                            }
                        }
                    }
                }
            }
        }) {
            composeBenchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            composeBenchmarkRule.measureRepeatedOnUiThread {
                getHostView().dispatchKeyEvent(KeyEvent(ACTION_DOWN, KEYCODE_TAB))
                getHostView().dispatchKeyEvent(KeyEvent(ACTION_UP, KEYCODE_TAB))
            }
        }
    }
}
