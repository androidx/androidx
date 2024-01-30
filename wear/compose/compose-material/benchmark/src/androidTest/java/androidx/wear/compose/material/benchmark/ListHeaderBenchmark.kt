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

package androidx.wear.compose.material.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ListHeaderBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val listHeaderCaseFactory = { ListHeaderTestCase() }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(listHeaderCaseFactory)
    }

    internal class ListHeaderTestCase : LayeredComposeTestCase() {
        @Composable
        override fun MeasuredContent() {
            ListHeader {
                Text("Test")
            }
        }

        @Composable
        override fun ContentWrappers(content: @Composable () -> Unit) {
            MaterialTheme {
                content()
            }
        }
    }
}
