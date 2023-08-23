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

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CardBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val cardTestCaseFactory = { CardTestCase() }
    private val clickableCardTestCaseFactory = { ClickableCardTestCase() }

    @Ignore
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(cardTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(cardTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(cardTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(cardTestCaseFactory)
    }

    @Test
    fun card_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(cardTestCaseFactory)
    }

    @Test
    fun clickableCard_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(clickableCardTestCaseFactory)
    }
}

internal class CardTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        Card(modifier = Modifier.size(200.dp)) { }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

internal class ClickableCardTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        Card(onClick = {}, modifier = Modifier.size(200.dp)) { }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
