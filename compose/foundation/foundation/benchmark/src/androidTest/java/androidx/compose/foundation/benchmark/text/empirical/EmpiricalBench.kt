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

package androidx.compose.foundation.benchmark.text.empirical

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import org.junit.Rule
import org.junit.Test

abstract class EmpiricalBench<S> where S : ToggleableTestCase, S : LayeredComposeTestCase {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    abstract val caseFactory: () -> S

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

@Composable
fun Subject(text: String, style: TextStyle) {
    Text(text, style = style)
}

@Composable
fun Subject(text: String, modifier: Modifier, style: TextStyle) {
    Text(text, modifier, style = style)
}

@Composable
fun Subject(text: AnnotatedString, style: TextStyle) {
    Text(text, style = style)
}

@Composable
fun Subject(
    text: AnnotatedString,
    style: TextStyle,
    inlineContent: Map<String, InlineTextContent>
) {
    Text(text, style = style, inlineContent = inlineContent)
}
