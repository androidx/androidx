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

package androidx.compose.ui.focus

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CustomFocusRectTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun focusRect_boundingBoxByDefault() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(focusTargetModifierNode, Modifier.size(10.toDp()))
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 10f, 10f))
        }
    }

    @Test
    fun focusRect_decidedByFocusProperties() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier = Modifier.focusProperties { focusRect = Rect(0f, 0f, 100f, 100f) },
            )
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 100f, 100f))
        }
    }

    @Test
    fun focusRect_allAscendantFocusProperties() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier =
                    Modifier.focusProperties { focusRect = focusRect.inflate(10f) }
                        .focusProperties { focusRect = focusRect.inflate(10f) }
                        .focusProperties { focusRect = Rect(0f, 0f, 10f, 10f) },
            )
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect())
                .isEqualTo(Rect(-20f, -20f, 30f, 30f))
        }
    }

    @Test
    fun focusRect_allAscendantFocusProperties_untilAnotherFocusTarget() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier =
                    Modifier.focusProperties { focusRect = focusRect.inflate(10f) }
                        .focusGroup()
                        .focusProperties { focusRect = focusRect.inflate(10f) }
                        .focusProperties { focusRect = Rect(0f, 0f, 10f, 10f) },
            )
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect())
                .isEqualTo(Rect(-10f, -10f, 20f, 20f))
        }
    }

    @Test
    fun focusRect_focusPropertiesAttachedLater() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode
        var includeFocusRect by mutableStateOf(false)

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier =
                    Modifier.size(30.toDp())
                        .then(
                            if (includeFocusRect) {
                                Modifier.focusProperties { focusRect = Rect(0f, 0f, 10f, 10f) }
                            } else {
                                Modifier
                            }
                        ),
            )
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 30f, 30f))
        }

        includeFocusRect = true

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 10f, 10f))
        }
    }

    @Test
    fun focusRect_focusPropertiesDetachedLater() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode
        var includeFocusRect by mutableStateOf(true)

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier =
                    Modifier.size(30.toDp())
                        .then(
                            if (includeFocusRect) {
                                Modifier.focusProperties { focusRect = Rect(0f, 0f, 10f, 10f) }
                            } else {
                                Modifier
                            }
                        ),
            )
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 10f, 10f))
        }

        includeFocusRect = false

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 30f, 30f))
        }
    }

    @Test
    fun focusRect_focusProperties_belowFocusTargetNode() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier = Modifier.focusProperties { focusRect = Rect(0f, 0f, 10f, 10f) },
            ) {
                Box(Modifier.focusProperties { focusRect = Rect(10f, 10f, 20f, 20f) })
            }
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 10f, 10f))
        }
    }

    @Test
    fun focusRect_canChange_accordingToFocusState() {
        val focusTargetModifierNode = FocusTargetModifierNode() as FocusTargetNode

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                focusTargetModifierNode,
                modifier =
                    Modifier.focusProperties {
                        if (focusTargetModifierNode.focusState.isFocused) {
                            focusRect = Rect(25f, 25f, 50f, 50f)
                        } else {
                            focusRect = Rect(0f, 0f, 100f, 100f)
                        }
                    },
            )
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(0f, 0f, 100f, 100f))
        }

        rule.runOnUiThread { focusTargetModifierNode.requestFocus() }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.fetchFocusRect()).isEqualTo(Rect(25f, 25f, 50f, 50f))
        }
    }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    @Composable
    private fun FocusTargetModifierNodeBox(
        focusTargetModifierNode: FocusTargetModifierNode,
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit = {},
    ) {
        val node = remember {
            object : DelegatingNode() {
                init {
                    delegate(focusTargetModifierNode)
                }
            }
        }
        Box(modifier.elementFor(node), content = content)
    }
}
