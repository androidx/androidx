/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.lazy

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.ComposeExecutionControl
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.SubcomposeLayoutReuseTestCase
import androidx.compose.testutils.benchmark.benchmarkReuseFor
import androidx.compose.testutils.setupContent
import androidx.compose.ui.platform.ViewRootForTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ReuseBenchmark {

    @get:Rule
    val rule = ComposeBenchmarkRule()

    @Test
    fun create_button() {
        rule.benchmarkCreateFor {
            Button(onClick = {}) {
                Text("Hello")
            }
        }
    }

    @Test
    fun create_button_different_text() {
        var inc = 0
        rule.benchmarkCreateFor {
            Button(onClick = {}) {
                val text = remember { "Hello ${inc++}" }
                Text(text)
            }
        }
    }

    @Test
    fun create_lazy_column() {
        rule.benchmarkCreateFor {
            LazyColumn {
                items(10) {
                    Button(onClick = {}) {
                        Text("Hello")
                    }
                }
            }
        }
    }

    @Test
    fun reuse_button() {
        rule.benchmarkReuseFor {
            Button(onClick = {}) {
                Text("Hello")
            }
        }
    }

    @Test
    fun reuse_button_different_text() {
        var inc = 0
        rule.benchmarkReuseFor {
            Button(onClick = {}) {
                val text = remember { "Hello ${inc++}" }
                Text(text)
            }
        }
    }

    @Test
    fun reuse_lazy_column() {
        rule.benchmarkReuseFor {
            LazyColumn {
                items(10) {
                    Button(onClick = {}) {
                        Text("Hello")
                    }
                }
            }
        }
    }

    @Test
    fun dispose_lazy_column() {
        rule.disposeBenchmark {
            SubcomposeLayoutReuseTestCase(reusableSlots = 0) {
                LazyColumn {
                    items(10) {
                        Button(onClick = {}) {
                            Text("Hello")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun deactivate_lazy_column() {
        rule.disposeBenchmark {
            SubcomposeLayoutReuseTestCase(reusableSlots = 1) {
                LazyColumn {
                    items(10) {
                        Button(onClick = {}) {
                            Text("Hello")
                        }
                    }
                }
            }
        }
    }
}

internal fun ComposeExecutionControl.doFramesUntilIdle() {
    do {
        doFrame()
    } while (hasPendingChanges() || hasPendingMeasureOrLayout())
}

private fun ComposeExecutionControl.hasPendingMeasureOrLayout(): Boolean {
    return (getHostView() as ViewRootForTest).hasPendingMeasureOrLayout
}

private fun ComposeBenchmarkRule.benchmarkCreateFor(content: @Composable () -> Unit) {
    createBenchmark {
        SubcomposeLayoutReuseTestCase(reusableSlots = 0, content)
    }
}

private fun ComposeBenchmarkRule.createBenchmark(
    testCase: () -> SubcomposeLayoutReuseTestCase,
) {
    runBenchmarkFor(testCase) {
        runOnUiThread {
            setupContent()
            doFramesUntilIdle()
        }

        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().clearContent()
                doFramesUntilIdle()
                assertNoPendingChanges()
            }

            getTestCase().initContent()
            doFramesUntilIdle()
        }
    }
}

private fun ComposeBenchmarkRule.disposeBenchmark(
    testCase: () -> SubcomposeLayoutReuseTestCase,
) {
    runBenchmarkFor(testCase) {
        runOnUiThread {
            setupContent()
            doFramesUntilIdle()
            assertNoPendingChanges()
        }

        measureRepeatedOnUiThread {
            getTestCase().clearContent()
            doFramesUntilIdle()

            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().initContent()
                doFramesUntilIdle()
                assertNoPendingChanges()
            }
        }
    }
}
