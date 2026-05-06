/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets as WindowInsetsOverride
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ModalBottomSheetTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    private val sheetHeight = 256.dp
    private val dragHandleSize = 44.dp

    private val sheetTag = "sheetContentTag"
    private val dragHandleTag = "dragHandleTag"
    private val BackTestTag = "Back"

    @Test
    fun modalBottomSheet_isDismissedOnTapOutside() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    enabledValues =
                        setOf(SheetValue.Expanded, SheetValue.PartiallyExpanded, SheetValue.Hidden),
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.size(sheetHeight).testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 4
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_disabledClickOutside() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = false),
                ) {
                    Box(Modifier.size(sheetHeight).testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 4
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should exist
        assertThat(sheetState.isVisible).isTrue()
        rule.onNodeWithTag(sheetTag).assertExists()
    }

    @Test
    fun modalBottomSheet_isDismissedOnTapOutsideWithPadding() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            val screenHeight = LocalWindowInfo.current.containerDpSize.height
            sheetState =
                SheetState(
                    enabledValues =
                        setOf(SheetValue.Expanded, SheetValue.PartiallyExpanded, SheetValue.Hidden),
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    modifier = Modifier.padding(top = screenHeight / 2),
                    // Have top padding account for the top half of the screen
                ) {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 2
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_isDismissedOnSwipeDown() {
        var showBottomSheet by mutableStateOf(true)
        val sheetState =
            SheetState(
                enabledValues =
                    setOf(SheetValue.Expanded, SheetValue.PartiallyExpanded, SheetValue.Hidden),
                positionalThreshold = {
                    with(rule.density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                },
                velocityThreshold = {
                    with(rule.density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                },
            )
        rule.setContent {
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.size(sheetHeight).testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Swipe Down
        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_defaultStateForSmallContentIsFullExpanded() {
        lateinit var sheetState: SheetState
        var height by mutableStateOf(0.dp)

        rule.setContent {
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                Box(Modifier.fillMaxWidth().testTag(sheetTag).height(sheetHeight))
            }
        }

        height = rule.onNode(isDialog()).getUnclippedBoundsInRoot().height
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        rule.onNodeWithTag(sheetTag).assertTopPositionInRootIsEqualTo(height - sheetHeight)
    }

    @Test
    fun modalBottomSheet_defaultStateForLargeContentIsHalfExpanded() {
        lateinit var sheetState: SheetState
        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        // Deliberately use fraction != 1f
                        .fillMaxSize(0.6f)
                        .testTag(sheetTag)
                )
            }
        }
        rule.waitForIdle()
        screenHeightPx =
            with(rule.density) { rule.onNode(isDialog()).getUnclippedBoundsInRoot().height.toPx() }
        assertThat(sheetState.targetValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx / 2f)
    }

    @Test
    fun modalBottomSheet_shortSheet_isDismissedOnBackPress() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden),
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.fillMaxHeight(0.4f).testTag(sheetTag)) {
                        Button(
                            onClick = { dispatcher.onBackPressed() },
                            modifier = Modifier.testTag(BackTestTag),
                            content = { Text("Content") },
                        )
                    }
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        rule.onNodeWithTag(BackTestTag).performClick()

        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        // Popup should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_doesNotDismissOnBack_whenPropertyFalse() {
        var dismissCount = 0
        lateinit var sheetState: SheetState

        rule.setContent {
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

            // Explicitly set shouldDismissOnBackPress = false
            ModalBottomSheet(
                onDismissRequest = { dismissCount++ },
                sheetState = sheetState,
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
            ) {
                Box(Modifier.fillMaxSize())
            }

            // Ensure sheet is expanded to start
            if (!sheetState.isVisible) {
                androidx.compose.runtime.LaunchedEffect(Unit) { sheetState.show() }
            }
        }

        rule.waitForIdle()
        assertThat(sheetState.isVisible).isTrue()

        Espresso.pressBackUnconditionally()
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun modalBottomSheet_tallSheet_isDismissedOnBackPress() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    enabledValues =
                        setOf(SheetValue.Expanded, SheetValue.PartiallyExpanded, SheetValue.Hidden),
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.fillMaxHeight(0.6f).testTag(sheetTag)) {
                        Button(
                            onClick = { dispatcher.onBackPressed() },
                            modifier = Modifier.testTag(BackTestTag),
                            content = { Text("Content") },
                        )
                    }
                }
            }
        }
        assertThat(sheetState.isVisible).isTrue()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        // Popup should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_shortSheet_sizeChanges_snapsToNewTarget() {
        lateinit var state: SheetState
        var size by mutableStateOf(56.dp)
        var screenHeight by mutableStateOf(0.dp)
        val expectedExpandedAnchor by derivedStateOf {
            with(rule.density) { (screenHeight - size).toPx() }
        }

        rule.setContent {
            val context = LocalContext.current
            screenHeight = LocalWindowInfo.current.containerDpSize.height
            state = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                Box(Modifier.height(size).fillMaxWidth())
            }
        }
        screenHeight = rule.onNode(isDialog()).getUnclippedBoundsInRoot().height
        assertThat(state.requireOffset()).isWithin(1f).of(expectedExpandedAnchor)

        size = 100.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(1f).of(expectedExpandedAnchor)

        size = 30.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(1f).of(expectedExpandedAnchor)
    }

    @Test
    fun modalBottomSheet_sheetMaxWidth_sizeChanges_snapsToNewTarget() {
        lateinit var sheetMaxWidth: MutableState<Dp>
        var screenWidth by mutableStateOf(0.dp)
        rule.setContent {
            sheetMaxWidth = remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            screenWidth = with(density) { LocalResources.current.displayMetrics.widthPixels.toDp() }
            ModalBottomSheet(onDismissRequest = {}, sheetMaxWidth = sheetMaxWidth.value) {
                Box(Modifier.fillMaxWidth().testTag(sheetTag))
            }
        }

        for (dp in listOf(0.dp, 200.dp, 400.dp)) {
            sheetMaxWidth.value = dp
            val sheetWidth = rule.onNodeWithTag(sheetTag).getUnclippedBoundsInRoot().width
            val expectedSheetWidth = minOf(sheetMaxWidth.value, screenWidth)
            assertThat(sheetWidth).isEqualTo(expectedSheetWidth)
        }
    }

    @Test
    fun modalBottomSheet_emptySheet_expandDoesNotAnimate() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            scope = rememberCoroutineScope()

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {}
        }
        assertThat(state.anchoredDraggableState.currentValue).isEqualTo(SheetValue.Hidden)
        val hiddenOffset = state.requireOffset()
        scope.launch { state.show() }
        rule.waitForIdle()

        assertThat(state.anchoredDraggableState.currentValue).isEqualTo(SheetValue.Expanded)
        val expandedOffset = state.requireOffset()

        assertThat(hiddenOffset).isEqualTo(expandedOffset)
    }

    @Test
    fun modalBottomSheet_callsOnDismissRequest_onNestedScrollFling() {
        var callCount by mutableStateOf(0)
        val expectedCallCount = 1
        val nestedScrollDispatcher = NestedScrollDispatcher()
        val nestedScrollConnection =
            object : NestedScrollConnection {
                // No-Op
            }
        lateinit var scope: CoroutineScope

        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    enabledValues = setOf(SheetValue.Expanded, SheetValue.Hidden),
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(onDismissRequest = { callCount += 1 }, sheetState = sheetState) {
                Column(
                    Modifier.testTag(sheetTag)
                        .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
                ) {
                    (0..50).forEach { Text(text = "$it") }
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        val scrollableContentHeight = rule.onNodeWithTag(sheetTag).fetchSemanticsNode().size.height
        // Simulate a drag + fling
        nestedScrollDispatcher.dispatchPostScroll(
            consumed = Offset.Zero,
            available = Offset(x = 0f, y = scrollableContentHeight / 2f),
            source = NestedScrollSource.UserInput,
        )
        scope.launch {
            nestedScrollDispatcher.dispatchPostFling(
                consumed = Velocity.Zero,
                available = Velocity(x = 0f, y = with(rule.density) { 200.dp.toPx() }),
            )
        }

        rule.waitForIdle()
        assertThat(sheetState.isVisible).isFalse()
        assertThat(callCount).isEqualTo(expectedCallCount)
    }

    @Ignore("b/307313354")
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun modalBottomSheet_assertSheetContentIsReadBeforeScrim() {
        lateinit var composeView: View
        var closeSheet = ""
        rule.setContent {
            closeSheet = getString(Strings.CloseSheet)
            ModalBottomSheet(onDismissRequest = {}, modifier = Modifier.testTag(sheetTag)) {
                composeView = LocalView.current
                Box(Modifier.fillMaxWidth().height(sheetHeight))
            }
        }

        val scrimViewId = rule.onNodeWithContentDescription(closeSheet).fetchSemanticsNode().id
        val sheetViewId = rule.onNodeWithTag(sheetTag).fetchSemanticsNode().id

        rule.runOnUiThread {
            val accessibilityNodeProvider = composeView.accessibilityNodeProvider
            val sheetViewANI = accessibilityNodeProvider.createAccessibilityNodeInfo(sheetViewId)
            // Ensure that sheet A11y info is read before scrim view.
            assertThat(sheetViewANI?.extras?.traversalBefore).isAtMost(scrimViewId)
        }
    }

    @Test
    fun modalBottomSheet_testDragHandleClick() {
        lateinit var sheetState: SheetState
        var showBottomSheet by mutableStateOf(true)

        rule.setContent {
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
                ) {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
        }

        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_gesturesDisabled_doesNotParticipateInNestedScroll() =
        runBlocking(AutoTestFrameClock()) {
            lateinit var scope: CoroutineScope
            lateinit var sheetState: SheetState
            val scrollConnection = object : NestedScrollConnection {}
            val scrollDispatcher = NestedScrollDispatcher()
            val sheetHeight = 300.dp
            val sheetHeightPx = with(rule.density) { sheetHeight.toPx() }

            rule.setContent {
                scope = rememberCoroutineScope()
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = {},
                    sheetGesturesEnabled = false,
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .nestedScroll(scrollConnection, scrollDispatcher)
                            .testTag(sheetTag)
                    )
                }
            }
            scope.launch { sheetState.expand() }
            rule.waitForIdle()
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

            val offsetBeforeScroll = sheetState.requireOffset()
            scrollDispatcher.dispatchPreScroll(
                Offset(x = 0f, y = -sheetHeightPx),
                NestedScrollSource.UserInput,
            )
            rule.waitForIdle()
            assertWithMessage("Offset after scroll is equal to offset before scroll")
                .that(sheetState.requireOffset())
                .isEqualTo(offsetBeforeScroll)

            val highFlingVelocity = Velocity(x = 0f, y = with(rule.density) { 500.dp.toPx() })
            scrollDispatcher.dispatchPreFling(highFlingVelocity)
            rule.waitForIdle()
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

    @Test
    fun modalBottomSheet_respectsContentWindowInsets_whenImeIsPresent() {
        val imeHeightDp = 200.dp
        var showBottomSheet by mutableStateOf(true)

        rule.setContent {
            val density = LocalDensity.current
            val imeInsets = Insets.of(0, 0, 0, with(density) { imeHeightDp.roundToPx() })
            val windowInsets =
                WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.ime(), imeInsets)
                    .build()

            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsetsOverride(windowInsets)
            ) {
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        contentWindowInsets = { WindowInsets(0) },
                    ) {
                        Box(Modifier.testTag(sheetTag).height(200.dp).fillMaxSize())
                    }
                }
            }
        }

        rule.waitForIdle()

        val sheetBounds = rule.onNodeWithTag(sheetTag).getUnclippedBoundsInRoot()
        val rootHeight = rule.onNode(isDialog()).getUnclippedBoundsInRoot().height

        // If the sheet is NOT pushed up by the hardcoded imePadding, its bottom should be at the
        // root height.
        assertThat(sheetBounds.bottom.value).isWithin(1f).of(rootHeight.value)
    }

    private val Bundle.traversalBefore: Int
        get() = getInt("android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL")
}
