/*
 * Copyright 2019 The Android Open Source Project
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

import android.view.View
import android.view.accessibility.AccessibilityNodeProvider
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AccessibilityBenchmark {

    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = BenchmarkRule(MicrobenchmarkConfig(traceAppTagEnabled = true))

    private lateinit var composeView: View
    private lateinit var provider: AccessibilityNodeProvider
    private lateinit var semanticsList: List<SemanticsNode>

    private val tag = "TestTag"

    /**
     * Measure the creation of AccessibilityNodeInfos, as well as the accessibility traversal
     * ordering due to the custom traversal indices set in the sample content.
     */
    @Test
    fun fetchAccessibilityNodeInfo() {
        composeTestRule.setContent {
            composeView = LocalView.current
            provider = composeView.accessibilityNodeProvider
            AccessibilityTestCase().Content()
        }

        // TODO(b/272068594): Add api to fetch the semantics id from SemanticsNodeInteraction
        //  Collection directly. If we were only fetching one node with `onNodeWithTag`, we could
        // have a `SemanticsNodeInteraction.semanticsId`, but we want to create ANIs for multiple
        // nodes.
        semanticsList =
            composeTestRule
                .onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)

        benchmarkRule.measureRepeated {
            semanticsList.forEach { provider.createAccessibilityNodeInfo(it.id) }
        }
    }

    class AccessibilityTestCase : ComposeTestCase {

        private val tag = "TestTag"

        @Composable
        override fun Content() {
            val composeView = LocalView.current

            // Set traversal groups and indices to force traversal sorting and ordering
            // to take place.
            repeat(25) {
                Column(Modifier.semantics { isTraversalGroup = true }) {
                    Row(Modifier.semantics { isTraversalGroup = true }) {
                        Button(
                            onClick = {},
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "ContentDescription 1"
                                    traversalIndex = 2f
                                    testTag = tag
                                }
                        ) {
                            BasicText("BasicText 1")
                        }
                        Button(
                            onClick = {},
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "ContentDescription 2"
                                    traversalIndex = 1f
                                    testTag = tag
                                }
                        ) {
                            BasicText("BasicText 2")
                        }
                        Button(
                            onClick = {},
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "ContentDescription 3"
                                    traversalIndex = 3f
                                    testTag = tag
                                }
                        ) {
                            BasicText("BasicText 3")
                        }
                    }
                }
            }

            // TODO(b/308007375): Eventually we will be able to remove `accessibilityForTesting()`;
            // this is just a temporary workaround for now.
            LaunchedEffect(Unit) {
                // Ensure that accessibility is enabled for testing.
                (composeView as RootForTest).forceAccessibilityForTesting(true)
            }
        }
    }
}
