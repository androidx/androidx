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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class IconButtonBenchmark(private val type: IconButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = IconButtonType.values()
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val iconButtonTestCaseFactory = { IconButtonTestCase(type) }

    @Ignore
    @Test
    fun iconButton_first_compose() {
        benchmarkRule.benchmarkFirstCompose(iconButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconButton_measure() {
        benchmarkRule.benchmarkFirstMeasure(iconButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconButton_layout() {
        benchmarkRule.benchmarkFirstLayout(iconButtonTestCaseFactory)
    }

    @Ignore
    @Test
    fun iconButton_draw() {
        benchmarkRule.benchmarkFirstDraw(iconButtonTestCaseFactory)
    }

    @Test
    fun iconButton_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(iconButtonTestCaseFactory)
    }
}

internal class IconButtonTestCase(
    private val type: IconButtonType
) : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        when (type) {
            IconButtonType.IconButton ->
                IconButton(onClick = { /* Do something! */ }) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Localized description"
                    )
                }

            IconButtonType.FilledIconButton ->
                FilledIconButton(onClick = { /* Do something! */ }) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Localized description"
                    )
                }

            IconButtonType.FilledTonalIconButton ->
                FilledTonalIconButton(onClick = { /* Do something! */ }) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Localized description"
                    )
                }

            IconButtonType.OutlinedIconButton ->
                OutlinedIconButton(onClick = { /* Do something! */ }) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Localized description"
                    )
                }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

enum class IconButtonType {
    IconButton, FilledIconButton, FilledTonalIconButton, OutlinedIconButton,
}
