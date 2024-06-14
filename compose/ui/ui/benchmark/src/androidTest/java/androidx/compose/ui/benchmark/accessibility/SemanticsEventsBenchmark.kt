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

package androidx.compose.ui.benchmark.accessibility

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SemanticsEventsBenchmark {

    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(MicrobenchmarkConfig(traceAppTagEnabled = true))

    private val semanticsFactory = { SemanticsTestCase() }

    /** Send semantic events by changing AnnotatedString in content via toggling. */
    @Test
    fun sendSemanticsEvents() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(caseFactory = semanticsFactory)
    }

    class SemanticsTestCase : ComposeTestCase, ToggleableTestCase {

        private lateinit var state: MutableState<Boolean>

        @Composable
        override fun Content() {
            state = remember { mutableStateOf(false) }
            val composeView = LocalView.current

            // Use an AnnotatedString to trigger semantics changes and send accessibility events.
            repeat(10) {
                Box(
                    Modifier.size(10.dp).semantics(mergeDescendants = true) {
                        setText { true }
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (!state.value) "1234" else "1235")
                    }
                )
                BasicText(state.value.toString())
            }

            // TODO(b/308007375): Eventually we will be able to remove `accessibilityForTesting()`;
            // this is just a temporary workaround for now.
            LaunchedEffect(Unit) {
                // Make sure the delay between batches of a11y events is set to zero.
                (composeView as RootForTest).setAccessibilityEventBatchIntervalMillis(0L)
                // Ensure that accessibility is enabled for testing.
                (composeView as RootForTest).forceAccessibilityForTesting(true)
            }
        }

        override fun toggleState() {
            state.value = !state.value
        }
    }
}
