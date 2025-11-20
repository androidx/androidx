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

package androidx.compose.material3.benchmark

import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FloatingToolbarBenchmark(private val type: FloatingToolbarType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = FloatingToolbarType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val mFloatingToolbarTestCaseFactory = { FloatingToolbarTestCase(type) }
    private val floatingToolbarWithFabTestCaseFactory = { FloatingToolbarWithFabTestCase(type) }

    @Test
    fun floatingToolbar_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(mFloatingToolbarTestCaseFactory)
    }

    @Test
    fun floatingToolbarWithFab_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(floatingToolbarWithFabTestCaseFactory)
    }

    @Ignore
    @Test
    fun floatingToolbar_first_compose() {
        benchmarkRule.benchmarkFirstCompose(mFloatingToolbarTestCaseFactory)
    }

    @Ignore
    @Test
    fun floatingToolbar_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(mFloatingToolbarTestCaseFactory)
    }

    @Ignore
    @Test
    fun floatingToolbar_first_layout() {
        benchmarkRule.benchmarkFirstLayout(mFloatingToolbarTestCaseFactory)
    }

    @Ignore
    @Test
    fun floatingToolbar_first_draw() {
        benchmarkRule.benchmarkFirstDraw(mFloatingToolbarTestCaseFactory)
    }

    @Test
    fun floatingToolbarWithFab_toggleState_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            caseFactory = floatingToolbarWithFabTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Ignore
    @Test
    fun floatingToolbarWithFab_toggleState_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            caseFactory = floatingToolbarWithFabTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Ignore
    @Test
    fun floatingToolbarWithFab_toggleState_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            caseFactory = floatingToolbarWithFabTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Ignore
    @Test
    fun floatingToolbarWithFab_toggleState_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            caseFactory = floatingToolbarWithFabTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class FloatingToolbarTestCase(private val type: FloatingToolbarType) :
    LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        when (type) {
            FloatingToolbarType.Horizontal ->
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = { LeadingContent() },
                    trailingContent = { TrailingContent() },
                    content = { MainContent() },
                )
            FloatingToolbarType.Vertical ->
                VerticalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier.fillMaxHeight(),
                    leadingContent = { LeadingContent() },
                    trailingContent = { TrailingContent() },
                    content = { MainContent() },
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class FloatingToolbarWithFabTestCase(private val type: FloatingToolbarType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var expanded: MutableState<Boolean>

    @Composable
    override fun MeasuredContent() {
        expanded = remember { mutableStateOf(false) }
        when (type) {
            FloatingToolbarType.Horizontal ->
                HorizontalFloatingToolbar(
                    expanded = expanded.value,
                    floatingActionButton = { ToolbarFab() },
                    // Snap the expand and collapse animations.
                    animationSpec = snap(),
                ) {
                    ToolbarContent()
                }
            FloatingToolbarType.Vertical ->
                VerticalFloatingToolbar(
                    expanded = expanded.value,
                    floatingActionButton = { ToolbarFab() },
                    // Snap the expand and collapse animations.
                    animationSpec = snap(),
                ) {
                    ToolbarContent()
                }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        expanded.value = !expanded.value
    }
}

enum class FloatingToolbarType {
    Horizontal,
    Vertical,
}

@Composable
private fun LeadingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Add, contentDescription = "Localized description")
    }
}

@Composable
private fun TrailingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Check, contentDescription = "Localized description")
    }
}

@Composable
private fun MainContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolbarFab() {
    FloatingToolbarDefaults.StandardFloatingActionButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Check, "Localized description")
    }
}

@Composable
private fun ToolbarContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Person, contentDescription = "Localized description")
    }
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
    }
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
    }
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
    }
}
