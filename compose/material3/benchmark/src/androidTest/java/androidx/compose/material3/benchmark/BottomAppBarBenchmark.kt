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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

class BottomAppBarBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val bottomAppBarTestCaseFactory = { BottomAppBarTestCase() }

    @Ignore
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(bottomAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(bottomAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(bottomAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(bottomAppBarTestCaseFactory)
    }

    @Test
    fun bottomAppBar_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(bottomAppBarTestCaseFactory)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
internal class BottomAppBarTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        BottomAppBar(
            horizontalArrangement = BottomAppBarDefaults.HorizontalArrangement,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Localized description",
                )
            }
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Localized description",
                )
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}
