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

package androidx.wear.compose.material3.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.filters.LargeTest
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ListHeaderBenchmark(private val type: ListHeaderType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = ListHeaderType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val listHeaderCaseFactory = { ListHeaderTestCase(type) }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(listHeaderCaseFactory)
    }
}

internal class ListHeaderTestCase(private val type: ListHeaderType) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        when (type) {
            ListHeaderType.ListHeader -> ListHeader { Text("Header") }
            ListHeaderType.ListSubheader -> ListSubHeader { Text("SubHeader") }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class ListHeaderType {
    ListHeader,
    ListSubheader
}
