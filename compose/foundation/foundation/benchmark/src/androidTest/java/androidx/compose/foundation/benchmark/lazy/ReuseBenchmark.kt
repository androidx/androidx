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
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.setupContent
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
    fun a_test_for_warmup() {
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
            CreateTestCase {
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
            ReuseTestCase {
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

private fun ComposeBenchmarkRule.benchmarkCreateFor(content: @Composable () -> Unit) {
    createBenchmark {
        CreateTestCase(content)
    }
}

private fun ComposeBenchmarkRule.benchmarkReuseFor(content: @Composable () -> Unit) {
    createBenchmark {
        ReuseTestCase(content)
    }
}

private fun ComposeBenchmarkRule.createBenchmark(
    testCase: () -> BaseReuseTestCase,
) {
    runBenchmarkFor(testCase) {
        setupContent()
        while (hasPendingChanges()) {
            doFrame()
        }

        measureRepeated {
            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().clearContent()
                while (hasPendingChanges()) {
                    doFrame()
                }
                assertNoPendingChanges()
            }

            getTestCase().initContent()
            while (hasPendingChanges()) {
                doFrame()
            }
        }
    }
}

private fun ComposeBenchmarkRule.disposeBenchmark(
    testCase: () -> BaseReuseTestCase,
) {
    runBenchmarkFor(testCase) {
        setupContent()
        while (hasPendingChanges()) {
            doFrame()
        }
        assertNoPendingChanges()

        measureRepeated {
            getTestCase().clearContent()
            while (hasPendingChanges()) {
                doFrame()
            }

            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().initContent()
                while (hasPendingChanges()) {
                    doFrame()
                }
                assertNoPendingChanges()
            }
        }
    }
}

private interface BaseReuseTestCase : ComposeTestCase {
    fun clearContent()
    fun initContent()
}

private class CreateTestCase(private val content: @Composable () -> Unit) : BaseReuseTestCase {
    private var active by mutableStateOf(true)

    @Composable
    override fun Content() {
        if (active) {
            content()
        }
    }

    override fun clearContent() {
        active = false
    }

    override fun initContent() {
        active = true
    }
}

private class ReuseTestCase(private val content: @Composable () -> Unit) : BaseReuseTestCase {
    private var active by mutableStateOf(true)

    @Composable
    override fun Content() {
        ReusableContentHost(active = active) {
            content()
        }
    }

    override fun clearContent() {
        active = false
    }

    override fun initContent() {
        active = true
    }
}
