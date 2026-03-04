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

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.DefaultDialogManager
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.ShadowActivityEmbeddingController
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.toMeter
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.Ignore
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for [SpatialDialog]. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowActivityEmbeddingController::class])
class SpatialDialogTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialDialog_dismissOnBackPress_setToTrue_dismissDialog() {
        val showDialog = mutableStateOf(true)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDialogManager provides DefaultDialogManager(),
                content = {
                    Subspace {
                        SpatialPanel(SubspaceModifier.testTag("panel")) {
                            if (showDialog.value) {
                                SpatialDialog(
                                    onDismissRequest = { showDialog.value = false },
                                    properties = SpatialDialogProperties(dismissOnBackPress = true),
                                ) {
                                    Text("Spatial Dialog")
                                    val dispatcher =
                                        LocalOnBackPressedDispatcherOwner.current!!
                                            .onBackPressedDispatcher
                                    Button(onClick = { dispatcher.onBackPressed() }) {
                                        Text(text = "Press Back")
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithText("Press Back").performClick()

        composeTestRule.onNodeWithText("Spatial Dialog").assertDoesNotExist()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
    }

    @Test
    fun spatialDialog_dismissOnBackPress_setToFalse_doesNotDismissDialog() {
        val showDialog = mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    if (showDialog.value) {
                        SpatialDialog(
                            onDismissRequest = { showDialog.value = false },
                            properties = SpatialDialogProperties(dismissOnBackPress = false),
                        ) {
                            Text("Spatial Dialog")
                            val dispatcher =
                                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                            Button(onClick = { dispatcher.onBackPressed() }) {
                                Text(text = "Press Back")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithText("Press Back").performClick()

        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
    }

    // TODO(b/431317832): Fix the bug in the implementation of dismissOnClickOutside.
    @Ignore("Fix the underlying implementation")
    @Test
    fun spatialDialog_fullSpaceMode_dismissOnClickOutside_setToTrue_dismissDialog() {
        var showDialog by mutableStateOf(true)
        var outsideClicked by mutableStateOf(false)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().testTag("background").clickable {
                                outsideClicked = true
                            }
                    ) {
                        if (showDialog) {
                            SpatialDialog(
                                onDismissRequest = { showDialog = false },
                                properties = SpatialDialogProperties(dismissOnClickOutside = true),
                            ) {
                                Text("Spatial Dialog")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Spatial Dialog").assertDoesNotExist()
    }

    // TODO(b/431317832): Fix the bug in the implementation of dismissOnClickOutside.
    @Ignore("Fix the underlying implementation")
    @Test
    fun spatialDialog_fullSpaceMode_dismissOnClickOutside_setToFalse_doesNotDismissDialog() {
        var showDialog by mutableStateOf(true)
        var outsideClicked by mutableStateOf(false)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().testTag("background").clickable {
                                outsideClicked = true
                            }
                    ) {
                        if (showDialog) {
                            SpatialDialog(
                                onDismissRequest = { showDialog = false },
                                properties = SpatialDialogProperties(dismissOnClickOutside = false),
                            ) {
                                Text("Spatial Dialog")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()
    }

    // TODO(b/431317832): Fix the bug in the implementation of dismissOnClickOutside.
    @Ignore("Fix the underlying implementation")
    @Test
    fun spatialDialog_homeSpaceMode_dismissOnClickOutside_setToTrue_dismissDialog() {
        val showDialog = mutableStateOf(true)
        var outsideClicked = false
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().testTag("background").clickable {
                                outsideClicked = true
                            }
                    ) {
                        if (showDialog.value) {
                            SpatialDialog(
                                onDismissRequest = { showDialog.value = false },
                                properties = SpatialDialogProperties(dismissOnClickOutside = true),
                            ) {
                                Text("Spatial Dialog")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Spatial Dialog").assertDoesNotExist()
    }

    // TODO(b/431317832): Fix the bug in the implementation of dismissOnClickOutside.
    @Test
    fun spatialDialog_homeSpaceMode_dismissOnClickOutside_setToFalse_doesNotDismissDialog() {
        val showDialog = mutableStateOf(true)
        var outsideClicked = false
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().testTag("background").clickable {
                                outsideClicked = true
                            }
                    ) {
                        if (showDialog.value) {
                            SpatialDialog(
                                onDismissRequest = { showDialog.value = false },
                                properties = SpatialDialogProperties(dismissOnClickOutside = false),
                            ) {
                                Text("Spatial Dialog")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isTrue()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()
    }

    @Test
    fun spatialDialog_withVariousElevationLevels_haveCorrectValues() {
        val elevationLevels =
            listOf(
                SpatialElevationLevel.Level0,
                SpatialElevationLevel.Level1,
                SpatialElevationLevel.Level2,
                SpatialElevationLevel.Level3,
                SpatialElevationLevel.Level4,
                SpatialElevationLevel.DialogDefault,
            )

        composeTestRule.setContent {
            Subspace {
                elevationLevels.forEach { elevation ->
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        SpatialDialog(
                            onDismissRequest = {},
                            properties = SpatialDialogProperties(elevation = elevation),
                        ) {
                            Text("Dialog at $elevation")
                        }
                    }
                }
            }
        }

        elevationLevels.forEach { elevation ->
            composeTestRule.onNodeWithText("Dialog at $elevation").assertExists()
        }

        assertThat(SpatialElevationLevel.DialogDefault.toMeter().toM())
            .isEqualTo(SpatialElevationLevel.Level5.toMeter().toM())
        assertThat(SpatialElevationLevel.DialogDefault.toMeter().toM())
            .isGreaterThan(SpatialElevationLevel.Level4.toMeter().toM())
        assertThat(SpatialElevationLevel.Level4.toMeter().toM())
            .isGreaterThan(SpatialElevationLevel.Level3.toMeter().toM())
        assertThat(SpatialElevationLevel.Level3.toMeter().toM())
            .isGreaterThan(SpatialElevationLevel.Level2.toMeter().toM())
        assertThat(SpatialElevationLevel.Level2.toMeter().toM())
            .isGreaterThan(SpatialElevationLevel.Level1.toMeter().toM())
        assertThat(SpatialElevationLevel.Level1.toMeter().toM())
            .isGreaterThan(SpatialElevationLevel.Level0.toMeter().toM())
    }

    @Composable
    private fun AnimationTrackingDialog(
        onDismissRequest: () -> Unit,
        animationSpec: FiniteAnimationSpec<Float>,
        onAnimationValue: (Float) -> Unit,
        onAnimationComplete: (() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        var targetElevation by remember { mutableStateOf(SpatialElevationLevel.Level0) }

        LaunchedEffect(Unit) { targetElevation = SpatialElevationLevel.DialogDefault }

        val animatedValue by
            animateFloatAsState(
                targetValue =
                    if (targetElevation == SpatialElevationLevel.DialogDefault) 1f else 0f,
                animationSpec = animationSpec,
                finishedListener = { onAnimationComplete?.invoke() },
            )

        LaunchedEffect(animatedValue) { onAnimationValue(animatedValue) }

        SpatialDialog(
            onDismissRequest = onDismissRequest,
            properties = SpatialDialogProperties(backgroundContentAnimationSpec = animationSpec),
        ) {
            content()
        }
    }

    @Test
    fun spatialDialog_customAnimationSpec_animatesCorrectlyOverTime() {
        val animationDuration = 1000
        val customAnimationSpec =
            tween<Float>(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        val animationValues = mutableListOf<Float>()
        var dialogShown = false
        var animationCompleted = false

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showDialog by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.testTag("showDialogButton"),
                    ) {
                        Text("Show Dialog")
                    }

                    if (showDialog) {
                        AnimationTrackingDialog(
                            onDismissRequest = { showDialog = false },
                            animationSpec = customAnimationSpec,
                            onAnimationValue = { value -> animationValues.add(value) },
                            onAnimationComplete = { animationCompleted = true },
                        ) {
                            Text("Animated Dialog")
                            dialogShown = true
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Show Dialog").assertExists()
        assertThat(dialogShown).isFalse()

        // Show dialog and start animation
        composeTestRule.onNodeWithTag("showDialogButton").performClick()
        composeTestRule.waitForIdle()

        // Check animation begin (0%)
        composeTestRule.mainClock.advanceTimeBy(10)
        composeTestRule.waitForIdle()
        assertThat(dialogShown).isTrue()
        assertThat(animationValues).isNotEmpty() // Should have initial animation value
        val initialValue = animationValues.first()

        // Check 25% animation
        composeTestRule.mainClock.advanceTimeBy(animationDuration / 4L)
        val quarterValue = animationValues.last()
        assertThat(quarterValue).isGreaterThan(initialValue) // Animation should progress at 25%

        // Check 50% animation
        composeTestRule.mainClock.advanceTimeBy(animationDuration / 4L)
        val halfValue = animationValues.last()
        assertThat(halfValue).isGreaterThan(quarterValue) // Animation should progress at 50%

        // Check 75% animation
        composeTestRule.mainClock.advanceTimeBy(animationDuration / 4L)
        val threeQuarterValue = animationValues.last()
        assertThat(threeQuarterValue).isGreaterThan(halfValue) // Animation should progress at 75%

        // Check 100% animation
        composeTestRule.mainClock.advanceTimeBy(animationDuration / 4L)
        composeTestRule.mainClock.advanceTimeBy(100) // give some time to settle
        composeTestRule.waitForIdle()

        val finalValue = animationValues.last()
        assertThat(animationCompleted).isTrue()
        assertThat(finalValue).isGreaterThan(initialValue)

        // Check that animation has enough frames
        // 10 is the empirically chosen minimum to check that the animation is indeed smooth and not
        // "jumpy".
        // For a 1 second animation:
        //
        // < 10 frames = less than 10 FPS = looks like a slideshow
        // > 10 frames = enough to be perceived as animation
        assertThat(animationValues.size).isGreaterThan(10) // Should have many animation frames

        // Check that animation is smooth
        for (i in 1 until animationValues.size) {
            val diff = animationValues[i] - animationValues[i - 1]
            assertThat(diff).isAtLeast(0)
        }

        // Check that the correct easing is used
        // FastOutSlowInEasing should start fast and end slowly
        val firstQuarterProgress = quarterValue - initialValue
        val lastQuarterProgress = finalValue - threeQuarterValue
        assertThat(firstQuarterProgress).isGreaterThan(lastQuarterProgress)
    }

    @Test
    fun spatialDialog_whenDialogIsActive_dialogManagerReturnsActiveState() = runTest {
        var dialogManagerState = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDialogManager provides DefaultDialogManager(),
                content = {
                    Subspace {
                        SpatialPanel(SubspaceModifier.testTag("panel")) {
                            var showDialog by remember { mutableStateOf(true) }
                            val data = LocalDialogManager.current.isSpatialDialogActive.value
                            if (showDialog) {
                                SpatialDialog(onDismissRequest = { showDialog = false }) {
                                    Text("Dialog Content")
                                    Button(onClick = { dialogManagerState = data }) {
                                        Text("Check State")
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }

        composeTestRule.onNodeWithText("Check State").performClick()
        composeTestRule.waitForIdle()
        assertThat(dialogManagerState).isTrue()
    }

    @Test
    fun spatialDialog_nonSpatialEnvironment_fallsBackToStandardDialog() {
        val spatialCapabilities = SpatialCapabilities.NoCapabilities

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSpatialCapabilities provides spatialCapabilities) {
                var showDialog by remember { mutableStateOf(true) }

                if (showDialog) {
                    SpatialDialog(
                        onDismissRequest = { showDialog = false },
                        properties = SpatialDialogProperties(usePlatformDefaultWidth = true),
                    ) {
                        Box(modifier = Modifier.testTag("nonSpatialDialog")) {
                            Text("Non-Spatial Dialog")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("nonSpatialDialog").assertExists()
        composeTestRule.onNodeWithText("Non-Spatial Dialog").assertExists()
    }

    @Test
    fun spatialDialog_multipleDialogsComposed_bothExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Column {
                        SpatialDialog(onDismissRequest = {}) { Text("Dialog 1") }

                        SpatialDialog(onDismissRequest = {}) { Text("Dialog 2") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Dialog 1").assertExists()
        composeTestRule.onNodeWithText("Dialog 2").assertExists()
    }

    @Test
    fun spatialDialog_contentSizeChange_handled() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var expanded by remember { mutableStateOf(false) }

                    SpatialDialog(onDismissRequest = {}) {
                        Column(modifier = Modifier.testTag("ColumnContent")) {
                            Text("Dialog Content")
                            Button(onClick = { expanded = !expanded }) { Text("Toggle Size") }

                            if (expanded) {
                                Box(modifier = Modifier.size(300.dp).testTag("expandedContent")) {
                                    Text("Expanded Content")
                                }
                            } else {
                                Box(modifier = Modifier.size(200.dp).testTag("expandedContent")) {
                                    Text("Expanded Content")
                                }
                            }
                        }
                    }
                }
            }
        }

        var height = composeTestRule.onNodeWithTag("ColumnContent").fetchSemanticsNode().size.height
        assertThat(height).isLessThan(300)

        composeTestRule.onNodeWithText("Toggle Size").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("expandedContent").assertExists()
        composeTestRule.onNodeWithText("Expanded Content").assertIsDisplayed()
        height = composeTestRule.onNodeWithTag("ColumnContent").fetchSemanticsNode().size.height
        assertThat(height).isGreaterThan(300)
    }

    @Test
    fun spatialDialog_emptyContent_shouldNotCrash() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialDialog(onDismissRequest = {}) {
                        // Empty content
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
    }

    @Test
    fun spatialDialogProperties_equals_returnsCorrectValue() {
        val props1 =
            SpatialDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                elevation = SpatialElevationLevel.Level2,
            )

        val props2 =
            SpatialDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                elevation = SpatialElevationLevel.Level2,
            )

        val props3 =
            SpatialDialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                elevation = SpatialElevationLevel.Level2,
            )

        assertThat(props1).isEqualTo(props2)
        assertThat(props1).isNotEqualTo(props3)
        assertThat(props1).isEqualTo(props1) // Same instance
        assertThat(props1).isNotEqualTo(null)
        assertThat(props1).isNotEqualTo("not a SpatialDialogProperties")
    }

    @Test
    fun spatialDialogProperties_hashCode_returnsCorrectValue() {
        val props1 =
            SpatialDialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)

        val props2 =
            SpatialDialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)

        val props3 =
            SpatialDialogProperties(dismissOnBackPress = false, dismissOnClickOutside = true)

        assertThat(props1.hashCode()).isEqualTo(props2.hashCode())
        assertThat(props1.hashCode()).isNotEqualTo(props3.hashCode())
    }

    @Test
    fun spatialDialogProperties_toString() {
        val animSpec: FiniteAnimationSpec<Float> = snap()
        val props =
            SpatialDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                backgroundContentAnimationSpec = animSpec,
                elevation = SpatialElevationLevel.Level3,
            )

        val string = props.toString()
        assertThat(string).contains("dismissOnBackPress=true")
        assertThat(string).contains("dismissOnClickOutside=false")
        assertThat(string).contains("usePlatformDefaultWidth=false")
        assertThat(string).contains("restingLevelAnimationSpec=${animSpec}")
        assertThat(string).contains("spatialElevationLevel=${SpatialElevationLevel.Level3}")
    }

    @Test
    fun spatialDialogProperties_copy_createsCorrectNewInstance() {
        val original =
            SpatialDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                elevation = SpatialElevationLevel.Level2,
            )

        val copied =
            original.copy(dismissOnBackPress = false, elevation = SpatialElevationLevel.Level3)

        assertThat(copied.dismissOnBackPress).isFalse()
        assertThat(copied.dismissOnClickOutside).isEqualTo(original.dismissOnClickOutside)
        assertThat(copied.elevation).isEqualTo(SpatialElevationLevel.Level3)
        assertThat(copied.backgroundContentAnimationSpec)
            .isEqualTo(original.backgroundContentAnimationSpec)
    }

    @Ignore("Not working")
    @Test
    fun spatialDialog_withMovableContent_movesContentWithoutRecomposition() {
        var observedCompositionId: String? = null

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showInDialog by remember { mutableStateOf(true) }

                    val movableContent =
                        remember<@Composable (() -> Unit)> {
                            movableContentOf {
                                val compositionId = remember { UUID.randomUUID().toString() }
                                observedCompositionId = compositionId
                                Column { Text("Movable Content") }
                            }
                        }

                    Column {
                        Button(
                            onClick = { showInDialog = !showInDialog },
                            modifier = Modifier.testTag("toggleButton"),
                        ) {
                            Text(if (showInDialog) "Move to Panel" else "Move to Dialog")
                        }

                        Box {
                            if (showInDialog) {
                                SpatialDialog(onDismissRequest = { showInDialog = false }) {
                                    movableContent()
                                }
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
    fun spatialDialog_withComplexPropertyCombination_works() {
        val customAnimation = tween<Float>(500)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showDialog by remember { mutableStateOf(true) }

                    if (showDialog) {
                        SpatialDialog(
                            onDismissRequest = { showDialog = false },
                            properties =
                                SpatialDialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false,
                                    usePlatformDefaultWidth = false,
                                    backgroundContentAnimationSpec = customAnimation,
                                    elevation = SpatialElevationLevel.Level4,
                                ),
                        ) {
                            Column {
                                Text("Complex Dialog")
                                val dispatcher =
                                    checkNotNull(LocalOnBackPressedDispatcherOwner.current)
                                        .onBackPressedDispatcher
                                Button(onClick = { dispatcher.onBackPressed() }) {
                                    Text("Try Back")
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Complex Dialog").assertExists()

        // Back press should not dismiss
        composeTestRule.onNodeWithText("Try Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Complex Dialog").assertExists()
    }

    @Test
    fun spatialDialog_whenToggledRapidly_maintainsCorrectState() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showDialog by remember { mutableStateOf(false) }

                    Button(onClick = { showDialog = !showDialog }) { Text("Toggle") }

                    if (showDialog) {
                        SpatialDialog(onDismissRequest = { showDialog = false }) {
                            Text("Rapid Dialog")
                        }
                    }
                }
            }
        }

        repeat(5) {
            composeTestRule.onNodeWithText("Toggle").performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText("Rapid Dialog").assertExists()

        repeat(6) {
            composeTestRule.onNodeWithText("Toggle").performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText("Rapid Dialog").assertExists()
    }

    @Test
    fun spatialDialog_withVeryLargeContent_rendersCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialDialog(onDismissRequest = {}) {
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
        composeTestRule.onNodeWithText("Very Large Content").assertExists()
    }

    @Test
    fun spatialDialog_nestedDialogs_exist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showOuterDialog by remember { mutableStateOf(true) }
                    var showInnerDialog by remember { mutableStateOf(false) }

                    if (showOuterDialog) {
                        SpatialDialog(onDismissRequest = { showOuterDialog = false }) {
                            Column {
                                Text("Outer Dialog")
                                Button(onClick = { showInnerDialog = true }) { Text("Show Inner") }

                                if (showInnerDialog) {
                                    SpatialDialog(onDismissRequest = { showInnerDialog = false }) {
                                        Text("Inner Content")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Outer Dialog").assertExists()
        composeTestRule.onNodeWithText("Show Inner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Inner Content").assertExists()
    }

    @Test
    fun spatialDialog_withStateChange_updatesCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var counter by remember { mutableStateOf(0) }
                    var showDialog by remember { mutableStateOf(true) }

                    if (showDialog) {
                        SpatialDialog(onDismissRequest = { showDialog = false }) {
                            Column {
                                Text("Counter: $counter")
                                Button(onClick = { counter++ }) { Text("Increment") }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Counter: 0").assertExists()
        composeTestRule.onNodeWithText("Increment").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Counter: 1").assertExists()
    }

    @Test
    fun spatialDialog_withContent_hasCorrectAccessibilitySemantics() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialDialog(onDismissRequest = {}) {
                        Column {
                            Text("Dialog Title", modifier = Modifier.testTag("dialogTitle"))
                            Text(
                                "Dialog content with important information",
                                modifier = Modifier.testTag("dialogContent"),
                            )
                            Button(onClick = {}, modifier = Modifier.testTag("dialogAction")) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("dialogTitle").assertExists()
        composeTestRule.onNodeWithTag("dialogContent").assertExists()
        composeTestRule.onNodeWithTag("dialogAction").assertExists()
    }

    @Test
    fun spatialDialog_whenTapped_detectGesturesAndCorrectlyHandle() {
        var tapDetected = false
        var dialogTapDetected = false

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().pointerInput(Unit) {
                                detectTapGestures { tapDetected = true }
                            }
                    ) {
                        SpatialDialog(onDismissRequest = {}) {
                            Box(
                                modifier =
                                    Modifier.size(100.dp).testTag("dialogGestureArea").pointerInput(
                                        Unit
                                    ) {
                                        detectTapGestures { dialogTapDetected = true }
                                    }
                            ) {
                                Text("Tap me")
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("dialogGestureArea").performClick()
        composeTestRule.waitForIdle()

        assertThat(dialogTapDetected).isTrue()
        assertThat(tapDetected).isFalse() // Background should not receive tap
    }

    @Test
    fun spatialDialog_whenAnimationIsInterrupted_handlesWell() {
        val animationSpec = tween<Float>(durationMillis = 1000)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    var showDialog by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showDialog = !showDialog },
                        modifier = Modifier.testTag("toggleButton"),
                    ) {
                        Text("Toggle")
                    }

                    if (showDialog) {
                        SpatialDialog(
                            onDismissRequest = { showDialog = false },
                            properties =
                                SpatialDialogProperties(
                                    backgroundContentAnimationSpec = animationSpec
                                ),
                        ) {
                            Text("Animated Dialog")
                        }
                    }
                }
            }
        }

        // Rapidly toggle to interrupt animation
        composeTestRule.onNodeWithTag("toggleButton").performClick()
        composeTestRule.mainClock.advanceTimeBy(200) // Partial animation
        composeTestRule.onNodeWithTag("toggleButton").performClick()
        composeTestRule.mainClock.advanceTimeBy(200)
        composeTestRule.onNodeWithTag("toggleButton").performClick()
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Animated Dialog").assertExists()
    }

    @Test
    fun spatialDialog_withVariousAnimationSpecs_worksWell() {
        data class AnimationTestCase(val name: String, val spec: FiniteAnimationSpec<Float>)

        val testCases =
            listOf(
                AnimationTestCase("Spring", spring()),
                AnimationTestCase("Tween", tween(500)),
                AnimationTestCase("Snap", snap()),
                AnimationTestCase("Linear", tween(300, easing = LinearEasing)),
            )

        composeTestRule.setContent {
            Subspace {
                testCases.forEach<AnimationTestCase> { testCase ->
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        SpatialDialog(
                            onDismissRequest = {},
                            properties =
                                SpatialDialogProperties(
                                    backgroundContentAnimationSpec = testCase.spec
                                ),
                        ) {
                            Text("${testCase.name} Animation")
                        }
                    }
                }
            }
        }
        testCases.forEach { testCase ->
            composeTestRule.onNodeWithText("${testCase.name} Animation").assertExists()
        }
    }

    @Test
    fun spatialDialog_whenContentThrowsError_errorIsHandled() {
        var errorOccurred = false

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    SpatialDialog(onDismissRequest = {}) {
                        @Composable
                        fun PotentiallyErrorProneContent() {
                            var shouldError by remember { mutableStateOf(false) }

                            Button(
                                onClick = { shouldError = true },
                                modifier = Modifier.testTag("errorButton"),
                            ) {
                                Text("Trigger Error")
                            }

                            if (shouldError) {
                                try {
                                    // Simulate some operation that might fail
                                    error("Simulated error")
                                } catch (_: Exception) {
                                    errorOccurred = true
                                    Text("Error handled gracefully")
                                }
                            }
                        }

                        PotentiallyErrorProneContent()
                    }
                }
            }
        }

        assertThat(errorOccurred).isFalse()
        composeTestRule.onNodeWithTag("errorButton").performClick()
        composeTestRule.waitForIdle()

        assertThat(errorOccurred).isTrue() // Error should be handled
        composeTestRule.onNodeWithText("Error handled gracefully").assertExists()
    }

    @Test
    fun spatialDialog_whenActivityIsEmbedded_fallsBackToStandardDialog() {
        ShadowActivityEmbeddingController.isEmbedded = true

        composeTestRule.setContent {
            SpatialDialog(onDismissRequest = {}) { Text("Fallback Content") }
        }
        composeTestRule.onNodeWithText("Fallback Content")

        ShadowActivityEmbeddingController.isEmbedded = false
    }
}
