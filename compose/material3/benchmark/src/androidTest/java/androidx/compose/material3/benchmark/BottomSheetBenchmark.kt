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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BottomSheetBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val bottomSheetScaffoldTestCaseFactory = { BottomSheetScaffoldTestCase() }
    private val modalBottomSheetTestCaseFactory = { ModalBottomSheetTestCase() }

    @Ignore
    @Test
    fun bottomSheetScaffold_first_compose() {
        benchmarkRule.benchmarkFirstCompose(bottomSheetScaffoldTestCaseFactory)
    }

    @Ignore
    @Test
    fun modalBottomSheet_first_compose() {
        benchmarkRule.benchmarkFirstCompose(modalBottomSheetTestCaseFactory)
    }

    @Ignore
    @Test
    fun bottomSheetScaffold_measure() {
        benchmarkRule.benchmarkFirstMeasure(bottomSheetScaffoldTestCaseFactory)
    }

    @Ignore
    @Test
    fun modalBottomSheet_measure() {
        benchmarkRule.benchmarkFirstMeasure(modalBottomSheetTestCaseFactory)
    }

    @Ignore
    @Test
    fun bottomSheetScaffold_layout() {
        benchmarkRule.benchmarkFirstLayout(bottomSheetScaffoldTestCaseFactory)
    }

    @Ignore
    @Test
    fun modalBottomSheet_layout() {
        benchmarkRule.benchmarkFirstLayout(modalBottomSheetTestCaseFactory)
    }

    @Ignore
    @Test
    fun bottomSheetScaffold_draw() {
        benchmarkRule.benchmarkFirstDraw(bottomSheetScaffoldTestCaseFactory)
    }

    @Ignore
    @Test
    fun modalBottomSheet_draw() {
        benchmarkRule.benchmarkFirstDraw(modalBottomSheetTestCaseFactory)
    }

    @Test
    fun bottomSheetScaffold_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(bottomSheetScaffoldTestCaseFactory)
    }

    @Test
    fun modalBottomSheet_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(modalBottomSheetTestCaseFactory)
    }

    @Ignore
    @Test
    fun bottomSheetScaffoldVisibilityTest() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = bottomSheetScaffoldTestCaseFactory,
            assertOneRecomposition = false
        )
    }

    @Ignore
    @Test
    fun modalBottomSheetVisibilityTest() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(modalBottomSheetTestCaseFactory)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScaffoldTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: BottomSheetScaffoldState
    private lateinit var scope: CoroutineScope

    @Composable
    override fun MeasuredContent() {
        state =
            rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = false)
            )
        scope = rememberCoroutineScope()
        BottomSheetScaffold(sheetContent = {}, scaffoldState = state) {}
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        if (state.bottomSheetState.isVisible) {
            scope.launch { state.bottomSheetState.hide() }
        } else {
            scope.launch { state.bottomSheetState.show() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class ModalBottomSheetTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: SheetState
    private lateinit var scope: CoroutineScope

    @Composable
    override fun MeasuredContent() {
        state = rememberModalBottomSheetState()
        scope = rememberCoroutineScope()
        Column {
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
            ) {}
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        if (state.isVisible) {
            scope.launch { state.hide() }
        } else {
            scope.launch { state.show() }
        }
    }
}
