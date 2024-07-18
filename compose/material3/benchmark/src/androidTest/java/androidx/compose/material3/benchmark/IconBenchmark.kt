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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class IconBenchmark(private val type: IconType) {
    companion object {
        @Parameterized.Parameters(name = "{0}") @JvmStatic fun parameters() = IconType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val iconTestCaseFactory = { IconTestCase(type) }

    @Test
    fun IconFirstPixel() {
        benchmarkRule.benchmarkToFirstPixel(iconTestCaseFactory)
    }
}

private class IconTestCase(private val type: IconType) : LayeredComposeTestCase() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        when (type) {
            IconType.FilledIcon -> Icon(Icons.Filled.Lock, contentDescription = null)
            IconType.FilledTintedIcon ->
                Icon(
                    rememberVectorPainter(image = Icons.Filled.Lock),
                    contentDescription = null,
                    tint = { Color.Blue }
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class IconType {
    FilledIcon,
    FilledTintedIcon
}
