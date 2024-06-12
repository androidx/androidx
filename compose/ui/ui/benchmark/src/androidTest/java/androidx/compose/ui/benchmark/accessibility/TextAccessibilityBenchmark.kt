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

package androidx.compose.ui.benchmark.accessibility

import android.view.View
import android.view.accessibility.AccessibilityNodeProvider
import android.view.accessibility.AccessibilityNodeProvider.HOST_VIEW_ID
import androidx.annotation.UiThread
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TextAccessibilityBenchmark(
    private val accessibilityEnabled: Boolean,
    private val invalidateSemanticsOnEachRun: Boolean
) {

    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(MicrobenchmarkConfig(traceAppTagEnabled = true))

    lateinit var view: View
    lateinit var nodeProvider: AccessibilityNodeProvider

    @Test
    fun createAccessibilityNodeInfoFromId_singleTextComponent() {
        if (!accessibilityEnabled) return

        measureRepeatedOnUiThread(
            content = { Text("Text Composable", Modifier.testTag("tag")) },
            benchmark = {
                val semanticsId = runWithTimingDisabled { findIdByTag("tag") }
                nodeProvider.createAccessibilityNodeInfo(semanticsId)
            }
        )
    }

    @Test
    fun createAccessibilityNodeInfoFromId_singleOfMultipleTextComponents() {
        if (!accessibilityEnabled) return

        measureRepeatedOnUiThread(
            content = {
                Column {
                    Text(modifier = Modifier.testTag("tag"), text = "Text Composable")
                    repeat(9) { Text("Text Composable") }
                }
            },
            benchmark = {
                val semanticsId = runWithTimingDisabled { findIdByTag("tag") }
                nodeProvider.createAccessibilityNodeInfo(semanticsId)
            }
        )
    }

    /**
     * We don't use the testTag here, but retained it to make this test comparable with
     * [createAccessibilityNodeInfoFromId_singleTextComponent].
     */
    @Test
    fun createAccessibilityNodeInfoFromRoot_singleTextComponent() {
        if (!accessibilityEnabled) return

        measureRepeatedOnUiThread(
            content = { Text("Text Composable", Modifier.testTag("text")) },
            benchmark = { nodeProvider.createAccessibilityNodeInfo(HOST_VIEW_ID) }
        )
    }

    /**
     * We don't use the testTag here, but retained it to make this test comparable with
     * [createAccessibilityNodeInfoFromId_singleOfMultipleTextComponents].
     */
    @Test
    fun createAccessibilityNodeInfoFromRoot_multipleTextComponents() {
        if (!accessibilityEnabled) return

        measureRepeatedOnUiThread(
            content = {
                Column {
                    Text(modifier = Modifier.testTag("text"), text = "Text Composable")
                    repeat(9) { Text("Text Composable") }
                }
            },
            benchmark = { nodeProvider.createAccessibilityNodeInfo(HOST_VIEW_ID) }
        )
    }

    @Test
    fun sendEvents_addRemoveSingleTextComponent() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    var include by mutableStateOf(true)

                    @Composable
                    override fun Content() {
                        setupAccessibility()
                        if (include) Text("abc")
                    }

                    override fun toggleState() {
                        include = !include
                        if (invalidateSemanticsOnEachRun) invalidateSemantics()
                    }
                }
            }
        )
    }

    @Test
    fun sendEvents_addRemoveMultipleTextComponents() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    var include by mutableStateOf(true)

                    @Composable
                    override fun Content() {
                        setupAccessibility()
                        Column {
                            if (include) {
                                repeat(10) { Text("abc") }
                            }
                        }
                    }

                    override fun toggleState() {
                        include = !include
                        if (invalidateSemanticsOnEachRun) invalidateSemantics()
                    }
                }
            }
        )
    }

    @Test
    fun sendEvents_changeNumberOfTextComponents() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    var count by mutableStateOf(5)

                    @Composable
                    override fun Content() {
                        setupAccessibility()
                        Column { repeat(count) { Text("abc") } }
                    }

                    override fun toggleState() {
                        count = if (count == 5) 10 else 5
                        if (invalidateSemanticsOnEachRun) invalidateSemantics()
                    }
                }
            }
        )
    }

    @Test
    fun sendEvents_changeTextInSingleTextComponent() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    var initialValue by mutableStateOf(true)

                    @Composable
                    override fun Content() {
                        setupAccessibility()
                        Text(if (initialValue) "abc" else "def")
                    }

                    override fun toggleState() {
                        initialValue = !initialValue
                        if (invalidateSemanticsOnEachRun) invalidateSemantics()
                    }
                }
            }
        )
    }

    @Test
    fun sendEvents_changeTextInMultipleTextComponents() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    var initialValue by mutableStateOf(true)

                    @Composable
                    override fun Content() {
                        setupAccessibility()

                        Column {
                            Text(if (initialValue) "abc" else "ABC")
                            Text(if (initialValue) "def" else "DEF")
                            Text(if (initialValue) "ghi" else "GHI")
                            Text(if (initialValue) "jkl" else "JKL")
                            Text(if (initialValue) "lmn" else "MNO")
                            Text(if (initialValue) "opq" else "OPQ")
                            Text(if (initialValue) "rst" else "RST")
                            Text(if (initialValue) "uvw" else "UVW")
                            Text(if (initialValue) "xyz" else "XYZ")
                        }
                    }

                    override fun toggleState() {
                        initialValue = !initialValue
                        if (invalidateSemanticsOnEachRun) invalidateSemantics()
                    }
                }
            }
        )
    }

    @Test
    fun sendEvents_changeTextInOneOfMultipleText() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    var initialValue by mutableStateOf(true)

                    @Composable
                    override fun Content() {
                        setupAccessibility()
                        Column {
                            Text("abc")
                            Text("def")
                            Text("ghi")
                            Text(if (initialValue) "jkl" else "JKL")
                            Text("opq")
                            Text("rst")
                            Text("uvw")
                            Text("xyz")
                        }
                    }

                    override fun toggleState() {
                        initialValue = !initialValue
                        if (invalidateSemanticsOnEachRun) invalidateSemantics()
                    }
                }
            }
        )
    }

    @Test
    fun sendEvents_scrollTest() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = {
                object : ComposeTestCase, ToggleableTestCase {
                    lateinit var state: LazyListState
                    lateinit var coroutineScope: CoroutineScope

                    @Composable
                    override fun Content() {
                        setupAccessibility()
                        state = rememberLazyListState()
                        coroutineScope = rememberCoroutineScope()

                        LazyColumn(Modifier.height(600.dp), state) {
                            items(300) { Text("item $it", Modifier.height(60.dp)) }
                        }
                    }

                    override fun toggleState() {
                        coroutineScope.launch {
                            state.scrollToItem(if (state.firstVisibleItemIndex == 0) 200 else 0)
                            if (invalidateSemanticsOnEachRun) invalidateSemantics()
                        }
                    }
                }
            }
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "accessibilityEnabled = {0}, invalidateSemanticsOnEachRun = {1}"
        )
        fun initParameters() =
            listOf(arrayOf(false, false), arrayOf(true, false), arrayOf(true, true))
    }

    private fun findIdByTag(@Suppress("SameParameterValue") tag: String): Int {
        return (view as RootForTest)
            .semanticsOwner
            .getAllSemanticsNodes(mergingEnabled = false)
            .find { it.config.getOrNull(SemanticsProperties.TestTag) == tag }!!
            .id
    }

    private fun measureRepeatedOnUiThread(
        content: @Composable () -> Unit,
        @UiThread benchmark: BenchmarkRule.Scope.() -> Unit
    ) {
        benchmarkRule.runBenchmarkFor(
            givenTestCase = {
                object : ComposeTestCase {
                    @Composable
                    override fun Content() {
                        view = LocalView.current
                        nodeProvider = view.accessibilityNodeProvider
                        setupAccessibility()
                        content()
                    }
                }
            }
        ) {
            benchmarkRule.measureRepeatedOnUiThread {
                runWithTimingDisabled {
                    doFrame()
                    assertNoPendingChanges()
                    if (invalidateSemanticsOnEachRun) invalidateSemantics()
                }
                benchmark()
                runWithTimingDisabled { disposeContent() }
            }
        }
    }

    @Composable
    private fun setupAccessibility() {
        view = LocalView.current
        // TODO(b/308007375): Eventually we will be able to remove `accessibilityForTesting()`;
        // this is just a temporary workaround for now.
        LaunchedEffect(Unit) {
            // Make sure the delay between batches of a11y events is set to zero.
            (view as RootForTest).setAccessibilityEventBatchIntervalMillis(0L)
            // Ensure that accessibility is enabled for testing.
            (view as RootForTest).forceAccessibilityForTesting(accessibilityEnabled)
        }
    }

    private fun invalidateSemantics() {
        // Setting forceAccessibilityForTesting invalidates semantics.
        (view as RootForTest).forceAccessibilityForTesting(accessibilityEnabled)
    }
}
