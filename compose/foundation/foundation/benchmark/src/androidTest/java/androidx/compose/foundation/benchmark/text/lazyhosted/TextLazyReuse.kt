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

package androidx.compose.foundation.benchmark.text.lazyhosted

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class TextLazyReuse(
    private val changeText: Boolean
) : LayeredComposeTestCase(), ToggleableTestCase {
    private var flipper = false
    private var toggleText = mutableStateOf("")
    private var active = mutableStateOf(true)
    private var reuseKey = mutableStateOf(0)

    private val style = TextStyle.Default.copy(
        fontFamily = FontFamily.Monospace
    )

    @Composable
    override fun MeasuredContent() {
        ReusableContentHost(active.value) {
            ReusableContent(reuseKey.value) {
                Text(
                    toggleText.value,
                    style = style,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override fun toggleState() {
        flipper = !flipper
        if (flipper) {
            active.value = false
        } else {
            active.value = true
            reuseKey.value = reuseKey.value++
            if (changeText) {
                toggleText.value = "reuse ${reuseKey.value}"
            }
        }
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextLazyReuseSameText {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    val caseFactory = { TextLazyReuse(false) }

    @Test
    fun recomposeOnly() {
        benchmarkRule.toggleStateBenchmarkRecompose(caseFactory)
    }

    @Test
    fun recomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(caseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.toggleStateBenchmarkDraw(caseFactory)
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextLazyReuseChangedText {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    val caseFactory = { TextLazyReuse(true) }

    @Test
    fun recomposeOnly() {
        benchmarkRule.toggleStateBenchmarkRecompose(caseFactory)
    }

    @Test
    fun recomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(caseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.toggleStateBenchmarkDraw(caseFactory)
    }
}