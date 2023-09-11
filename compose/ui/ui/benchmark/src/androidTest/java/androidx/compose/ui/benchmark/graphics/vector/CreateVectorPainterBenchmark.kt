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

package androidx.compose.ui.benchmark.graphics.vector

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.ui.Modifier
import androidx.compose.ui.benchmark.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CreateVectorPainterBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun recreateContent() {
        benchmarkRule.toggleStateBenchmarkDraw({
            RecreateVectorPainterTestCase()
        }, assertOneRecomposition = false)
    }

    @Test
    fun renderVectorWithDifferentSizes() {
        benchmarkRule.toggleStateBenchmarkDraw({
            ResizeVectorPainter()
        }, assertOneRecomposition = false)
    }
}

private class RecreateVectorPainterTestCase : ComposeTestCase, ToggleableTestCase {

    private var alpha by mutableStateOf(1f)

    @Composable
    override fun Content() {
        Column {
            Box(modifier = Modifier.wrapContentSize()) {
                Image(
                    painter = painterResource(R.drawable.ic_hourglass),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp),
                    alpha = alpha
                )
            }
        }
    }

    override fun toggleState() {
        if (alpha == 1.0f) {
            alpha = 0.5f
        } else {
            alpha = 1.0f
        }
    }
}

private class ResizeVectorPainter : ComposeTestCase, ToggleableTestCase {

    private var alpha by mutableStateOf(1f)

    @Composable
    override fun Content() {
        Column {
            Box(modifier = Modifier.wrapContentSize()) {
                Image(
                    painter = painterResource(R.drawable.ic_hourglass),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    alpha = alpha
                )
            }

            Box(modifier = Modifier.wrapContentSize()) {
                Image(
                    painter = painterResource(R.drawable.ic_hourglass),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp),
                    alpha = alpha
                )
            }
        }
    }

    override fun toggleState() {
        if (alpha == 1.0f) {
            alpha = 0.5f
        } else {
            alpha = 1.0f
        }
    }
}
