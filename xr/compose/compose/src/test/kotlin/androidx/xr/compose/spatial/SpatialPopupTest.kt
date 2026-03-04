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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.ShadowActivityEmbeddingController
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.Ignore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for [SpatialPopup]. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowActivityEmbeddingController::class])
class SpatialPopupTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    // TODO(b/431079857): Fix underline implementation first and un-ignore this.
    @Ignore("Fix underline implementation first")
    @Test
    fun spatialPopup_HSM_dismissOnBackPressTrue_invokesDismissRequest() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showPopup by remember { mutableStateOf(true) }

                    if (showPopup) {
                        SpatialPopup(
                            onDismissRequest = { showPopup = false },
                            properties = PopupProperties(dismissOnBackPress = true),
                        ) {
                            Box(modifier = Modifier.size(330.dp).background(Color.Black)) {
                                Text("Spatial Popup")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Spatial Popup").assertDoesNotExist()
    }

    @Test
    fun spatialPopup_FSM_dismissOnBackPressTrue_invokesDismissRequest() {
        composeTestRule.configureFakeSession().scene.requestFullSpaceMode()

        composeTestRule.setContent {
            var showPopup1 by remember { mutableStateOf(true) }
            if (showPopup1) {
                SpatialPopup(
                    onDismissRequest = { showPopup1 = false },
                    properties = PopupProperties(dismissOnBackPress = true),
                ) {
                    Text("Spatial Popup")
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Spatial Popup").assertDoesNotExist()
    }

    @Test
    fun spatialPopup_HSM_dismissOnBackPressFalse_doesNotInvokeDismissRequest() {
        var showPopup by mutableStateOf(true)
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            if (showPopup) {
                SpatialPopup(
                    onDismissRequest = { showPopup = false },
                    properties = PopupProperties(dismissOnBackPress = false),
                ) {
                    Text("Spatial Popup")
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()
    }

    @Test
    fun spatialPopup_FSM_dismissOnBackPressFalse_doesNotInvokeDismissRequest() {
        var showPopup by mutableStateOf(true)
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            if (showPopup) {
                SpatialPopup(
                    onDismissRequest = { showPopup = false },
                    properties = PopupProperties(dismissOnBackPress = false),
                ) {
                    Text("Spatial Popup")
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()
    }

    // TODO(b/431093749): Fix the underline impl and un-ignore this test.
    @Ignore("Fix underline implementation first")
    @Test
    fun spatialPopup_FSM_dismissOnClickOutsideTrue_dismissesOnOutsideClick() {
        var showPopup by mutableStateOf(true)
        var outsideClicked = false
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.Blue)
                                .testTag("background")
                                .clickable { outsideClicked = true }
                    ) {
                        if (showPopup) {
                            SpatialPopup(
                                onDismissRequest = { showPopup = false },
                                properties = PopupProperties(dismissOnClickOutside = true),
                            ) {
                                Box(modifier = Modifier.size(100.dp).background(Color.Red)) {
                                    Text("Popup Content")
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Popup Content").assertExists()

        // Click outside the popup
        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Popup Content").assertDoesNotExist()
    }

    // TODO(b/431177146): Fix underline impl and un-ignore this test.
    @Ignore("Fix underline implementation first")
    @Test
    fun spatialPopup_HSM_dismissOnClickOutsideTrue_dismissesOnOutsideClick() {
        var showPopup by mutableStateOf(true)
        var outsideClicked = false
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.Blue)
                                .testTag("background")
                                .clickable { outsideClicked = true }
                    ) {
                        if (showPopup) {
                            SpatialPopup(
                                onDismissRequest = { showPopup = false },
                                properties = PopupProperties(dismissOnClickOutside = true),
                            ) {
                                Box(modifier = Modifier.size(100.dp).background(Color.Red)) {
                                    Text("Popup Content")
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Popup Content").assertExists()

        // Click outside the popup
        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Popup Content").assertDoesNotExist()
    }

    @Test
    fun spatialPopup_HSM_dismissOnClickOutsideFalse_doesNotDismissOnOutsideClick() {
        var showPopup by mutableStateOf(true)
        var outsideClicked = false
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.Blue)
                                .testTag("background")
                                .clickable { outsideClicked = true }
                    ) {
                        if (showPopup) {
                            SpatialPopup(
                                onDismissRequest = { showPopup = false },
                                properties = PopupProperties(dismissOnClickOutside = false),
                            ) {
                                Box(modifier = Modifier.size(100.dp).background(Color.Red)) {
                                    Text("Popup Content")
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Popup Content").assertExists()

        // Click outside the popup
        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Popup Content").assertExists()
    }

    @Test
    fun spatialPopup_FSM_dismissOnClickOutsideFalse_doesNotDismissOnOutsideClick() {
        var showPopup by mutableStateOf(true)
        var outsideClicked = false
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.Blue)
                                .testTag("background")
                                .clickable { outsideClicked = true }
                    ) {
                        if (showPopup) {
                            SpatialPopup(
                                onDismissRequest = { showPopup = false },
                                properties = PopupProperties(dismissOnClickOutside = false),
                            ) {
                                Box(modifier = Modifier.size(100.dp).background(Color.Red)) {
                                    Text("Popup Content")
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Popup Content").assertExists()

        // Click outside the popup
        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Popup Content").assertExists()
    }

    // TODO(b/431096310): Test alignment not only the existence.
    @Test
    fun spatialPopup_allAlignmentOptions_exists() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(modifier = Modifier.size(300.dp)) {
                        // Test all alignments in one composition
                        SpatialPopup(alignment = Alignment.TopStart) {
                            Text("TopStart", modifier = Modifier.testTag("popup_TopStart"))
                        }
                        SpatialPopup(alignment = Alignment.TopCenter) {
                            Text("TopCenter", modifier = Modifier.testTag("popup_TopCenter"))
                        }
                        SpatialPopup(alignment = Alignment.TopEnd) {
                            Text("TopEnd", modifier = Modifier.testTag("popup_TopEnd"))
                        }
                        SpatialPopup(alignment = Alignment.CenterStart) {
                            Text("CenterStart", modifier = Modifier.testTag("popup_CenterStart"))
                        }
                        SpatialPopup(alignment = Alignment.Center) {
                            Text("Center", modifier = Modifier.testTag("popup_Center"))
                        }
                        SpatialPopup(alignment = Alignment.CenterEnd) {
                            Text("CenterEnd", modifier = Modifier.testTag("popup_CenterEnd"))
                        }
                        SpatialPopup(alignment = Alignment.BottomStart) {
                            Text("BottomStart", modifier = Modifier.testTag("popup_BottomStart"))
                        }
                        SpatialPopup(alignment = Alignment.BottomCenter) {
                            Text("BottomCenter", modifier = Modifier.testTag("popup_BottomCenter"))
                        }
                        SpatialPopup(alignment = Alignment.BottomEnd) {
                            Text("BottomEnd", modifier = Modifier.testTag("popup_BottomEnd"))
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("popup_TopStart").assertExists()
        composeTestRule.onNodeWithTag("popup_TopCenter").assertExists()
        composeTestRule.onNodeWithTag("popup_TopEnd").assertExists()
        composeTestRule.onNodeWithTag("popup_CenterStart").assertExists()
        composeTestRule.onNodeWithTag("popup_Center").assertExists()
        composeTestRule.onNodeWithTag("popup_CenterEnd").assertExists()
        composeTestRule.onNodeWithTag("popup_BottomStart").assertExists()
        composeTestRule.onNodeWithTag("popup_BottomCenter").assertExists()
        composeTestRule.onNodeWithTag("popup_BottomEnd").assertExists()
    }

    // TODO(b/431085506): Test if elevation parameter is actually applied.
    @Test
    fun spatialPopup_allElevationLevels_exists() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Column {
                        SpatialPopup(elevation = SpatialElevationLevel.Level0) { Text("Level0") }
                        SpatialPopup(elevation = SpatialElevationLevel.Level1) { Text("Level1") }
                        SpatialPopup(elevation = SpatialElevationLevel.Level2) { Text("Level2") }
                        SpatialPopup(elevation = SpatialElevationLevel.Level3) { Text("Level3") }
                        SpatialPopup(elevation = SpatialElevationLevel.Level4) { Text("Level4") }
                        SpatialPopup(elevation = SpatialElevationLevel.Level5) { Text("Level5") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Level0").assertExists()
        composeTestRule.onNodeWithText("Level1").assertExists()
        composeTestRule.onNodeWithText("Level2").assertExists()
        composeTestRule.onNodeWithText("Level3").assertExists()
        composeTestRule.onNodeWithText("Level4").assertExists()
        composeTestRule.onNodeWithText("Level5").assertExists()
    }

    // TODO(b/431177148): Un-ignore this test after making this work in test env.
    @Ignore("Test not working in test environment! In emulator it is working!")
    @Test
    fun spatialPopup_withMovableContent_movesContentWithoutRecomposition() {
        var observedCompositionId: String? = null
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showInPopup by remember { mutableStateOf(true) }

                    val movableContent =
                        remember<@Composable (() -> Unit)> {
                            movableContentOf {
                                val compositionId = remember { UUID.randomUUID().toString() }
                                observedCompositionId = compositionId
                                Text("Movable Content")
                            }
                        }

                    Column {
                        Button(
                            onClick = { showInPopup = !showInPopup },
                            modifier = Modifier.testTag("toggleButton"),
                        ) {
                            Text(if (showInPopup) "Move to Panel" else "Move to Popup")
                        }

                        Box {
                            if (showInPopup) {
                                SpatialPopup { movableContent() }
                            } else {
                                movableContent()
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Movable Content").assertExists()

        val initialId = observedCompositionId

        composeTestRule.onNodeWithTag("toggleButton").performClick()
        composeTestRule.waitForIdle()

        assertThat(observedCompositionId).isEqualTo(initialId)
        composeTestRule.onNodeWithText("Movable Content").assertExists()

        composeTestRule.onNodeWithTag("toggleButton").performClick()
        composeTestRule.waitForIdle()

        assertThat(observedCompositionId).isEqualTo(initialId)
        composeTestRule.onNodeWithText("Movable Content").assertExists()
    }

    @Test
    fun spatialPopup_nonSpatialEnvironment_fallsBackToStandardPopup() {
        val spatialCapabilities = SpatialCapabilities.NoCapabilities

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSpatialCapabilities provides spatialCapabilities) {
                SpatialPopup {
                    Box(modifier = Modifier.testTag("nonSpatialPopup")) {
                        Text("Non-Spatial Popup")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("nonSpatialPopup").assertExists()
        composeTestRule.onNodeWithText("Non-Spatial Popup").assertExists()
    }

    @Test
    fun spatialPopup_nestedPopups_bothExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialPopup {
                        Column {
                            Text("Outer Popup")
                            SpatialPopup { Text("Inner Popup") }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Outer Popup").assertExists()
        composeTestRule.onNodeWithText("Inner Popup").assertExists()
    }

    @Test
    fun spatialPopup_contentSizeChanges_updatesCorrectly() {
        var contentSize by mutableStateOf(100.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialPopup {
                        Box(modifier = Modifier.size(contentSize).testTag("resizableContent")) {
                            Text("Resizable")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("resizableContent").assertWidthIsEqualTo(100.dp)
        composeTestRule.onNodeWithTag("resizableContent").assertHeightIsEqualTo(100.dp)

        contentSize = 200.dp
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("resizableContent").assertWidthIsEqualTo(200.dp)
        composeTestRule.onNodeWithTag("resizableContent").assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun spatialPopup_emptyContent_doesNotCrash() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialPopup {
                        // Empty content
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
    }

    @Test
    fun spatialPopup_zeroSizeContent_handlesGracefully() {
        composeTestRule.setContent {
            SpatialPopup {
                Box(modifier = Modifier.size(0.dp).testTag("zeroSizeBox")) {
                    // Content with zero size
                }
            }
        }

        composeTestRule.onNodeWithTag("zeroSizeBox").assertExists()
    }

    @Test
    fun spatialPopup_veryLargeContent_rendersCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialPopup {
                        Box(modifier = Modifier.size(2000.dp).testTag("largeContent")) {
                            Text("Very Large Content")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("largeContent").assertExists()
        composeTestRule.onNodeWithTag("largeContent").assertWidthIsEqualTo(2000.dp)
        composeTestRule.onNodeWithTag("largeContent").assertHeightIsEqualTo(2000.dp)
    }

    @Test
    fun spatialPopup_rapidToggle_handlesCorrectly() = runTest {
        var showPopup by mutableStateOf(false)

        composeTestRule.setContent {
            Column {
                Button(onClick = { showPopup = !showPopup }) { Text("Toggle") }
                if (showPopup) {
                    SpatialPopup { Text("Rapid Toggle Popup") }
                }
            }
        }

        // Rapid toggle - 10 is empirically chosen to ensure rapid state changes
        repeat(10) {
            composeTestRule.onNodeWithText("Toggle").performClick()
            delay(50) // 50ms delay between toggles
        }

        composeTestRule.waitForIdle()
        // After even number of toggles, popup should not exist
        composeTestRule.onNodeWithText("Rapid Toggle Popup").assertDoesNotExist()
    }

    @Test
    fun spatialPopup_withNullOnDismissRequest_doesNotCrash() {
        var showPopup by mutableStateOf(true)

        composeTestRule.setContent {
            if (showPopup) {
                SpatialPopup(
                    onDismissRequest = null,
                    properties = PopupProperties(dismissOnBackPress = true),
                ) {
                    Text("Popup with null dismiss")
                }
            }
        }

        composeTestRule.onNodeWithText("Popup with null dismiss").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Popup with null dismiss").assertExists()
    }

    @Test
    fun spatialPopup_onDispose_called() {
        var popupDisposed = false

        composeTestRule.setContent {
            var showPopup by remember { mutableStateOf(true) }

            if (showPopup) {
                SpatialPopup {
                    DisposableEffect(Unit) { onDispose { popupDisposed = true } }
                    Text("Disposable Popup")
                    Button(onClick = { showPopup = false }) { Text("Close") }
                }
            }
        }

        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        assertThat(popupDisposed).isTrue()
    }

    @Test
    fun spatialPopup_complexContent_rendersCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialPopup {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Complex Popup Title")
                            Spacer(modifier = Modifier.size(8.dp))
                            Row {
                                Button(onClick = {}) { Text("Action 1") }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(onClick = {}) { Text("Action 2") }
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                "Additional content with multiple lines\nthat should render correctly"
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Complex Popup Title").assertExists()
        composeTestRule.onNodeWithText("Action 1").assertExists()
        composeTestRule.onNodeWithText("Action 2").assertExists()
        composeTestRule
            .onNodeWithText("Additional content with multiple lines\nthat should render correctly")
            .assertExists()
    }

    @Test
    fun spatialPopup_withManyConcurrentPopups_areAllCreated() = runTest {
        val popupCount = 100

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    val popups = remember { mutableStateListOf<Int>() }

                    Column {
                        Button(
                            onClick = { popups.addAll(0 until popupCount) },
                            modifier = Modifier.testTag("addPopups"),
                        ) {
                            Text("Add $popupCount Popups")
                        }

                        popups.forEach { index ->
                            SpatialPopup(offset = IntOffset(index * 10, index * 10)) {
                                Text("Popup $index", modifier = Modifier.testTag("popup_$index"))
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("addPopups").performClick()
        composeTestRule.waitForIdle()

        repeat(popupCount) { composeTestRule.onNodeWithTag("popup_$it").assertExists() }
    }

    @Test
    fun spatialPopup_densityChange_adaptsCorrectly() {
        var currentDensity by mutableStateOf(Density(1f))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides currentDensity) {
                SpatialPopup(offset = IntOffset(50, 50)) {
                    Box(modifier = Modifier.size(100.dp).testTag("densityPopup")) {
                        Text("Density Test")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("densityPopup").assertExists()

        currentDensity = Density(2f)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("densityPopup").assertExists()
        composeTestRule.onNodeWithTag("densityPopup").assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun spatialPopup_withAsyncContent_loadsCorrectly() = runTest {
        var contentLoaded by mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SpatialPopup {
                if (!contentLoaded) {
                    CircularProgressIndicator(modifier = Modifier.testTag("loadingIndicator"))

                    LaunchedEffect(Unit) {
                        delay(500) // Simulate async loading
                        contentLoaded = true
                    }
                } else {
                    Text("Async Content Loaded", modifier = Modifier.testTag("asyncContent"))
                }
            }
        }

        composeTestRule.onNodeWithTag("loadingIndicator").assertExists()

        composeTestRule.mainClock.advanceTimeBy(600)

        composeTestRule.waitUntil(timeoutMillis = 2000) { contentLoaded }

        composeTestRule.onNodeWithTag("loadingIndicator").assertDoesNotExist()
        composeTestRule.onNodeWithTag("asyncContent").assertExists()
    }

    @Test
    fun spatialPopup_withSpatialDialog_coexistCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showDialog by remember { mutableStateOf(false) }
                    var showPopup by remember { mutableStateOf(false) }

                    Column {
                        Button(
                            onClick = {
                                showDialog = true
                                showPopup = true
                            },
                            modifier = Modifier.testTag("showBoth"),
                        ) {
                            Text("Show Dialog and Popup")
                        }

                        if (showDialog) {
                            SpatialDialog(onDismissRequest = { showDialog = false }) {
                                Text("Spatial Dialog Content")
                            }
                        }

                        if (showPopup) {
                            SpatialPopup(
                                alignment = Alignment.TopEnd,
                                elevation = SpatialElevationLevel.Level4,
                            ) {
                                Text("Spatial Popup Content")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Dialog Content").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Spatial Popup Content").assertIsNotDisplayed()

        composeTestRule.onNodeWithTag("showBoth").performClick()
        composeTestRule.waitForIdle()

        // Both should coexist
        composeTestRule.onNodeWithText("Spatial Dialog Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spatial Popup Content").assertIsDisplayed()
    }

    // TODO(b/431085506): Test if elevation parameter is applied.
    @Test
    fun spatialPopup_withMultiplePopups_layersCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SpatialPopup(elevation = SpatialElevationLevel.Level2) {
                            Box(modifier = Modifier.size(200.dp).background(Color.Blue)) {
                                Text("Elevated Content", color = Color.White)
                            }
                        }

                        SpatialPopup(
                            alignment = Alignment.Center,
                            elevation = SpatialElevationLevel.Level4,
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(100.dp)
                                        .background(Color.Red)
                                        .testTag("popupAboveElevation")
                            ) {
                                Text("Popup Above", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("popupAboveElevation").assertExists()
        composeTestRule.onNodeWithText("Elevated Content").assertExists()
        composeTestRule.onNodeWithText("Popup Above").assertExists()
    }

    @Test
    fun spatialPopup_withAnimatedContent_animatesCorrectly() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            var expanded by remember { mutableStateOf(false) }

            SpatialPopup {
                val animatedSize by
                    animateDpAsState(targetValue = if (expanded) 200.dp else 100.dp, label = "size")

                Box(
                    modifier =
                        Modifier.size(animatedSize).background(Color.Green).testTag("animatedBox")
                ) {
                    Button(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.Center).testTag("toggleAnimation"),
                    ) {
                        Text(if (expanded) "Collapse" else "Expand")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Expand").assertExists()

        composeTestRule.onNodeWithTag("animatedBox").assertWidthIsEqualTo(100.dp)
        composeTestRule.onNodeWithTag("animatedBox").assertHeightIsEqualTo(100.dp)

        composeTestRule.onNodeWithTag("toggleAnimation").performClick()

        // Advance animation
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("animatedBox").assertWidthIsEqualTo(200.dp)
        composeTestRule.onNodeWithTag("animatedBox").assertHeightIsEqualTo(200.dp)

        composeTestRule.onNodeWithText("Collapse").assertExists()
    }

    @Test
    fun spatialPopup_withDropdownMenu_behavesCorrectly() {
        composeTestRule.setContent {
            var showDropdown by remember { mutableStateOf(false) }
            var selectedItem by remember { mutableStateOf("None") }

            SpatialPopup(alignment = Alignment.TopCenter) {
                Column {
                    Button(
                        onClick = { showDropdown = true },
                        modifier = Modifier.testTag("dropdownButton"),
                    ) {
                        Text("Selected: $selectedItem")
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                    ) {
                        listOf("Option 1", "Option 2", "Option 3").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedItem = option
                                    showDropdown = false
                                },
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("dropdownButton").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Option 2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Selected: Option 2").assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun spatialPopup_withModalBottomSheet_interactsCorrectly() {
        var popupTextString = "N/A"
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    val scope = rememberCoroutineScope()
                    val sheetState = rememberModalBottomSheetState()
                    var showBottomSheet by remember { mutableStateOf(false) }
                    var popupText by remember { mutableStateOf("Initial Text") }

                    Scaffold {
                        Column(modifier = Modifier.padding(it)) {
                            // Popup that can be updated from bottom sheet
                            SpatialPopup(alignment = Alignment.TopStart) {
                                Card {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = popupText,
                                            modifier = Modifier.testTag("popup_text"),
                                        )
                                        Button(
                                            onClick = { showBottomSheet = true },
                                            modifier = Modifier.testTag("showSheet"),
                                        ) {
                                            Text("Edit in Sheet")
                                        }
                                    }
                                }
                            }

                            if (showBottomSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { showBottomSheet = false },
                                    sheetState = sheetState,
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        TextField(
                                            value = popupText,
                                            onValueChange = {
                                                popupText = it
                                                popupTextString = it
                                            },
                                            label = { Text("Edit Popup Text") },
                                            modifier = Modifier.testTag("textField"),
                                        )
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    sheetState.hide()
                                                    showBottomSheet = false
                                                }
                                            },
                                            modifier = Modifier.testTag("applyChanges"),
                                        ) {
                                            Text("Apply")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("showSheet").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("textField").performTextInput("Updated ")
        composeTestRule.onNodeWithTag("applyChanges").performClick()
        composeTestRule.waitForIdle()

        assertThat(popupTextString).isEqualTo("Updated Initial Text")
        composeTestRule.onNodeWithTag("popup_text").assertTextEquals("Updated Initial Text")
    }

    @Test
    fun spatialPopup_withLazyList_scrollsIndependently() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    val items = List(50) { "Item $it" }

                    Box {
                        LazyColumn(modifier = Modifier.testTag("mainList")) {
                            items(items) { item -> Text(item, modifier = Modifier.padding(16.dp)) }
                        }

                        SpatialPopup(offset = IntOffset(70, 70)) {
                            LazyColumn(
                                modifier =
                                    Modifier.size(200.dp, 300.dp)
                                        .background(Color.LightGray)
                                        .testTag("popupList")
                            ) {
                                items(items) { item ->
                                    Text("Popup $item", modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("mainList").assertExists()
        composeTestRule.onNodeWithTag("popupList").assertExists()

        composeTestRule.onNodeWithText("Item 0").assertExists()
        composeTestRule.onNodeWithText("Popup Item 0").assertExists()

        composeTestRule.onNodeWithTag("mainList").performScrollToIndex(47)
        composeTestRule.onNodeWithTag("popupList").performScrollToIndex(47)

        composeTestRule.onNodeWithText("Item 47").assertExists()
        composeTestRule.onNodeWithText("Popup Item 47").assertExists()
    }

    @Test
    fun spatialPopup_withAnimatedVisibility_transitionsCorrectly() {
        composeTestRule.setContent {
            var visible by remember { mutableStateOf(false) }

            Column {
                Button(
                    onClick = { visible = !visible },
                    modifier = Modifier.testTag("toggleVisibility"),
                ) {
                    Text(if (visible) "Hide" else "Show")
                }

                AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                    SpatialPopup(offset = IntOffset(200, 200), alignment = Alignment.Center) {
                        Card(modifier = Modifier.size(200.dp).testTag("animatedPopup")) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text("Animated Popup", modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("toggleVisibility").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("animatedPopup").assertExists()

        composeTestRule.onNodeWithTag("toggleVisibility").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("animatedPopup").assertDoesNotExist()
    }

    @Test
    fun spatialPopup_multipleWithDifferentProperties_maintainIndependence() {
        data class PopupConfig(
            val tag: String,
            val alignment: Alignment,
            val offset: IntOffset,
            val elevation: Dp,
        )

        val popupConfigs =
            listOf(
                PopupConfig(
                    "Popup1",
                    Alignment.TopStart,
                    IntOffset(0, 0),
                    SpatialElevationLevel.Level1,
                ),
                PopupConfig(
                    "Popup2",
                    Alignment.TopEnd,
                    IntOffset(10, 10),
                    SpatialElevationLevel.Level2,
                ),
                PopupConfig(
                    "Popup3",
                    Alignment.BottomStart,
                    IntOffset(-10, -10),
                    SpatialElevationLevel.Level3,
                ),
                PopupConfig(
                    "Popup4",
                    Alignment.BottomEnd,
                    IntOffset(20, -20),
                    SpatialElevationLevel.Level4,
                ),
            )

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        popupConfigs.forEach { config ->
                            var dismissed by remember { mutableStateOf(false) }

                            if (!dismissed) {
                                SpatialPopup(
                                    alignment = config.alignment,
                                    offset = config.offset,
                                    elevation = config.elevation,
                                    onDismissRequest = { dismissed = true },
                                    properties =
                                        PopupProperties(
                                            dismissOnClickOutside = true,
                                            dismissOnBackPress = true,
                                        ),
                                ) {
                                    Card(modifier = Modifier.size(100.dp).testTag(config.tag)) {
                                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                            Text(config.tag)
                                            Text("Level: ${config.elevation}")
                                            Button(
                                                onClick = { dismissed = true },
                                                modifier = Modifier.testTag("dismiss_${config.tag}"),
                                            ) {
                                                Text("X")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verify all popups exist
        popupConfigs.forEach { config -> composeTestRule.onNodeWithTag(config.tag).assertExists() }

        // Dismiss one popup shouldn't affect others
        composeTestRule.onNodeWithTag("dismiss_Popup2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("Popup2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("Popup1").assertExists()
        composeTestRule.onNodeWithTag("Popup3").assertExists()
        composeTestRule.onNodeWithTag("Popup4").assertExists()
    }

    @Test
    fun spatialPopup_withTapHandling_processesCorrectly() {
        var popupTapped = false
        var backgroundTapped = false

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Gray)
                        .clickable { backgroundTapped = true }
                        .testTag("background")
            ) {
                Text(
                    "Background Tap Area",
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                )

                SpatialPopup(
                    alignment = Alignment.Center,
                    properties = PopupProperties(dismissOnClickOutside = false),
                ) {
                    Box(
                        modifier =
                            Modifier.size(200.dp).background(Color.Blue).testTag("popupContent")
                    ) {
                        Button(
                            onClick = { popupTapped = true },
                            modifier = Modifier.align(Alignment.Center).testTag("popupButton"),
                        ) {
                            Text("Tap Me", color = Color.White)
                        }
                    }
                }
            }
        }

        // Test popup tap
        composeTestRule.onNodeWithTag("popupButton").performClick()
        composeTestRule.waitForIdle()

        assertThat(popupTapped).isTrue()
        assertThat(backgroundTapped).isFalse()
    }

    // TODO(b/431085506): Test if the elevation param is applied.
    @Test
    fun spatialPopup_differentAlignmentsWithOffset_exist() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp)) {
                Box(modifier = Modifier.size(200.dp).align(Alignment.Center)) {
                    SpatialPopup(alignment = Alignment.TopStart, offset = IntOffset(10, 10)) {
                        Text("TopStart with offset", modifier = Modifier.testTag("popup_TopStart"))
                    }

                    SpatialPopup(alignment = Alignment.Center, offset = IntOffset(10, 10)) {
                        Text("Center with offset", modifier = Modifier.testTag("popup_Center"))
                    }

                    SpatialPopup(alignment = Alignment.BottomEnd, offset = IntOffset(10, 10)) {
                        Text(
                            "BottomEnd with offset",
                            modifier = Modifier.testTag("popup_BottomEnd"),
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("popup_TopStart").assertExists()
        composeTestRule.onNodeWithTag("popup_Center").assertExists()
        composeTestRule.onNodeWithTag("popup_BottomEnd").assertExists()
    }

    @Test
    fun spatialPopup_maxIntOffset_handlesGracefully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SpatialPopup(offset = IntOffset(Int.MAX_VALUE, Int.MAX_VALUE)) {
                    Text("Max Int Offset", modifier = Modifier.testTag("popup"))
                }
            }
        }

        composeTestRule.onNodeWithTag("popup").assertExists()
    }

    @Test
    fun spatialPopup_minIntOffset_handlesGracefully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SpatialPopup(
                    alignment = Alignment.Center,
                    offset = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE),
                ) {
                    Text("Min Int Offset", modifier = Modifier.testTag("popup"))
                }
            }
        }

        composeTestRule.onNodeWithTag("popup").assertExists()
    }

    @Test
    fun spatialPopup_maxXMinYOffset_handlesGracefully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SpatialPopup(
                    alignment = Alignment.Center,
                    offset = IntOffset(Int.MAX_VALUE, Int.MIN_VALUE),
                ) {
                    Text("Max X Min Y", modifier = Modifier.testTag("popup"))
                }
            }
        }

        composeTestRule.onNodeWithTag("popup").assertExists()
    }

    @Test
    fun spatialPopup_minXMaxYOffset_handlesGracefully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SpatialPopup(
                    alignment = Alignment.Center,
                    offset = IntOffset(Int.MIN_VALUE, Int.MAX_VALUE),
                ) {
                    Text("Min X Max Y", modifier = Modifier.testTag("popup"))
                }
            }
        }

        composeTestRule.onNodeWithTag("popup").assertExists()
    }

    @Test
    fun spatialPopup_veryLargePositiveOffset_handlesGracefully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SpatialPopup(alignment = Alignment.Center, offset = IntOffset(10000, 10000)) {
                    Text("Very Large Positive", modifier = Modifier.testTag("popup"))
                }
            }
        }
        composeTestRule.onNodeWithTag("popup").assertExists()
    }

    @Test
    fun spatialPopup_veryLargeNegativeOffset_handlesGracefully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                SpatialPopup(offset = IntOffset(-10000, -10000)) {
                    Text("Very Large Negative", modifier = Modifier.testTag("popup"))
                }
            }
        }
        composeTestRule.onNodeWithTag("popup").assertExists()
    }

    // TODO(b/431177149): Fix the impl and un-ignore the test.
    @Ignore("First, fix implementation, before un-ignore this test")
    @Test
    fun spatialPopup_fullSpaceLtr_positionsCorrectly() {
        correctPositionTest(isHomeSpace = false, layoutDirection = LayoutDirection.Ltr)
    }

    @Test
    fun spatialPopup_homeSpaceLtr_positionsCorrectly() {
        correctPositionTest(isHomeSpace = true, layoutDirection = LayoutDirection.Ltr)
    }

    // TODO(b/431081587): Fix the impl and un-ignore the test.
    @Ignore("First, fix implementation, before un-ignore this test")
    @Test
    fun spatialPopup_fullSpaceRtl_positionsCorrectly() {
        correctPositionTest(isHomeSpace = false, layoutDirection = LayoutDirection.Rtl)
    }

    @Test
    fun spatialPopup_homeSpaceRtl_positionsCorrectly() {
        correctPositionTest(isHomeSpace = true, layoutDirection = LayoutDirection.Rtl)
    }

    private fun correctPositionTest(isHomeSpace: Boolean, layoutDirection: LayoutDirection) {
        composeTestRule.configureFakeSession()
        if (isHomeSpace) {
            composeTestRule.session?.scene?.requestHomeSpaceMode()
        } else {
            composeTestRule.session?.scene?.requestFullSpaceMode()
        }

        val parentSize = 300.dp
        val subParentSize = 140.dp
        val popupSize = 100.dp
        val popupOffset = 20.dp

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Box(
                            modifier =
                                Modifier.size(parentSize)
                                    .background(color = Color.Gray)
                                    .testTag("parent_box_$layoutDirection")
                        ) {
                            val valueInPx =
                                with(LocalDensity.current) { popupOffset.toPx().toInt() }
                            Box(
                                modifier =
                                    Modifier.size(subParentSize)
                                        .background(color = Color.Black)
                                        .testTag("sub_box_$layoutDirection")
                            ) {
                                SpatialPopup(offset = IntOffset(valueInPx, valueInPx)) {
                                    Box(
                                        modifier =
                                            Modifier.size(popupSize)
                                                .background(color = Color.Blue)
                                                .testTag("box_popup_$layoutDirection")
                                    ) {
                                        Text(
                                            "$layoutDirection",
                                            modifier =
                                                Modifier.testTag("popup_text_$layoutDirection")
                                                    .align(Alignment.CenterStart),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("parent_box_$layoutDirection").assertExists()
        composeTestRule.onNodeWithTag("sub_box_$layoutDirection").assertExists()
        composeTestRule.onNodeWithTag("box_popup_$layoutDirection").assertExists()
        composeTestRule.onNodeWithTag("popup_text_$layoutDirection").assertExists()

        val boxPositionOnScreen =
            composeTestRule
                .onNodeWithTag("box_popup_$layoutDirection")
                .fetchSemanticsNode()
                .positionOnScreen
        assertThat(boxPositionOnScreen.x).isEqualTo(popupOffset.value)
        assertThat(boxPositionOnScreen.y).isEqualTo(popupOffset.value)

        val textPositionOnScreen =
            composeTestRule
                .onNodeWithTag("popup_text_$layoutDirection")
                .fetchSemanticsNode()
                .positionOnScreen
        assertThat(textPositionOnScreen.x).isEqualTo(popupOffset.value)
    }

    @Test
    fun spatialPopup_whenActivityIsEmbedded_fallsBackToStandardPopup() {
        ShadowActivityEmbeddingController.isEmbedded = true

        composeTestRule.setContent { SpatialPopup { Text("Fallback Content") } }
        composeTestRule.onNodeWithText("Fallback Content")

        ShadowActivityEmbeddingController.isEmbedded = false
    }
}
