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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.toRemoteDp
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.text.DecimalFormat
import kotlin.OptIn
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class RcPlayerPrimitivesTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun testRemoteTextRenders() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument { RemoteText("Hello Remote") }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        // Verify that the text is rendered and visible to Compose testing API
        rule.onNodeWithText("Hello Remote").assertExists()
    }

    @Test
    fun testRemoteBoxRendersChildren() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument { RemoteBox { RemoteText("Inside Box") } }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("Inside Box").assertExists()
    }

    @Test
    fun testRemoteRowLaysOutHorizontally() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteRow {
                    RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("Text 1") }
                    RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("Text 2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("Text 1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("Text 2").fetchSemanticsNode()

        assert(node2.positionInRoot.x > node1.positionInRoot.x)
    }

    @Test
    fun testRemoteColumnLaysOutVertically() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("Text 1") }
                    RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("Text 2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("Text 1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("Text 2").fetchSemanticsNode()

        assert(node2.positionInRoot.y > node1.positionInRoot.y)
    }

    @Test
    fun testNestedLayouts() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteRow {
                        RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("1") }
                        RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("2") }
                    }
                    RemoteRow {
                        RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("3") }
                        RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("4") }
                    }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()
        val node3 = rule.onNodeWithText("3").fetchSemanticsNode()
        val node4 = rule.onNodeWithText("4").fetchSemanticsNode()

        assert(node2.positionInRoot.x > node1.positionInRoot.x)
        assert(node3.positionInRoot.y > node1.positionInRoot.y)
        assert(node4.positionInRoot.x > node3.positionInRoot.x)
        assert(node4.positionInRoot.y > node2.positionInRoot.y)
    }

    @Test
    fun testPaddingModifier() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteBox(
                    modifier = RemoteModifier.padding(10f.rf.createReference(forceRemote = true))
                ) {
                    RemoteText("Target")
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node = rule.onNodeWithText("Target").fetchSemanticsNode()

        // Verify that padding pushes the child inwards!
        assert(node.positionInRoot.x > 0)
        assert(node.positionInRoot.y > 0)
    }

    @Test
    fun testClickableModifier() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val state = rememberMutableRemoteInt(initialValue = 0)
            val document = rememberRemoteDocument {
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            action =
                                androidx.compose.remote.creation.compose.action.valueChange(
                                    state,
                                    1.ri,
                                )
                        )
                ) {
                    RemoteText(state.toRemoteString(DecimalFormat("#0")))
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        // Verify initial text is "0"!
        rule.onNodeWithText("0").assertExists()

        // Perform click!
        rule.onNodeWithText("0").performClick()

        // Advance clock to allow state update and recomposition!
        rule.mainClock.advanceTimeBy(100)

        // Verify updated text is "1"!
        rule.onNodeWithText("1").assertExists()
    }

    @Test
    fun testSizeModifier() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteRow {
                    RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("1") }
                    RemoteBox { RemoteText("2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        val density = rule.density.density
        val expectedDistancePx = 50 * density

        assert((node2.positionInRoot.x - node1.positionInRoot.x) >= expectedDistancePx - 5)
    }

    @Test
    fun testRemoteStringState() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            val textState = rememberMutableRemoteString(initialValue = "Initial")
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteText(textState)
                    RemoteBox(
                        modifier =
                            RemoteModifier.clickable(
                                action =
                                    androidx.compose.remote.creation.compose.action.valueChange(
                                        textState,
                                        "Updated".rs,
                                    )
                            )
                    ) {
                        RemoteText("Click Me")
                    }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        // Verify initial text is "Initial"!
        rule.onNodeWithText("Initial").assertExists()

        // Perform click!
        rule.onNodeWithText("Click Me").performClick()

        // Advance clock!
        rule.mainClock.advanceTimeBy(100)

        // Verify updated text is "Updated"!
        rule.onNodeWithText("Updated").assertExists()
    }

    @Test
    fun testRemoteIntState() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            val intState = rememberMutableRemoteInt(initialValue = 0)
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteText(intState.toRemoteString(DecimalFormat("#0")))
                    RemoteBox(
                        modifier =
                            RemoteModifier.clickable(
                                action =
                                    androidx.compose.remote.creation.compose.action.valueChange(
                                        intState,
                                        1.ri,
                                    )
                            )
                    ) {
                        RemoteText("Click Me")
                    }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        // Verify initial text is "0"!
        rule.onNodeWithText("0").assertExists()

        // Perform click!
        rule.onNodeWithText("Click Me").performClick()

        // Advance clock!
        rule.mainClock.advanceTimeBy(100)

        // Verify updated text is "1"!
        rule.onNodeWithText("1").assertExists()
    }

    @Test
    fun testMathOperations() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteRow {
                    RemoteBox(
                        modifier =
                            RemoteModifier.width(
                                100f.rf.createReference(forceRemote = true) / 2f.rf
                            )
                    ) {
                        RemoteText("1")
                    }
                    RemoteBox { RemoteText("2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        // Since we passed RemoteFloat, it is interpreted as pixels!
        // So expected distance is exactly 50 pixels!
        val expectedDistancePx = 50f

        assert((node2.positionInRoot.x - node1.positionInRoot.x) >= expectedDistancePx - 2)
    }

    @Test
    fun testWidthInModifier_min() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteRow {
                    RemoteBox(modifier = RemoteModifier.widthIn(min = 50.rdp, max = 100.rdp)) {
                        RemoteText("1", modifier = RemoteModifier.size(10.rdp))
                    }
                    RemoteBox { RemoteText("2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        val density = rule.density.density
        val expectedDistancePx = 50 * density

        assert((node2.positionInRoot.x - node1.positionInRoot.x) >= expectedDistancePx - 2)
    }

    @Test
    fun testWidthInModifier_max() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteRow {
                    RemoteBox(modifier = RemoteModifier.widthIn(min = 50.rdp, max = 100.rdp)) {
                        RemoteText("1", modifier = RemoteModifier.size(150.rdp))
                    }
                    RemoteBox { RemoteText("2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(300.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        val density = rule.density.density
        val expectedDistancePx = 100 * density

        assert((node2.positionInRoot.x - node1.positionInRoot.x) >= expectedDistancePx - 2)
    }

    @Test
    fun testHeightInModifier_min() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteBox(modifier = RemoteModifier.heightIn(min = 50.rdp, max = 100.rdp)) {
                        RemoteText("1", modifier = RemoteModifier.size(10.rdp))
                    }
                    RemoteBox { RemoteText("2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        val density = rule.density.density
        val expectedDistancePx = 50 * density

        assert((node2.positionInRoot.y - node1.positionInRoot.y) >= expectedDistancePx - 2)
    }

    @Test
    fun testHeightInModifier_max() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteBox(modifier = RemoteModifier.heightIn(min = 50.rdp, max = 100.rdp)) {
                        RemoteText("1", modifier = RemoteModifier.size(150.rdp))
                    }
                    RemoteBox { RemoteText("2") }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(300.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        val density = rule.density.density
        val expectedDistancePx = 100 * density

        assert((node2.positionInRoot.y - node1.positionInRoot.y) >= expectedDistancePx - 2)
    }

    @Test
    @Ignore("Fails with first frame issue: componentWidth() returns 0 initially")
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testComponentValueReactivity() {
        // TODO: Investigate why componentWidth() returns 0 initially and fails to update.
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            rule.mainClock.autoAdvance = false
            rule.setContent {
                val parentSize = rememberMutableRemoteFloat(initialValue = 100f)
                val document = rememberRemoteDocument {
                    RemoteBox(modifier = RemoteModifier.width(parentSize)) {
                        val width = rememberMutableRemoteFloat { componentWidth() }

                        RemoteColumn {
                            RemoteText(width.toRemoteString(DecimalFormat("#0")))
                            RemoteBox(
                                modifier =
                                    RemoteModifier.clickable(
                                        action = valueChange(parentSize, 200f.rf)
                                    )
                            ) {
                                RemoteText("Change")
                            }
                        }
                    }
                }

                Box(modifier = androidx.compose.ui.Modifier.size(300.dp)) {
                    document.value?.let { RcPlayer(document = it) }
                }
            }

            // Add frame delay before checks!
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // Verify initial text is "100"!
            rule.onNodeWithText("100").assertExists()

            // Perform click to change parent size!
            rule.onNodeWithText("Change").performClick()

            // Advance clock to allow update!
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()

            // Verify updated text is "200"!
            rule.onNodeWithText("200").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @Ignore("Causes infinite recomposition loop")
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testBorderModifier() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            rule.setContent {
                val document = rememberRemoteDocument {
                    val borderWidth = androidx.compose.runtime.remember { 1.rdp }
                    val borderColor =
                        androidx.compose.runtime.remember {
                            androidx.compose.ui.graphics.Color.Red.rc
                        }
                    RemoteBox(modifier = RemoteModifier.border(borderWidth, borderColor)) {
                        RemoteText("Target")
                    }
                }

                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    document.value?.let { RcPlayer(document = it) }
                }
            }

            rule.onNodeWithText("Target").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testVisibleModifier() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            val visibleState = rememberMutableRemoteInt(initialValue = 1)
            val document = rememberRemoteDocument {
                RemoteColumn {
                    RemoteBox(modifier = RemoteModifier.visibility(visibleState)) {
                        RemoteText("Target")
                    }
                    RemoteBox(
                        modifier =
                            RemoteModifier.clickable(action = valueChange(visibleState, 0.ri))
                    ) {
                        RemoteText("Hide")
                    }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        // Verify initial state: Target exists!
        rule.onNodeWithText("Target").assertExists()

        // Perform click to hide!
        rule.onNodeWithText("Hide").performClick()

        // Advance clock!
        rule.mainClock.advanceTimeBy(100)

        // Verify that Target is not displayed!
        rule.onNodeWithText("Target").assertIsNotDisplayed()
    }

    @Test
    @Ignore("Fails with unexpected offset values: shift is smaller than expected")
    fun testOffsetModifier() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteRow {
                    RemoteBox(modifier = RemoteModifier.size(50.rdp)) { RemoteText("1") }
                    RemoteBox(
                        modifier =
                            RemoteModifier.offset(x = 20f.rf.toRemoteDp(), y = 30f.rf.toRemoteDp())
                                .size(50.rdp)
                    ) {
                        RemoteText("2")
                    }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        val node1 = rule.onNodeWithText("1").fetchSemanticsNode()
        val node2 = rule.onNodeWithText("2").fetchSemanticsNode()

        val density = rule.density.density

        // node1 is at 0, has width 50.dp. So node2 starts at 50.dp.
        // Offset adds 20 pixels to X and 30 pixels to Y!
        val expectedXDiffPx = 50 * density + 20
        val expectedYDiffPx = 30f

        assert((node2.positionInRoot.x - node1.positionInRoot.x) >= expectedXDiffPx - 2)
        assert((node2.positionInRoot.y - node1.positionInRoot.y) >= expectedYDiffPx - 2)
    }

    @Test
    @Ignore("Causes infinite recomposition loop")
    fun testBackgroundModifier() {
        rule.setContent {
            val document = rememberRemoteDocument {
                val color =
                    androidx.compose.runtime.remember { androidx.compose.ui.graphics.Color.Blue.rc }
                val boxModifier =
                    androidx.compose.runtime.remember(color) {
                        RemoteModifier.background(color).size(100.rdp)
                    }
                RemoteBox(modifier = boxModifier) { RemoteText("Target") }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.onNodeWithText("Target").assertExists()
    }

    @Test
    @Ignore("Causes infinite recomposition loop due to writes during composition")
    fun testDrawCircle() {
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteBox {
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                        drawCircle(paint = null, radius = 50f.rf)
                    }
                    RemoteText("Target")
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.onNodeWithText("Target").assertExists()
    }

    @Test
    @Ignore("Causes infinite recomposition loop due to writes during composition")
    fun testDrawRect() {
        rule.setContent {
            val document = rememberRemoteDocument {
                RemoteBox {
                    RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) { drawRect(paint = null) }
                    RemoteText("Target")
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.onNodeWithText("Target").assertExists()
    }
}
