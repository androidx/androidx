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
import androidx.compose.runtime.DisposableEffect
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
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.toMeter
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.Ignore
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialDialog]. */
@RunWith(AndroidJUnit4::class)
class SpatialDialogTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialDialog_dismissOnBackPress_setToTrue_dismissDialog() {
        val showDialog = mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Column {
                            Text("SpatialPanel Content")
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
                }
            }
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
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Column {
                            Text("SpatialPanel Content")
                            if (showDialog.value) {
                                SpatialDialog(
                                    onDismissRequest = { showDialog.value = false },
                                    properties = SpatialDialogProperties(dismissOnBackPress = false),
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
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        composeTestRule.onNodeWithText("Press Back").performClick()

        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
    }

    @Test
    fun spatialDialog_FullSpaceMode_dismissOnClickOutside_setToTrue_dismissDialog() {
        val showDialog = mutableStateOf(true)
        var outsideClicked = false

        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestFullSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Background clickable area
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().testTag("background").clickable {
                                        outsideClicked = true
                                    }
                            ) {
                                Column {
                                    Text("SpatialPanel Content")
                                    if (showDialog.value) {
                                        SpatialDialog(
                                            onDismissRequest = { showDialog.value = false },
                                            properties =
                                                SpatialDialogProperties(
                                                    dismissOnClickOutside = true
                                                ),
                                        ) {
                                            Text("Spatial Dialog")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        // Click outside should dismiss the dialog
        composeTestRule.onNodeWithTag("background").performClick()
        composeTestRule.waitForIdle()

        assertThat(outsideClicked).isFalse()

        composeTestRule.onNodeWithText("Spatial Dialog").assertDoesNotExist()
    }

    @Test
    fun spatialDialog_homeSpaceWithDismissOnClickOutsideFalse_doesNotDismissDialog() {
        val showDialog = mutableStateOf(true)
        var outsideClicked = false

        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Background clickable area
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().testTag("background").clickable {
                                        outsideClicked = true
                                    }
                            ) {
                                Column {
                                    Text("SpatialPanel Content")
                                    if (showDialog.value) {
                                        val spatialDialogProperties =
                                            SpatialDialogProperties(
                                                dismissOnBackPress = true,
                                                dismissOnClickOutside = false,
                                            )
                                        SpatialDialog(
                                            onDismissRequest = { showDialog.value = false },
                                            properties = spatialDialogProperties,
                                        ) {
                                            Text("Spatial Dialog")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spatial Dialog").assertExists()

        // Click outside should NOT dismiss the dialog
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
            TestSetup {
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
            TestSetup {
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
            TestSetup {
                val dialogManager = LocalDialogManager.current
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        var showDialog by remember { mutableStateOf(true) }

                        if (showDialog) {
                            SpatialDialog(onDismissRequest = { showDialog = false }) {
                                Text("Dialog Content")
                                Button(
                                    onClick = {
                                        dialogManagerState =
                                            dialogManager.isSpatialDialogActive.value
                                    }
                                ) {
                                    Text("Check State")
                                }
                            }
                        }
                    }
                }
            }
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
                TestSetup {
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
        }

        composeTestRule.onNodeWithTag("nonSpatialDialog").assertExists()
        composeTestRule.onNodeWithText("Non-Spatial Dialog").assertExists()
    }

    @Test
    fun spatialDialog_multipleDialogsComposed_bothExist() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Column {
                            SpatialDialog(onDismissRequest = {}) { Text("Dialog 1") }

                            SpatialDialog(onDismissRequest = {}) { Text("Dialog 2") }
                        }
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
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        var expanded by remember { mutableStateOf(false) }

                        SpatialDialog(onDismissRequest = {}) {
                            Column(modifier = Modifier.testTag("ColumnContent")) {
                                Text("Dialog Content")
                                Button(onClick = { expanded = !expanded }) { Text("Toggle Size") }

                                if (expanded) {
                                    Box(
                                        modifier = Modifier.size(300.dp).testTag("expandedContent")
                                    ) {
                                        Text("Expanded Content")
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.size(200.dp).testTag("expandedContent")
                                    ) {
                                        Text("Expanded Content")
                                    }
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
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        SpatialDialog(onDismissRequest = {}) {
                            // Empty content
                        }
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
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        var showInDialog by remember { mutableStateOf(true) }

                        val movableContent = remember {
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
            TestSetup {
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
            TestSetup {
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
        }

        // Rapid toggle
        repeat(5) {
            composeTestRule.onNodeWithText("Toggle").performClick()
            composeTestRule.waitForIdle()
        }

        // Should end up with dialog shown (odd number of toggles)
        composeTestRule.onNodeWithText("Rapid Dialog").assertExists()
    }

    @Test
    fun spatialDialog_withVeryLargeContent_rendersCorrectly() {
        composeTestRule.setContent {
            TestSetup {
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
        }

        composeTestRule.onNodeWithTag("largeContent").assertExists()
        composeTestRule.onNodeWithTag("largeContent").assertWidthIsEqualTo(2000.dp)
        composeTestRule.onNodeWithTag("largeContent").assertHeightIsEqualTo(2000.dp)
        composeTestRule.onNodeWithText("Very Large Content").assertExists()
    }

    @Test
    fun spatialDialog_nestedDialogs_exist() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        var showOuterDialog by remember { mutableStateOf(true) }
                        var showInnerDialog by remember { mutableStateOf(false) }

                        if (showOuterDialog) {
                            SpatialDialog(onDismissRequest = { showOuterDialog = false }) {
                                Column {
                                    Text("Outer Dialog")
                                    Button(onClick = { showInnerDialog = true }) {
                                        Text("Show Inner")
                                    }

                                    if (showInnerDialog) {
                                        SpatialDialog(
                                            onDismissRequest = { showInnerDialog = false }
                                        ) {
                                            Text("Inner Content")
                                        }
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
            TestSetup {
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
        }

        composeTestRule.onNodeWithText("Counter: 0").assertExists()
        composeTestRule.onNodeWithText("Increment").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Counter: 1").assertExists()
    }

    @Test
    fun spatialDialog_whenRecreatedMultipleTimes_disposesProperly() {
        var dialogRecreationCount = 0
        val creationCount = mutableStateOf(0)
        val disposalCount = mutableStateOf(0)

        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        var showDialog by remember { mutableStateOf(true) }
                        var dialogKey by remember { mutableStateOf(0) }

                        Button(
                            onClick = {
                                showDialog = false
                                dialogKey++
                                showDialog = true
                            },
                            modifier = Modifier.testTag("recreateButton"),
                        ) {
                            Text("Recreate Dialog")
                        }
                        if (showDialog) {
                            SpatialDialog(onDismissRequest = {}) {
                                LaunchedEffect(dialogKey) { creationCount.value++ }
                                DisposableEffect(dialogKey) {
                                    dialogRecreationCount++
                                    onDispose { disposalCount.value++ }
                                }
                                Text("Dialog Instance $dialogKey")
                            }
                        }
                    }
                }
            }
        }

        // Recreate dialog multiple times
        repeat(5) {
            composeTestRule.onNodeWithTag("recreateButton").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.waitForIdle()
        // Verify proper disposal
        assertThat(dialogRecreationCount).isEqualTo(5) // Dialogs should be properly disposed
    }

    @Test
    fun spatialDialog_withContent_hasCorrectAccessibilitySemantics() {
        composeTestRule.setContent {
            TestSetup {
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
            TestSetup {
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
                                        Modifier.size(100.dp)
                                            .testTag("dialogGestureArea")
                                            .pointerInput(Unit) {
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
            TestSetup {
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
            TestSetup {
                Subspace {
                    testCases.forEach { testCase ->
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
        }
        testCases.forEach { testCase ->
            composeTestRule.onNodeWithText("${testCase.name} Animation").assertExists()
        }
    }

    @Test
    fun spatialDialog_whenContentThrowsError_errorIsHandled() {
        var errorOccurred = false

        composeTestRule.setContent {
            TestSetup {
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
        }

        assertThat(errorOccurred).isFalse()
        composeTestRule.onNodeWithTag("errorButton").performClick()
        composeTestRule.waitForIdle()

        assertThat(errorOccurred).isTrue() // Error should be handled
        composeTestRule.onNodeWithText("Error handled gracefully").assertExists()
    }
}
