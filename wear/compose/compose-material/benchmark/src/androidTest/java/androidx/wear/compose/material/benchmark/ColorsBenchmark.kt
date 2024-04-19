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

package androidx.wear.compose.material.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.contentColorFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ColorsBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val colorsTestCaseFactory = { ColorsTestCase() }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(colorsTestCaseFactory)
    }
}

private class ColorsTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        MaterialTheme {
            Column {
                // Primary
                Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colors.primary))
                Box(
                    modifier = Modifier.size(1.dp)
                        .background(
                            MaterialTheme.colors.contentColorFor(MaterialTheme.colors.primary)
                    )
                )

                // Secondary
                Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colors.secondary))
                Box(
                    modifier = Modifier.size(1.dp)
                        .background(
                            MaterialTheme.colors.contentColorFor(MaterialTheme.colors.secondary)
                        )
                )

                // Background
                Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colors.background))
                Box(
                    modifier = Modifier.size(1.dp)
                        .background(
                            MaterialTheme.colors.contentColorFor(MaterialTheme.colors.background)
                        )
                )

                // Surface
                Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colors.surface))
                Box(
                    modifier = Modifier.size(1.dp)
                        .background(
                            MaterialTheme.colors.contentColorFor(MaterialTheme.colors.surface)
                        )
                )

                // Error
                Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colors.error))
                Box(
                    modifier = Modifier.size(1.dp)
                        .background(
                            MaterialTheme.colors.contentColorFor(MaterialTheme.colors.error)
                        )
                )
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        content()
    }
}
