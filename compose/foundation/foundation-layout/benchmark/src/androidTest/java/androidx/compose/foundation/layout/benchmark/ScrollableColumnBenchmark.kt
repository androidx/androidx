/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkFirstSemanticsUpdate
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ScrollableColumnBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()
    private val testCaseFactory = { ScrollableColumnTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(testCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(testCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(testCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(testCaseFactory)
    }

    @Test
    fun first_semantics() {
        benchmarkRule.benchmarkFirstSemanticsUpdate(testCaseFactory)
    }

    @Test
    fun changeScroll_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(testCaseFactory, toggleCausesRecompose = false)
    }

    @Test
    fun changeScroll_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(testCaseFactory, toggleCausesRecompose = false)
    }

    @Test
    fun changeScroll_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(testCaseFactory, toggleCausesRecompose = false)
    }

    @Test
    fun changeScroll_semantics() {
        benchmarkRule.toggleStateBenchmarkSemantics(testCaseFactory, toggleCausesRecompose = false)
    }
}

class ScrollableColumnTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    // ScrollerPosition must now be constructed during composition to obtain the Density
    private lateinit var scrollState: ScrollState

    @Composable
    override fun MeasuredContent() {
        (LocalView.current as RootForTest).forceAccessibilityForTesting(true)
        scrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(scrollState)) {
            repeat(5) { ClickableColumn() }
        }
    }

    override fun toggleState() {
        runBlocking { scrollState.scrollTo(if (scrollState.value == 0) 10 else 0) }
    }

    @Composable
    fun ClickableColumn() {
        val playStoreColor = Color(red = 0x00, green = 0x00, blue = 0x80)
        Column(Modifier.fillMaxWidth().clickable { println("Click!") }) {
            Text("Some title")
            Box(Modifier.size(50.dp).background(playStoreColor))
            Text("3.5 ★")
        }
    }
}
