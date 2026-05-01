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

package androidx.xr.compose.spatial

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.session
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SpatialElevationTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val parentTestTag = "parent"

    @Test
    fun spatialElevation_mainContent_isComposed() {
        composeTestRule.setContent {
            SpatialElevation {
                Box(modifier = Modifier.size(100.dp).testTag("MainContent")) {
                    Text("Main Content")
                }
            }
        }

        composeTestRule.onAllNodesWithTag("MainContent").onLast().assertIsDisplayed()
    }

    @Test
    fun spatialElevation_popup_doesNotThrowError() {
        composeTestRule.setContent { SpatialElevation { Popup { Text("Popup") } } }

        composeTestRule.onAllNodesWithText("Popup").onLast().assertIsDisplayed()
    }

    @Test
    fun spatialElevation_xrNotSupported_doesNotThrowError() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent { SpatialElevation { Popup { Text("Popup") } } }

        composeTestRule.onNodeWithText("Popup").assertExists()
    }

    @Test
    fun spatialElevation_homeSpaceMode_doesNotElevate() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) { SpatialElevation { Text("Main Content") } }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun spatialElevation_fullSpaceMode_doesElevate() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) { SpatialElevation { Text("Main Content") } }
        }

        // Since we no longer double-compose, there should be exactly ONE instance of the content.
        composeTestRule.onAllNodesWithText("Main Content").assertCountEquals(1)
        // Verify that the one existing node is the one being displayed
        composeTestRule.onNodeWithText("Main Content").assertIsDisplayed()
    }

    @Test
    fun spatialElevation_elevated_panelSizeMatchesContentSize() {
        composeTestRule.setContent {
            Box(Modifier.size(1000.dp))
            SpatialElevation { Box(Modifier.size(100.dp)) { Text("Main Content") } }
        }

        composeTestRule.onAllNodesWithText("Main Content").onLast().assertIsDisplayed()
        val entities = composeTestRule.session?.scene?.getEntitiesOfType(PanelEntity::class.java)
        checkNotNull(entities).single { !it.isMainPanelEntity && it.sizeInPixels.width == 100 }
    }

    @Test
    fun spatialElevation_elevatedPanel_noXYOffsetIfParentViewIsSameSize() {
        composeTestRule.setContent {
            Box(Modifier.size(100.dp))
            SpatialElevation(elevation = 10.dp) {
                Box(Modifier.size(100.dp)) { Text("Main Content") }
            }
        }

        composeTestRule.onAllNodesWithText("Main Content").onLast().assertIsDisplayed()
        val entities =
            checkNotNull(composeTestRule.session?.scene?.getEntitiesOfType(PanelEntity::class.java))
        val panel = checkNotNull(entities).single { it.sizeInPixels.width == 100 }
        assertThat(panel).isNotEqualTo(composeTestRule.session?.scene?.mainPanelEntity)
        assertThat(panel.getPose().translation.x).isEqualTo(0f)
        assertThat(panel.getPose().translation.y).isEqualTo(0f)
        assertThat(panel.getPose().translation.z).isNotEqualTo(0f)
    }

    @Test
    fun spatialElevation_elevatedPanel_contentIsOnlyDisplayedOnce() {
        composeTestRule.setContent {
            Box(Modifier.size(100.dp))
            SpatialElevation(elevation = 10.dp) {
                Box(Modifier.size(100.dp)) { Text("Main Content") }
            }
        }

        composeTestRule.onAllNodesWithText("Main Content").assertCountEquals(1)

        // That single node should be visible.
        composeTestRule.onNodeWithText("Main Content").assertIsDisplayed()
    }

    @Test
    fun spatialElevation_scrolledOffScreen_setsAlphaToZero() {
        val scrollState = ScrollState(0)

        composeTestRule.setContent {
            Column(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                SpatialElevation { Box(Modifier.size(100.dp).testTag("ElevatedContent")) }
                Box(Modifier.size(1000.dp))
            }
        }

        val panel =
            checkNotNull(composeTestRule.session?.scene?.getEntitiesOfType(PanelEntity::class.java))
                .single { !it.isMainPanelEntity }

        assertThat(panel.getAlpha()).isEqualTo(1f)

        runBlocking { scrollState.scrollTo(500) }
        composeTestRule.waitForIdle()

        assertThat(panel.getAlpha()).isEqualTo(0f)
    }

    @Test
    fun spatialElevation_preservesStateWhenToggled() {
        var isSpatialEnabled by mutableStateOf(false)
        val spatialCapabilities = mock<SpatialCapabilities>()
        whenever(spatialCapabilities.isSpatialUiEnabled).thenReturn(isSpatialEnabled)

        var count = 0

        composeTestRule.setContent {
            val currentSpatialEnabled = isSpatialEnabled
            whenever(spatialCapabilities.isSpatialUiEnabled).thenReturn(currentSpatialEnabled)

            CompositionLocalProvider(LocalSpatialCapabilities provides spatialCapabilities) {
                SpatialElevation {
                    var localCount by remember { mutableStateOf(0) }
                    SideEffect { count = localCount }

                    Box(modifier = Modifier.clickable { localCount++ }.testTag("btn")) {
                        Text("Count: $localCount")
                    }
                }
            }
        }

        // 1. Increment the counter
        composeTestRule.onNodeWithTag("btn").performClick()
        composeTestRule.waitForIdle()
        assertThat(count).isEqualTo(1)

        // 2. Toggle spatial UI
        isSpatialEnabled = true
        composeTestRule.waitForIdle()

        // 3. Verify that the state (count) was not lost!
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun spatialElevation_initialSetUpSettlesDeterministically_inThreePasses() {
        var outerLayoutPassCount = 0
        var innerLayoutPassCount = 0

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.testTag("outerBox").layout { measurable, constraints ->
                        outerLayoutPassCount++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(100.dp).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerLayoutPassCount++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(innerLayoutPassCount).isEqualTo(1)
        assertThat(outerLayoutPassCount).isEqualTo(2)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun spatialElevation_layoutPasses_onInitialAndResize() {
        var outerLayoutPassCount = 0
        var innerLayoutPassCount = 0

        var contentSize by mutableStateOf(100.dp)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.testTag("outerBox").layout { measurable, constraints ->
                        outerLayoutPassCount++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(contentSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerLayoutPassCount++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        outerLayoutPassCount = 0
        innerLayoutPassCount = 0

        contentSize = 200.dp

        composeTestRule.waitForIdle()

        assertThat(innerLayoutPassCount).isEqualTo(1)
        assertThat(outerLayoutPassCount).isEqualTo(1)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun spatialElevation_layoutPasses_stableAcrossMultipleResizes() {
        var outerLayoutPassCount = 0
        var innerLayoutPassCount = 0

        var contentSize by mutableStateOf(100.dp)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.testTag("outerBox").layout { measurable, constraints ->
                        outerLayoutPassCount++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(contentSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerLayoutPassCount++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        val testSizes = listOf(50.dp, 200.dp, 100.dp, 300.dp)

        testSizes.forEachIndexed { _, newSize ->
            outerLayoutPassCount = 0
            innerLayoutPassCount = 0

            contentSize = newSize
            composeTestRule.waitForIdle()

            assertThat(innerLayoutPassCount).isEqualTo(1)
            assertThat(outerLayoutPassCount).isEqualTo(1)

            composeTestRule
                .onNodeWithTag("innerBox")
                .assertWidthIsEqualTo(newSize)
                .assertHeightIsEqualTo(newSize)

            composeTestRule
                .onNodeWithTag("outerBox")
                .assertWidthIsEqualTo(newSize)
                .assertHeightIsEqualTo(newSize)
        }
    }

    // Applying the minimum size to the Box, not to the SpatialElevation.
    @Test
    fun spatialElevation_requests_MinimumConstraints_doesNotConform() {
        val minWidth = 100.dp
        val contentSize = 10.dp
        var actualSize: IntSize? = null
        var outerBoxSize: IntSize? = null

        val expectedWidthPx = with(composeTestRule.density) { minWidth.roundToPx() }
        val expectedContentSizePx = with(composeTestRule.density) { contentSize.roundToPx() }

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.sizeIn(minWidth = minWidth).onGloballyPositioned { coordinates ->
                        outerBoxSize = coordinates.size
                    }
            ) {
                SpatialElevation {
                    Box(
                        Modifier.size(contentSize).onGloballyPositioned { coordinates ->
                            actualSize = coordinates.size
                        }
                    ) {
                        Text("Small Content")
                    }
                }
            }
        }

        assertThat(actualSize?.width).isEqualTo(expectedContentSizePx)
        assertThat(outerBoxSize?.width).isEqualTo(expectedWidthPx)
    }

    @Test
    fun spatialElevation_respectsMinimumConstraints_propagateMinConstraints() {
        val minWidth = 100.dp
        val contentSize = 10.dp
        var actualSize: IntSize? = null
        var outerBoxSize: IntSize? = null

        val expectedWidthPx = with(composeTestRule.density) { minWidth.roundToPx() }

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.sizeIn(minWidth = minWidth).onGloballyPositioned { coordinates ->
                        outerBoxSize = coordinates.size
                    },
                propagateMinConstraints = true,
            ) {
                SpatialElevation {
                    Box(
                        Modifier.size(contentSize).onGloballyPositioned { coordinates ->
                            actualSize = coordinates.size
                        }
                    ) {
                        Text("Small Content")
                    }
                }
            }
        }

        assertThat(actualSize?.width).isEqualTo(expectedWidthPx)
        assertThat(outerBoxSize?.width).isEqualTo(expectedWidthPx)
    }

    @Test
    fun spatialElevation_respectsMinimumConstraint_customLayout() {
        val minWidth = 100.dp
        val contentSize = 10.dp
        var actualWidthPx: Int? = null
        val expectedWidthPx = with(composeTestRule.density) { minWidth.roundToPx() }

        composeTestRule.setContent {
            Layout(
                content = {
                    SpatialElevation { Box(Modifier.size(contentSize)) { Text("Small Content") } }
                }
            ) { measurables, incomingConstraints ->
                val testConstraints = incomingConstraints.copy(minWidth = expectedWidthPx)
                val placeable = measurables.single().measure(testConstraints)

                actualWidthPx = placeable.width

                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(actualWidthPx).isEqualTo(expectedWidthPx)
    }

    @Test
    fun innerContentResize_triggersSingleLayoutPass() {
        var innerSize by mutableStateOf(100.dp)
        var outerSize by mutableStateOf(300.dp)

        var innerPasses = 0
        var outerPasses = 0

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.size(outerSize).testTag("outerBox").layout { measurable, constraints ->
                        outerPasses++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(innerSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerPasses++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(300.dp)

        innerPasses = 0
        outerPasses = 0

        innerSize = 200.dp
        composeTestRule.waitForIdle()

        assertThat(innerPasses).isEqualTo(1)
        assertThat(outerPasses).isEqualTo(1)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(300.dp)
    }

    @Test
    fun outerContainerResize_triggersSingleLayoutPass() {
        var innerSize by mutableStateOf(50.dp)
        var outerSize by mutableStateOf(150.dp)

        var innerPasses = 0
        var outerPasses = 0

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.size(outerSize).testTag("outerBox").layout { measurable, constraints ->
                        outerPasses++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(innerSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerPasses++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)

        innerPasses = 0
        outerPasses = 0

        outerSize = 200.dp
        composeTestRule.waitForIdle()

        assertThat(outerPasses).isEqualTo(1)
        assertThat(innerPasses).isEqualTo(1)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun simultaneousResize_triggersSingleLayoutPass() {
        var innerSize by mutableStateOf(50.dp)
        var outerSize by mutableStateOf(150.dp)

        var innerPasses = 0
        var outerPasses = 0

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.size(outerSize).testTag("outerBox").layout { measurable, constraints ->
                        outerPasses++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(innerSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerPasses++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)

        innerPasses = 0
        outerPasses = 0

        outerSize = 250.dp
        innerSize = 125.dp
        composeTestRule.waitForIdle()

        assertThat(outerPasses).isEqualTo(1)
        assertThat(innerPasses).isEqualTo(1)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(125.dp)
            .assertHeightIsEqualTo(125.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(250.dp)
            .assertHeightIsEqualTo(250.dp)
    }

    @Test
    fun expandInnerContent_propagatesToOuter_triggersSingleLayoutPass() {
        var innerSize by mutableStateOf(100.dp)

        var innerPasses = 0
        var outerPasses = 0

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.testTag("outerBox").layout { measurable, constraints ->
                        outerPasses++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(innerSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerPasses++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        innerPasses = 0
        outerPasses = 0

        innerSize = 200.dp
        composeTestRule.waitForIdle()

        assertThat(innerPasses).isEqualTo(1)
        assertThat(outerPasses).isEqualTo(1)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun shrinkInnerContent_propagatesToOuter_triggersSingleLayoutPass() {
        var innerSize by mutableStateOf(100.dp)

        var innerPasses = 0
        var outerPasses = 0

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.testTag("outerBox").layout { measurable, constraints ->
                        outerPasses++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            ) {
                SpatialElevation {
                    Box(
                        modifier =
                            Modifier.size(innerSize).testTag("innerBox").layout {
                                measurable,
                                constraints ->
                                innerPasses++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        innerPasses = 0
        outerPasses = 0

        innerSize = 50.dp
        composeTestRule.waitForIdle()

        assertThat(innerPasses).isEqualTo(1)
        assertThat(outerPasses).isEqualTo(1)

        composeTestRule
            .onNodeWithTag("innerBox")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onNodeWithTag("outerBox")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun spatialElevation_allowsZeroSizedContent() {
        composeTestRule.setContent {
            SpatialElevation { Box(Modifier.size(0.dp).testTag("ZeroContent")) }
        }

        composeTestRule.onNodeWithTag("ZeroContent").assertExists()
        composeTestRule.onNodeWithTag("ZeroContent").assertWidthIsEqualTo(0.dp)
    }
}
