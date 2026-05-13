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

package androidx.xr.compose.spatial

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.compose.unit.toMeter
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.runtime.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION") // TODO(b/462428503) Remove when deprecated Orbiter is removed.
class OrbiterTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val parentTestTag = "parent"

    @Test
    fun orbiter_inFullSpaceMode_isElevated() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithText("Main Content").assertExists()
        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_inNonXrMode_isInline() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun orbiter_inHomeSpaceMode_isInline() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun orbiter_inHomeSpaceMode_whenShouldRenderInNonSpatialFalse_doesNotRenderContent() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box {
                Orbiter(ContentEdge.Top, shouldRenderInNonSpatial = false) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithText("Main Content").assertDoesNotExist()
    }

    @Test
    fun orbiter_multipleInstances_inFullSpaceMode_areElevated() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(position = ContentEdge.Top) { Text("Top") }
                Orbiter(position = ContentEdge.Start) { Text("Start") }
                Orbiter(position = ContentEdge.End) { Text("End") }
                Orbiter(position = ContentEdge.Bottom) { Text("Bottom") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_afterSwitchToFullSpace_isSpatialized() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(position = ContentEdge.Bottom) { Text("Bottom") }
            }
            checkNotNull(LocalSession.current).scene.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_inFullSpaceMode_whenShouldRenderInNonSpatialFalse_isElevated() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top, shouldRenderInNonSpatial = false) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_inNonXrMode_whenShouldRenderInNonSpatialBecomesTrue_isInline() {
        composeTestRule.activity.disableXr()

        var shouldRenderInNonSpatial by mutableStateOf(false)
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top, shouldRenderInNonSpatial = shouldRenderInNonSpatial) {
                    Text("Main Content")
                }
            }
        }

        shouldRenderInNonSpatial = true

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun orbiter_inHomeSpaceMode_rendersContent() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box {
                Text("Main Content")
                Orbiter(ContentEdge.Start) { Text("Orbiter Content") }
            }
        }

        composeTestRule.onNodeWithText("Main Content").assertExists()
        composeTestRule.onNodeWithText("Orbiter Content").assertExists()
    }

    @Test
    fun orbiter_whenRemovedFromComposition_removesContent() {
        var showOrbiter by mutableStateOf(true)
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                Text("Main Content")
                if (showOrbiter) {
                    Orbiter(position = ContentEdge.Top) { Text("Top Orbiter Content") }
                }
            }
        }

        composeTestRule.onNodeWithText("Top Orbiter Content").assertExists()
        showOrbiter = false
        composeTestRule.onNodeWithText("Top Orbiter Content").assertDoesNotExist()
    }

    @Test
    fun orbiter_whenSwitchingModes_updatesHierarchy() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Box(modifier = Modifier.size(100.dp)) { Text("Main Content") }
                Orbiter(
                    position = ContentEdge.Top,
                    offset = 0.dp,
                    offsetType = OrbiterOffsetType.InnerEdge,
                ) {
                    Text("Top Orbiter Content")
                }
                Orbiter(position = ContentEdge.Start) { Text("Start Orbiter Content") }
                Orbiter(position = ContentEdge.Bottom) { Text("Bottom Orbiter Content") }
                Orbiter(position = ContentEdge.End) { Text("End Orbiter Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
        composeTestRule.runOnIdle {
            checkNotNull(composeTestRule.session).scene.requestHomeSpaceMode()
        }

        // All orbiters become children of the Parent node
        composeTestRule.onNodeWithTag(parentTestTag).onChildren().assertCountEquals(5)
        composeTestRule.runOnIdle {
            checkNotNull(composeTestRule.session).scene.requestFullSpaceMode()
        }

        // Orbiters exist outside the compose hierarchy
        composeTestRule.onNodeWithTag(parentTestTag).onChildren().assertCountEquals(1)
    }

    @Test
    fun orbiter_withoutSubspace_usesMainWindowSize() {
        composeTestRule.setContent {
            Orbiter(ContentEdge.Top) {
                // The content of the Orbiter. We'll use its size, which is constrained
                // by the parent's panel size, to verify the change.
                Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {
                    Text("Some Orbiter content")
                }
            }
        }

        val session = composeTestRule.session
        assertNotNull(session)

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(
                with(composeTestRule.density) {
                    (session.context as Activity).window.decorView.width.toDp()
                }
            )
            .assertHeightIsEqualTo(
                with(composeTestRule.density) {
                    (session.context as Activity).window.decorView.height.toDp()
                }
            )
    }

    @Test
    fun orbiter_inSpatialPanel_usesSpatialPanelSize() {
        val testMainPanelEntity = mock<RtPanelEntity>()
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : SceneRuntime by runtime {
                    override var mainPanelEntity: RtPanelEntity = testMainPanelEntity
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.width(200.dp).height(200.dp).testTag("panel")) {
                    Orbiter(ContentEdge.Top) {
                        // The content of the Orbiter. We'll use its size, which is constrained
                        // by the parent's panel size, to verify the change.
                        Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {
                            Text("Some Orbiter content")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
        // Check `getMainWindowSize` is never called.
        verify(testMainPanelEntity, never()).sizeInPixels
    }

    @Test
    fun orbiter_inSpatialPanel_whenParentResizes_resizes() {
        var panelWidthDp by mutableStateOf(200.dp)
        var panelHeightDp by mutableStateOf(200.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(panelWidthDp)
                            .height(panelHeightDp)
                            .testTag("spatialPanelParent")
                ) {
                    Orbiter(ContentEdge.Start) {
                        // The content of the Orbiter. We'll use its size, which is constrained
                        // by the parent's panel size, to verify the change.
                        Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox"))
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)

        panelWidthDp = 300.dp
        panelHeightDp = 300.dp

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)
    }

    @Test
    fun orbiter_inMainPanel_usesMainPanelSize() {
        val testMainPanelEntity = mock<RtPanelEntity>()

        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : SceneRuntime by runtime {
                    override val mainPanelEntity = testMainPanelEntity
                }
            }
        )

        composeTestRule.setContent {
            Subspace {
                SpatialMainPanel(SubspaceModifier.width(200.dp).height(200.dp).testTag("panel"))
                Orbiter(ContentEdge.Top) {
                    // The content of the Orbiter. We'll use its size, which is constrained
                    // by the parent's panel size, to verify the change.
                    Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {}
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
        // Check `getMainWindowSize` is never called.
        verify(testMainPanelEntity, never()).sizeInPixels
    }

    @Test
    fun orbiter_inMainPanel_whenParentResizes_resizes() {
        var panelWidthDp by mutableStateOf(200.dp)
        var panelHeightDp by mutableStateOf(200.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialMainPanel(
                    modifier =
                        SubspaceModifier.width(panelWidthDp)
                            .height(panelHeightDp)
                            .testTag("mainPanelParent")
                )
                Orbiter(ContentEdge.Top) {
                    // The content of the Orbiter. We'll use its size, which is constrained
                    // by the parent's panel size, to verify the change.
                    Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox"))
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)

        panelWidthDp = 300.dp
        panelHeightDp = 300.dp

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)
    }

    @Test
    fun orbiter_whenContentLargerThanSpatialPanel_isConstrained() {
        composeTestRule.setContent {
            Subspace {
                // Parent panel with a fixed size
                SpatialPanel(SubspaceModifier.size(200.dp)) {
                    Orbiter(ContentEdge.Top) {
                        // Orbiter content that is larger than the parent panel
                        Box(modifier = Modifier.size(300.dp).testTag("orbiterContentBox"))
                    }
                }
            }
        }

        // The orbiter's content should be constrained by the parent's size (200.dp)
        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun orbiter_whenContentLargerThanMainPanel_isConstrained() {
        composeTestRule.setContent {
            Subspace {
                // Main panel with a fixed size
                SpatialMainPanel(SubspaceModifier.size(200.dp))
                Orbiter(ContentEdge.Top) {
                    // Orbiter content that is larger than the main panel
                    Box(modifier = Modifier.size(300.dp).testTag("orbiterContentBox"))
                }
            }
        }

        // The orbiter's content should be constrained by the main panel's size (200.dp)
        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun orbiter_whenContentLargerThanMainWindow_isConstrained() {
        var windowWidthDp by mutableStateOf(0.dp)
        var windowHeightDp by mutableStateOf(0.dp)

        composeTestRule.setContent {
            val window = composeTestRule.activity.window
            windowWidthDp = with(composeTestRule.density) { window.decorView.width.toDp() }
            windowHeightDp = with(composeTestRule.density) { window.decorView.height.toDp() }

            Orbiter(ContentEdge.Top) {
                // Orbiter content that is larger than the main window
                Box(
                    modifier =
                        Modifier.size(windowWidthDp + 100.dp, windowHeightDp + 100.dp)
                            .testTag("orbiterContentBox")
                )
            }
        }

        // The orbiter's content should be constrained by the main window's size
        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(windowWidthDp)
            .assertHeightIsEqualTo(windowHeightDp)
    }

    @Test
    fun orbiter_inSubspace_withoutMainPanel_isSizeZero() {
        composeTestRule.setContent {
            Subspace {
                Orbiter(ContentEdge.Top) {
                    // The content of the Orbiter. We'll use its size, which is constrained
                    // by the parent's panel size, to verify the change.
                    Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {}
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun orbiter_withoutSpatialCapabilities_doesNotThrow() {
        composeTestRule.setContent {
            Subspace {
                CompositionLocalProvider(
                    LocalSpatialCapabilities provides SpatialCapabilities.NoCapabilities
                ) {
                    Orbiter(position = ContentEdge.Top) {
                        Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {}
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("orbiterContentBox").assertExists()
    }

    @Test
    fun orbiter_unparented_whenMainWindowResizes_resizes() {
        var triggerResize by mutableStateOf(false)

        var initialWidth = 0
        var initialHeight = 0
        var targetResizeWidth = 0
        var targetResizeHeight = 0

        composeTestRule.setContent {
            val session = checkNotNull(LocalSession.current)

            initialWidth = (session.context as Activity).window.decorView.width
            initialHeight = (session.context as Activity).window.decorView.height
            targetResizeWidth = initialWidth + 100
            targetResizeHeight = initialHeight + 100

            // This LaunchedEffect will simulate the window resize when triggerResize becomes
            // true.
            LaunchedEffect(triggerResize) {
                if (triggerResize) {
                    (session.context as Activity)
                        .window
                        .decorView
                        .layout(0, 0, targetResizeWidth, targetResizeHeight)
                }
            }

            Orbiter(ContentEdge.Top) {
                Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {
                    Text("Some Orbiter content")
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(with(composeTestRule.density) { initialWidth.toDp() })
            .assertHeightIsEqualTo(with(composeTestRule.density) { initialHeight.toDp() })

        triggerResize = true

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(with(composeTestRule.density) { targetResizeWidth.toDp() })
            .assertHeightIsEqualTo(with(composeTestRule.density) { targetResizeHeight.toDp() })
    }

    @Test
    fun orbiter_inSpatialPanel_isParentedToPanel() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(ContentEdge.Top) {
                        Box(modifier = Modifier.size(10.dp).testTag("orbiterContentBox")) {
                            Text("Some Orbiter content")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("orbiterContentBox").assertWidthIsEqualTo(10.dp)

        val session = checkNotNull(composeTestRule.session)
        val entity =
            session.scene.getEntitiesOfType(PanelEntity::class.java).first {
                it.sizeInPixels.width == 10
            }
        val parentPanel =
            session.scene.getEntitiesOfType(PanelEntity::class.java).first {
                it.sizeInPixels.width == 100
            }
        assertThat(entity.parent).isNotEqualTo(session.scene.mainPanelEntity)
        assertThat(entity.parent).isEqualTo(parentPanel)
    }

    @Test
    fun orbiter_unparented_isParentedToMainPanel() {
        composeTestRule.setContent {
            Orbiter(ContentEdge.Top) {
                Box(modifier = Modifier.size(10.dp).testTag("orbiterContentBox")) {
                    Text("Some Orbiter content")
                }
            }
        }

        composeTestRule.onNodeWithTag("orbiterContentBox").assertExists()

        val session = checkNotNull(composeTestRule.session)
        val entity =
            session.scene.getEntitiesOfType(PanelEntity::class.java).first {
                it.sizeInPixels.width == 10
            }
        assertThat(entity.parent).isEqualTo(session.scene.mainPanelEntity)
    }

    @Test
    fun orbiter_anchorPointTop_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(anchorPoint = OrbiterAnchorPoint.Top, offset = DpVolumeOffset.Zero) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)
        // Parent height / 2 + Orbiter height / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointTopStart_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.TopStart,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 - Orbiter width / 2) = -(50.dp - 5.dp) = -45.dp
        // y = Parent height / 2 + Orbiter height / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointTopEnd_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(anchorPoint = OrbiterAnchorPoint.TopEnd, offset = DpVolumeOffset.Zero) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 - Orbiter width / 2 = 50.dp - 5.dp = 45.dp
        // y = Parent height / 2 + Orbiter height / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointBottom_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(anchorPoint = OrbiterAnchorPoint.Bottom, offset = DpVolumeOffset.Zero) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // -(Parent height / 2 + Orbiter height / 2) = -(50.dp + 5.dp) = -55.dp
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointBottomStart_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.BottomStart,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 - Orbiter width / 2) = -(50.dp - 5.dp) = -45.dp
        // y = -(Parent height / 2 + Orbiter height / 2) = -(50.dp + 5.dp) = -55.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointBottomEnd_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.BottomEnd,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 - Orbiter width / 2 = 50.dp - 5.dp = 45.dp
        // y = -(Parent height / 2 + Orbiter height / 2) = -(50.dp + 5.dp) = -55.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointEnd_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(anchorPoint = OrbiterAnchorPoint.End, offset = DpVolumeOffset.Zero) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointEndTop_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(anchorPoint = OrbiterAnchorPoint.EndTop, offset = DpVolumeOffset.Zero) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        // y = Parent height / 2 - Orbiter height / 2 = 50.dp - 5.dp = 45.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointEndBottom_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.EndBottom,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        // y = -(Parent height / 2 - Orbiter height / 2) = -(50.dp - 5.dp) = -45.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointStart_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(anchorPoint = OrbiterAnchorPoint.Start, offset = DpVolumeOffset.Zero) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // -(Parent width / 2 + Orbiter width / 2) = -(50.dp + 5.dp) = -55.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointStartTop_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.StartTop,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 + Orbiter width / 2) = -(50.dp + 5.dp) = -55.dp
        // y = Parent height / 2 - Orbiter height / 2 = 50.dp - 5.dp = 45.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointStartBottom_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.StartBottom,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 + Orbiter width / 2) = -(50.dp + 5.dp) = -55.dp
        // y = -(Parent height / 2 - Orbiter height / 2) = -(50.dp - 5.dp) = -45.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointStartInRtl_positionsCorrectly() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Subspace {
                    SpatialPanel(SubspaceModifier.size(100.dp)) {
                        Orbiter(
                            anchorPoint = OrbiterAnchorPoint.Start,
                            offset = DpVolumeOffset.Zero,
                        ) {
                            Box(modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // In RTL, Start is on the right (+X)
        // Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_withOffset_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Top,
                        offset = DpVolumeOffset(10.dp, 10.dp, 10.dp),
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // Base Top position: 0, 55.dp, 0
        // Offset: 10.dp, 10.dp, 10.dp
        // Result: 10.dp, 65.dp, 10.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(10.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(65.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.z).isWithin(0.001f).of(10.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteTopLeft_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.TopLeft,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 - Orbiter width / 2) = -(50.dp - 5.dp) = -45.dp
        // y = Parent height / 2 + Orbiter height / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteTopRight_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.TopRight,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 - Orbiter width / 2 = 50.dp - 5.dp = 45.dp
        // y = Parent height / 2 + Orbiter height / 2 = 50.dp + 5.dp = 55.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteRightTop_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.RightTop,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        // y = Parent height / 2 - Orbiter height / 2 = 50.dp - 5.dp = 45.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteRight_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.Right,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        // y = 0
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(0f)
    }

    @Test
    fun orbiter_anchorPointAbsoluteRightBottom_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.RightBottom,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 + Orbiter width / 2 = 50.dp + 5.dp = 55.dp
        // y = -(Parent height / 2 - Orbiter height / 2) = -(50.dp - 5.dp) = -45.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteBottomRight_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.BottomRight,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = Parent width / 2 - Orbiter width / 2 = 50.dp - 5.dp = 45.dp
        // y = -(Parent height / 2 + Orbiter height / 2) = -(50.dp + 5.dp) = -55.dp
        assertThat(orbiterEntity.getPose().translation.x).isWithin(0.001f).of(45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteBottomLeft_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.BottomLeft,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 - Orbiter width / 2) = -(50.dp - 5.dp) = -45.dp
        // y = -(Parent height / 2 + Orbiter height / 2) = -(50.dp + 5.dp) = -55.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteLeftBottom_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.LeftBottom,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 + Orbiter width / 2) = -(50.dp + 5.dp) = -55.dp
        // y = -(Parent height / 2 - Orbiter height / 2) = -(50.dp - 5.dp) = -45.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y)
            .isWithin(0.001f)
            .of(-45.dp.toMeter().toM())
    }

    @Test
    fun orbiter_anchorPointAbsoluteLeft_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.Left,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 + Orbiter width / 2) = -(50.dp + 5.dp) = -55.dp
        // y = 0
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(0f)
    }

    @Test
    fun orbiter_anchorPointAbsoluteLeftTop_positionsCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(
                        anchorPoint = OrbiterAnchorPoint.Absolute.LeftTop,
                        offset = DpVolumeOffset.Zero,
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        val density = composeTestRule.density
        val orbiterEntity = getOrbiterEntity(10.dp, density)

        // x = -(Parent width / 2 + Orbiter width / 2) = -(50.dp + 5.dp) = -55.dp
        // y = Parent height / 2 - Orbiter height / 2 = 50.dp - 5.dp = 45.dp
        assertThat(orbiterEntity.getPose().translation.x)
            .isWithin(0.001f)
            .of(-55.dp.toMeter().toM())
        assertThat(orbiterEntity.getPose().translation.y).isWithin(0.001f).of(45.dp.toMeter().toM())
    }

    private fun getOrbiterEntity(size: Dp, density: Density): PanelEntity {
        val sizePx = with(density) { size.roundToPx() }
        val session = checkNotNull(composeTestRule.session)
        return session.scene.getEntitiesOfType(PanelEntity::class.java).first {
            it.sizeInPixels.width == sizePx
        }
    }
}
