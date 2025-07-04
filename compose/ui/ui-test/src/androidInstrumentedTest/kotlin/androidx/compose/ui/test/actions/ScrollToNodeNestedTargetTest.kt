/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollToNodeNestedTargetTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun scrollToNode_largeSemanticsContainer_scrollsToBottom() {
        rule.setContent {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("Container")
            ) {
                repeat(10) { Box(Modifier.size(100.dp)) }
                with(LocalDensity.current) {
                    // We create a container that's larger that the viewport so that the target is
                    // not on screen when the container is already visible.
                    val containerHeightPx = LocalWindowInfo.current.containerSize.height * 3
                    // It's important that the container has a semantics modifier to verify we still
                    // find the correct target node and scroll to it.
                    Box(Modifier.height(containerHeightPx.toDp()).semantics {}) {
                        BasicText("Target", modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }

        rule.onNodeWithTag("Container").performScrollToNode(hasText("Target"))
        rule.waitForIdle()

        rule.onNodeWithText("Target").assertIsDisplayed()
    }

    @Test
    fun scrollToNode_targetWithAnyDescendantMatcher_scrollsToTarget() {
        rule.setContent {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("Container")
            ) {
                repeat(10) { Box(Modifier.size(100.dp)) }
                Box(Modifier.semantics {}) {
                    BasicText("Target", modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }

        rule.onNodeWithTag("Container").performScrollToNode(hasAnyDescendant(hasText("Target")))
        rule.waitForIdle()

        rule.onNodeWithText("Target").assertIsDisplayed()
    }
}
