/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.lighting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LightAnchorModifierTest {

    @get:Rule val rule = createComposeRule()

    private val anchor = LightAnchorState()

    @Test
    fun publishesThePositionOfTheAnchoredComposable() {
        rule.setContent {
            Box(Modifier.offset { IntOffset(10, 20) }.size(5.dp).lightAnchor(anchor))
        }

        rule.runOnIdle { assertThat(anchor.rootPosition()).isEqualTo(Offset(10f, 20f)) }
    }

    @Test
    fun clearsWhenTheAnchoredComposableLeavesTheComposition() {
        var showAnchor by mutableStateOf(true)
        rule.setContent { if (showAnchor) Box(Modifier.size(5.dp).lightAnchor(anchor)) }
        rule.runOnIdle { assertThat(anchor.coordinates).isNotNull() }

        rule.runOnIdle { showAnchor = false }

        rule.runOnIdle { assertThat(anchor.coordinates).isNull() }
    }

    @Test
    fun clearsWhenTheAnchoredComposableIsNoLongerPlaced() {
        var placeAnchor by mutableStateOf(true)
        rule.setContent {
            PlacedIf({ placeAnchor }) { Box(Modifier.size(5.dp).lightAnchor(anchor)) }
        }
        rule.runOnIdle { assertThat(anchor.coordinates).isNotNull() }

        rule.runOnIdle { placeAnchor = false }

        rule.runOnIdle { assertThat(anchor.coordinates).isNull() }
    }

    @Test
    fun movesToTheNewStateWhenTheStateChanges() {
        val newAnchor = LightAnchorState()
        var useNewAnchor by mutableStateOf(false)
        rule.setContent {
            Box(Modifier.size(5.dp).lightAnchor(if (useNewAnchor) newAnchor else anchor))
        }
        rule.runOnIdle { assertThat(anchor.coordinates).isNotNull() }

        rule.runOnIdle { useNewAnchor = true }

        rule.runOnIdle {
            assertThat(anchor.coordinates).isNull()
            assertThat(newAnchor.coordinates).isNotNull()
        }
    }

    @Test
    fun removingOneOfTwoAnchoredComposablesKeepsTheOther() {
        var showFirst by mutableStateOf(true)
        rule.setContent {
            Box {
                // Fixed size, so removing its child doesn't re-position the second anchor.
                Box(Modifier.size(20.dp)) {
                    if (showFirst) Box(Modifier.size(5.dp).lightAnchor(anchor))
                }
                Box(Modifier.offset { IntOffset(60, 60) }.size(5.dp).lightAnchor(anchor))
            }
        }
        // Both anchors publish; the second one is positioned last, so it is the one reported.
        rule.runOnIdle { assertThat(anchor.rootPosition()).isEqualTo(Offset(60f, 60f)) }

        rule.runOnIdle { showFirst = false }

        rule.runOnIdle { assertThat(anchor.rootPosition()).isEqualTo(Offset(60f, 60f)) }
    }

    private fun LightAnchorState.rootPosition(): Offset? = coordinates?.positionInRoot()
}

/** Measures [content], but only places it while [placed] returns true. */
@Composable
private fun PlacedIf(placed: () -> Boolean, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        layout(placeable.width, placeable.height) { if (placed()) placeable.place(0, 0) }
    }
}
