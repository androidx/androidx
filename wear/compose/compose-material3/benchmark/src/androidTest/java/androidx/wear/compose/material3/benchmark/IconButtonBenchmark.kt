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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedIconButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class IconButtonBenchmark(private val iconButtonType: IconButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = IconButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { IconButtonTestCase(iconButtonType) }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(testCaseFactory)
    }
}

internal class IconButtonTestCase(private val iconButtonType: IconButtonType) :
    LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        when (iconButtonType) {
            IconButtonType.FilledIconButton -> FilledIconButton(onClick = {}) { StandardIcon() }
            IconButtonType.FilledTonalIconButton ->
                FilledTonalIconButton(onClick = {}) { StandardIcon() }
            IconButtonType.OutlinedIconButton -> OutlinedIconButton(onClick = {}) { StandardIcon() }
            IconButtonType.IconButton -> IconButton(onClick = {}) { StandardIcon() }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class IconButtonType {
    FilledIconButton,
    FilledTonalIconButton,
    OutlinedIconButton,
    IconButton
}

@Composable
internal fun StandardIcon() {
    val size = 24f
    val imageVector =
        ImageVector.Builder(
                defaultWidth = size.dp,
                defaultHeight = size.dp,
                viewportWidth = size,
                viewportHeight = size
            )
            .build()

    Icon(imageVector = imageVector, contentDescription = "vector")
}
