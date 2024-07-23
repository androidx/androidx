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

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalFloatingAppBar
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class FloatingAppBarBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val horizontalFloatingAppBarTestCaseFactory = { HorizontalFloatingAppBarTestCase() }
    private val verticalFloatingAppBarTestCaseFactory = { VerticalFloatingAppBarTestCase() }

    @Ignore
    @Test
    fun horizontalFloatingAppBar_first_compose() {
        benchmarkRule.benchmarkFirstCompose(horizontalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalFloatingAppBar_first_compose() {
        benchmarkRule.benchmarkFirstCompose(verticalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun horizontalFloatingAppBar_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(horizontalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalFloatingAppBar_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(verticalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun horizontalFloatingAppBar_first_layout() {
        benchmarkRule.benchmarkFirstLayout(horizontalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalFloatingAppBar_first_layout() {
        benchmarkRule.benchmarkFirstLayout(verticalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun horizontalFloatingAppBar_first_draw() {
        benchmarkRule.benchmarkFirstDraw(horizontalFloatingAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalFloatingAppBar_first_draw() {
        benchmarkRule.benchmarkFirstDraw(verticalFloatingAppBarTestCaseFactory)
    }

    @Test
    fun horizontalFloatingAppBar_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(horizontalFloatingAppBarTestCaseFactory)
    }

    @Test
    fun verticalFloatingAppBar_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(verticalFloatingAppBarTestCaseFactory)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class HorizontalFloatingAppBarTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        HorizontalFloatingAppBar(
            expanded = true,
            modifier = Modifier.fillMaxWidth(),
            leadingContent = { leadingContent() },
            trailingContent = { trailingContent() },
            content = { mainContent() },
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class VerticalFloatingAppBarTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        VerticalFloatingAppBar(
            expanded = true,
            modifier = Modifier.fillMaxHeight(),
            leadingContent = { leadingContent() },
            trailingContent = { trailingContent() },
            content = { mainContent() },
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

@Composable
private fun leadingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Add, contentDescription = "Localized description")
    }
}

@Composable
private fun trailingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Check, contentDescription = "Localized description")
    }
}

@Composable
private fun mainContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
    }
}
