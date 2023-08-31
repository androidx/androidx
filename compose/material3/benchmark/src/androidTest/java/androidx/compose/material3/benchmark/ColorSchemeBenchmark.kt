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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ColorSchemeBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val colorSchemeTestCaseFactory = { ColorSchemeTestCase() }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(colorSchemeTestCaseFactory)
    }
}

class ColorSchemeTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        Column {
            Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colorScheme.surface))
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface))
            )
            Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primary))
            )
            Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colorScheme.secondary))
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondary))
            )
            Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colorScheme.tertiary))
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.tertiary))
            )
            Box(modifier = Modifier.size(1.dp).background(MaterialTheme.colorScheme.error))
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.error))
            )
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.surfaceContainerLowest)
            )
            Box(modifier = Modifier.size(1.dp).background(
                MaterialTheme.colorScheme.contentColorFor(
                    MaterialTheme.colorScheme.surfaceContainerLowest)
                )
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
